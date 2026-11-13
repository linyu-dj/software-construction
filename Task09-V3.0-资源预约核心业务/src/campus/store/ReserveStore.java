package campus.store;

import campus.entity.ReserveRecord;
import campus.entity.ReserveStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 内存预约存储（信息专家）。
 *
 * 和 UserStore / ResourceStore 一个思路：集合在谁手里，查询就归谁。
 * 这一轮沿用 V2.2 的函数式写法，筛选、排序全部走 Stream，按编号查找返回 Optional。
 */
public class ReserveStore {

    /** 预约编号前缀 */
    private static final String ID_PREFIX = "B";

    private final List<ReserveRecord> records = new ArrayList<>();

    /** 启动时用文件加载结果填充 */
    public void replaceAll(List<ReserveRecord> loaded) {
        records.clear();
        if (loaded != null) {
            records.addAll(loaded);
        }
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public int size() {
        return records.size();
    }

    public void add(ReserveRecord record) {
        records.add(record);
    }

    /** 按记录编号查找 */
    public Optional<ReserveRecord> findById(String recordId) {
        if (recordId == null) {
            return Optional.empty();
        }
        return records.stream()
                .filter(record -> record.getRecordId().equals(recordId))
                .findFirst();
    }

    /** 某位申请人的全部预约，按编号升序 */
    public List<ReserveRecord> findByApplicant(String username) {
        String target = username == null ? "" : username.trim();
        return records.stream()
                .filter(record -> record.getApplicantUsername().equals(target))
                .sorted(Comparator.comparing(ReserveRecord::getRecordId))
                .collect(Collectors.toList());
    }

    /** 某个资源的全部预约，按编号升序 */
    public List<ReserveRecord> findByResource(String resourceId) {
        String target = resourceId == null ? "" : resourceId.trim();
        return records.stream()
                .filter(record -> record.getResourceId().equals(target))
                .sorted(Comparator.comparing(ReserveRecord::getRecordId))
                .collect(Collectors.toList());
    }

    /** 某状态下已被通过的预约是否占用该资源——删除资源前的校验 */
    public boolean hasPassReservation(String resourceId) {
        String target = resourceId == null ? "" : resourceId.trim();
        return records.stream()
                .anyMatch(record -> record.getResourceId().equals(target)
                        && record.getStatus() == ReserveStatus.PASS);
    }

    /**
     * 条件组合查询：资源编号、申请人、状态三个条件都可为空，空表示不限。
     * 申请人支持模糊匹配（输入 a 命中 admin），资源编号要求完整匹配。
     */
    public List<ReserveRecord> search(String resourceId, String applicantKeyword, ReserveStatus status) {
        String resourceTarget = resourceId == null ? "" : resourceId.trim();
        String applicantTarget = applicantKeyword == null
                ? "" : applicantKeyword.trim().toLowerCase(Locale.ROOT);
        return records.stream()
                .filter(record -> resourceTarget.isEmpty() || record.getResourceId().equals(resourceTarget))
                .filter(record -> applicantTarget.isEmpty()
                        || record.getApplicantUsername().toLowerCase(Locale.ROOT).contains(applicantTarget))
                .filter(record -> status == null || record.getStatus() == status)
                .sorted(Comparator.comparing(ReserveRecord::getRecordId))
                .collect(Collectors.toList());
    }

    /** 只读快照，供持久化使用 */
    public List<ReserveRecord> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    /** 生成下一个记录编号：取现有最大序号加一，格式 B001，不复用空号 */
    public String nextId() {
        int max = records.stream()
                .mapToInt(record -> parseSequence(record.getRecordId()))
                .max()
                .orElse(0);
        return ID_PREFIX + String.format("%03d", max + 1);
    }

    /** 从 B012 这样的编号里取出 12，解析失败按 0 处理 */
    private static int parseSequence(String recordId) {
        if (recordId == null || !recordId.startsWith(ID_PREFIX)) {
            return 0;
        }
        try {
            return Integer.parseInt(recordId.substring(ID_PREFIX.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
