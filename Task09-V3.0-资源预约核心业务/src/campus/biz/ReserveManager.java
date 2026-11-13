package campus.biz;

import campus.common.Result;
import campus.entity.CampusResource;
import campus.entity.ReserveRecord;
import campus.entity.ReserveStatus;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.InvalidReserveStateException;
import campus.exception.PermissionDeniedException;
import campus.exception.ReserveNotFoundException;
import campus.exception.ResourceNotFoundException;
import campus.exception.TimeConflictException;
import campus.store.ReserveStore;
import campus.store.ResourceStore;
import campus.utils.FileUtil;
import campus.utils.TimeUtil;
import campus.utils.Validator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约业务类（V3.0），承担 GRASP 控制器职责，并协调两条业务线的状态。
 *
 * 这一轮系统第一次出现「两条业务线互相影响」：审核通过要改资源状态，
 * 取消已通过的预约要把资源放回来，删除资源要先看有没有已通过的预约。
 *
 * 当前这些联动集中于同一个类，ResourceManager 也直接持有 ReserveStore。
 * 该跨域耦合是本阶段的有意设计债：任务 10 分层时替换为服务层调用。
 * 提前拆分会使下一轮失去可分析的具体问题，因此此处保留。
 *
 * 方法契约：全部返回 Result；业务校验失败抛自定义异常，方法内部捕获后转成
 * Result.fail。异常保证「不满足即中止」，Result 负责向视图层交付可读结论。
 */
public class ReserveManager {

    private final ReserveStore reserveStore;
    private final ResourceStore resourceStore;

    public ReserveManager(ReserveStore reserveStore, ResourceStore resourceStore) {
        this.reserveStore = reserveStore;
        this.resourceStore = resourceStore;
    }

    // ==================== 学生操作 ====================

    /**
     * 提交预约申请。
     *
     * 校验顺序很重要：先时间格式与前后关系，再资源状态，最后时间冲突。
     * 顺序不对会出现「资源不可约」和「时间填错了」同时成立时先报哪个的问题，
     * 现在的顺序让用户先看到最容易改的错误。
     */
    public Result<ReserveRecord> submit(User applicant, String resourceId,
                                        String startText, String endText, String remark) {
        try {
            Validator.requireStudent(applicant, "提交预约申请");
            Validator.requireNonBlank(resourceId, "资源编号");

            LocalDateTime startTime = parseTime(startText, "预约开始时间");
            LocalDateTime endTime = parseTime(endText, "预约结束时间");
            requireTimeRange(startTime, endTime);

            CampusResource resource = resourceStore.findById(resourceId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "未找到编号为 " + resourceId + " 的资源。"));
            if (resource.getStatus() != ResourceStatus.AVAILABLE) {
                throw new BusinessException("资源 " + resource.getId() + " 当前状态为「"
                        + resource.getStatus().getLabel() + "」，不可预约。");
            }
            requireNoTimeConflict(resource.getId(), startTime, endTime);

            ReserveRecord record = new ReserveRecord(
                    reserveStore.nextId(), resource.getId(), applicant.getUsername(),
                    LocalDateTime.now(), startTime, endTime, ReserveStatus.PENDING,
                    remark == null ? "" : remark.trim());
            reserveStore.add(record);
            FileUtil.appendReserve(record);
            return Result.success("预约提交成功，记录编号 " + record.getRecordId()
                    + "，当前状态「待审核」，请等待管理员审核。", record);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 查询我的预约：只返回当前登录学生的记录 */
    public Result<List<ReserveRecord>> listMine(User applicant) {
        try {
            Validator.requireStudent(applicant, "查询我的预约");
            List<ReserveRecord> records = reserveStore.findByApplicant(applicant.getUsername());
            return Result.success("查询成功，您共有 " + records.size() + " 条预约记录。", records);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 取消预约：待审核与已通过可以取消，已驳回与已取消不能再取消。
     * 取消一条已通过的预约时，资源要放回可预约状态。
     */
    public Result<Void> cancel(User applicant, String recordId) {
        try {
            Validator.requireStudent(applicant, "取消预约");
            Validator.requireNonBlank(recordId, "预约记录编号");

            ReserveRecord record = reserveStore.findById(recordId.trim())
                    .orElseThrow(() -> new ReserveNotFoundException(
                            "未找到编号为 " + recordId + " 的预约记录。"));
            if (!record.getApplicantUsername().equals(applicant.getUsername())) {
                throw new PermissionDeniedException("无权取消他人的预约记录 " + record.getRecordId() + "。");
            }
            if (record.getStatus().isFinal()) {
                throw new InvalidReserveStateException("预约 " + record.getRecordId() + " 当前状态为「"
                        + record.getStatus().getLabel() + "」，不能取消。");
            }

            boolean wasPass = record.getStatus() == ReserveStatus.PASS;
            record.setStatus(ReserveStatus.CANCEL);
            FileUtil.saveReserves(reserveStore.snapshot());

            if (wasPass) {
                restoreResource(record.getResourceId());
            }
            return Result.success("预约 " + record.getRecordId() + " 已取消"
                    + (wasPass ? "，资源 " + record.getResourceId() + " 已恢复为可预约。" : "。"));
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ==================== 管理员操作 ====================

    /** 查询全部预约记录 */
    public Result<List<ReserveRecord>> listAll(User operator) {
        try {
            Validator.requireAdmin(operator, "查询全部预约");
            List<ReserveRecord> records = reserveStore.snapshot().stream()
                    .sorted(Comparator.comparing(ReserveRecord::getRecordId))
                    .collect(Collectors.toList());
            return Result.success("查询成功，系统共有 " + records.size() + " 条预约记录。", records);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 审核预约：通过则资源置为占用，驳回则资源不动。
     * 只允许审核「待审核」的记录，避免对已通过的记录重复审核。
     */
    public Result<Void> audit(User operator, String recordId, boolean approve) {
        try {
            Validator.requireAdmin(operator, "审核预约");
            Validator.requireNonBlank(recordId, "预约记录编号");

            ReserveRecord record = reserveStore.findById(recordId.trim())
                    .orElseThrow(() -> new ReserveNotFoundException(
                            "未找到编号为 " + recordId + " 的预约记录。"));
            if (record.getStatus() != ReserveStatus.PENDING) {
                throw new InvalidReserveStateException("预约 " + record.getRecordId() + " 当前状态为「"
                        + record.getStatus().getLabel() + "」，不能再次审核。");
            }

            if (approve) {
                CampusResource resource = resourceStore.findById(record.getResourceId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "预约关联的资源 " + record.getResourceId() + " 已不存在，无法通过。"));
                resource.setStatus(ResourceStatus.OCCUPIED);
                record.setStatus(ReserveStatus.PASS);
                FileUtil.saveReserves(reserveStore.snapshot());
                FileUtil.saveResources(resourceStore.snapshot());
                return Result.success("审核通过：预约 " + record.getRecordId()
                        + " 已通过，资源 " + resource.getId() + " 已置为「占用」。");
            }

            record.setStatus(ReserveStatus.REJECT);
            FileUtil.saveReserves(reserveStore.snapshot());
            return Result.success("审核驳回：预约 " + record.getRecordId() + " 已驳回，资源状态未变更。");
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 按条件查询预约：资源编号、申请人、状态，条件留空表示不限 */
    public Result<List<ReserveRecord>> search(User operator, String resourceId,
                                              String applicant, ReserveStatus status) {
        try {
            Validator.requireAdmin(operator, "条件查询预约");
            List<ReserveRecord> records = reserveStore.search(resourceId, applicant, status);
            return Result.success("查询成功，符合条件的预约共 " + records.size() + " 条。", records);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ==================== 内部工具 ====================

    /** 解析时间文本，为空或格式不对统一提示，不让 null 流到后面 */
    private static LocalDateTime parseTime(String text, String label) {
        if (text == null || text.trim().isEmpty()) {
            throw new EmptyParamException(label + "不能为空。");
        }
        LocalDateTime parsed = TimeUtil.parse(text);
        if (parsed == null) {
            throw new BusinessException(label + "格式不正确，请按 "
                    + TimeUtil.inputPatternHint() + " 填写，例如 2026-11-20 14:00。");
        }
        return parsed;
    }

    /** 起止时间关系：开始必须早于结束，结束必须晚于当前时刻 */
    private static void requireTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("预约开始时间必须早于结束时间。");
        }
        if (!endTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("预约结束时间必须晚于当前时间。");
        }
    }

    /**
     * 冲突校验：同一资源下，已通过（PASS）的预约与本段时间重叠则禁止提交。
     * 边界相接不算冲突——8:00-10:00 与 10:00-12:00 可以并存。
     */
    private void requireNoTimeConflict(String resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ReserveRecord> existing = reserveStore.findByResource(resourceId);
        for (ReserveRecord record : existing) {
            if (record.getStatus() != ReserveStatus.PASS) {
                continue;
            }
            boolean overlaps = record.getReserveStartTime().isBefore(endTime)
                    && startTime.isBefore(record.getReserveEndTime());
            if (overlaps) {
                throw new TimeConflictException("资源 " + resourceId + " 在 "
                        + TimeUtil.formatShort(record.getReserveStartTime()) + " 至 "
                        + TimeUtil.formatShort(record.getReserveEndTime())
                        + " 已有通过的预约（" + record.getRecordId() + "），时间段冲突。");
            }
        }
    }

    /** 已通过的预约被取消后，把资源放回可预约；资源已不存在则跳过 */
    private void restoreResource(String resourceId) {
        resourceStore.findById(resourceId).ifPresent(resource -> {
            if (resource.getStatus() == ResourceStatus.OCCUPIED) {
                resource.setStatus(ResourceStatus.AVAILABLE);
                FileUtil.saveResources(resourceStore.snapshot());
            }
        });
    }
}
