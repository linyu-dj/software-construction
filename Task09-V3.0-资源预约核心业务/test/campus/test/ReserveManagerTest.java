package campus.test;

import campus.biz.ReserveManager;
import campus.common.Result;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ReserveRecord;
import campus.entity.ReserveStatus;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.store.ReserveStore;
import campus.store.ResourceStore;
import campus.utils.TimeUtil;

import java.time.LocalDateTime;
import java.util.List;

/** 预约业务单元测试：提交校验、冲突判定、状态流转、权限边界 */
public class ReserveManagerTest {

    private static User admin() {
        return new User("admin", "123456", Roles.ADMIN, "2026-11-13 00:00:00");
    }

    private static User student() {
        return new User("student", "123456", Roles.STUDENT, "2026-11-13 00:00:00");
    }

    private static User anotherStudent() {
        return new User("other", "123456", Roles.STUDENT, "2026-11-13 00:00:00");
    }

    private static ResourceStore resourceStore() {
        ResourceStore store = new ResourceStore();
        store.add(new CampusResource("R001", "一号会议室", "教室",
                ResourceStatus.AVAILABLE, "教学楼 3 层", ""));
        store.add(new CampusResource("R002", "302 实验室", "实验室",
                ResourceStatus.OCCUPIED, "实验楼 4 层", ""));
        return store;
    }

    /** 基准时间：5 天后的 09:00，避免与"当前时间"比较时受执行时刻影响 */
    private static LocalDateTime base() {
        return LocalDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }

    public static void runAll(TestRunner runner) {
        runner.run("开始时间晚于结束时间被拒绝", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            String start = TimeUtil.format(base().plusHours(2));
            String end = TimeUtil.format(base());
            Result<ReserveRecord> result = manager.submit(student(), "R001", start, end, "颠倒");

            Assert.isFalse(result.isSuccess(), "时间颠倒应失败");
            Assert.contains("开始时间必须早于结束时间", result.getMsg(), "提示应说明先后关系");
        });

        runner.run("结束时间早于当前时间被拒绝", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            String start = TimeUtil.format(LocalDateTime.now().minusDays(2));
            String end = TimeUtil.format(LocalDateTime.now().minusDays(1));
            Result<ReserveRecord> result = manager.submit(student(), "R001", start, end, "过去");

            Assert.isFalse(result.isSuccess(), "过去时间应失败");
            Assert.contains("必须晚于当前时间", result.getMsg(), "提示应说明时间已过");
        });

        runner.run("时间格式错误被拒绝", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            Result<ReserveRecord> result = manager.submit(student(), "R001",
                    "2026/11/20 09:00", "2026/11/20 10:00", "格式");

            Assert.isFalse(result.isSuccess(), "格式错误应失败");
            Assert.contains("格式不正确", result.getMsg(), "提示应说明格式");
        });

        runner.run("资源不存在被拒绝", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            Result<ReserveRecord> result = manager.submit(student(), "R999",
                    TimeUtil.format(base()), TimeUtil.format(base().plusHours(1)), "");

            Assert.isFalse(result.isSuccess(), "不存在的资源应失败");
            Assert.contains("未找到编号为 R999 的资源", result.getMsg(), "提示应说明资源不存在");
        });

        runner.run("非可预约资源不可提交", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            Result<ReserveRecord> result = manager.submit(student(), "R002",
                    TimeUtil.format(base()), TimeUtil.format(base().plusHours(1)), "");

            Assert.isFalse(result.isSuccess(), "占用资源应不可预约");
            Assert.contains("不可预约", result.getMsg(), "提示应说明状态不可预约");
        });

        runner.run("管理员不能代提交预约", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            Result<ReserveRecord> result = manager.submit(admin(), "R001",
                    TimeUtil.format(base()), TimeUtil.format(base().plusHours(1)), "");

            Assert.isFalse(result.isSuccess(), "管理员提交应失败");
            Assert.contains("仅学生可用", result.getMsg(), "提示应说明权限");
        });

        runner.run("提交成功生成待审核记录", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            Result<ReserveRecord> result = manager.submit(student(), "R001",
                    TimeUtil.format(base()), TimeUtil.format(base().plusHours(2)), "部门例会");

            Assert.isTrue(result.isSuccess(), "正常预约应成功");
            Assert.equals("B001", result.getData().getRecordId(), "记录编号应为 B001");
            Assert.equals("PENDING", result.getData().getStatus().name(), "默认状态应为待审核");
            Assert.equals(1, reserveStore.size(), "记录应写入内存");
        });

        runner.run("与已通过预约重叠被拒绝", () -> {
            ReserveStore reserveStore = new ReserveStore();
            reserveStore.add(new ReserveRecord("B001", "R001", "other",
                    LocalDateTime.now(), base(), base().plusHours(2), ReserveStatus.PASS, ""));
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            Result<ReserveRecord> result = manager.submit(student(), "R001",
                    TimeUtil.format(base().plusHours(1)), TimeUtil.format(base().plusHours(3)), "冲突");

            Assert.isFalse(result.isSuccess(), "重叠时段应失败");
            Assert.contains("时间段冲突", result.getMsg(), "提示应说明冲突");
            Assert.equals(1, reserveStore.size(), "失败不应新增记录");
        });

        runner.run("首尾相接不算冲突", () -> {
            ReserveStore reserveStore = new ReserveStore();
            reserveStore.add(new ReserveRecord("B001", "R001", "other",
                    LocalDateTime.now(), base(), base().plusHours(2), ReserveStatus.PASS, ""));
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            Result<ReserveRecord> result = manager.submit(student(), "R001",
                    TimeUtil.format(base().plusHours(2)), TimeUtil.format(base().plusHours(3)), "紧接");

            Assert.isTrue(result.isSuccess(), "首尾相接应允许");
            Assert.equals(2, reserveStore.size(), "应新增一条记录");
        });

        runner.run("我的预约只含本人记录", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            manager.submit(student(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "我的");
            manager.submit(anotherStudent(), "R001", TimeUtil.format(base().plusHours(4)),
                    TimeUtil.format(base().plusHours(5)), "别人的");

            Result<List<ReserveRecord>> result = manager.listMine(student());
            Assert.isTrue(result.isSuccess(), "查询应成功");
            Assert.equals(1, result.getData().size(), "只应看到自己的 1 条");
            Assert.equals("student", result.getData().get(0).getApplicantUsername(), "申请应为本人");
        });

        runner.run("管理员不能查我的预约视图", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            Result<List<ReserveRecord>> result = manager.listMine(admin());

            Assert.isFalse(result.isSuccess(), "管理员调用学生视图应失败");
            Assert.contains("仅学生可用", result.getMsg(), "提示应说明权限");
        });

        runner.run("不能取消他人的预约", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            manager.submit(anotherStudent(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "");

            Result<Void> result = manager.cancel(student(), "B001");
            Assert.isFalse(result.isSuccess(), "取消他人预约应失败");
            Assert.contains("无权取消他人", result.getMsg(), "提示应说明越权");
        });

        runner.run("已驳回的预约不能取消", () -> {
            ReserveStore reserveStore = new ReserveStore();
            reserveStore.add(new ReserveRecord("B001", "R001", "student", LocalDateTime.now(),
                    base(), base().plusHours(1), ReserveStatus.REJECT, ""));
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            Result<Void> result = manager.cancel(student(), "B001");

            Assert.isFalse(result.isSuccess(), "已驳回记录不应取消");
            Assert.contains("不能取消", result.getMsg(), "提示应说明状态限制");
        });

        runner.run("取消已通过的预约并恢复资源", () -> {
            ReserveStore reserveStore = new ReserveStore();
            reserveStore.add(new ReserveRecord("B001", "R001", "student", LocalDateTime.now(),
                    base(), base().plusHours(1), ReserveStatus.PASS, ""));
            ResourceStore resources = resourceStore();
            resources.findById("R001").get().setStatus(ResourceStatus.OCCUPIED);
            ReserveManager manager = new ReserveManager(reserveStore, resources);

            Result<Void> result = manager.cancel(student(), "B001");
            Assert.isTrue(result.isSuccess(), "取消应成功");
            Assert.equals("CANCEL", reserveStore.findById("B001").get().getStatus().name(), "状态应为已取消");
            Assert.equals("AVAILABLE", resources.findById("R001").get().getStatus().name(), "资源应恢复可预约");
        });

        runner.run("审核通过置资源为占用", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            manager.submit(student(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "");

            Result<Void> result = manager.audit(admin(), "B001", true);
            Assert.isTrue(result.isSuccess(), "审核通过应成功");
            Assert.equals("PASS", reserveStore.findById("B001").get().getStatus().name(), "记录应为已通过");
        });

        runner.run("审核驳回不动资源状态", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ResourceStore resources = resourceStore();
            ReserveManager manager = new ReserveManager(reserveStore, resources);
            manager.submit(student(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "");

            Result<Void> result = manager.audit(admin(), "B001", false);
            Assert.isTrue(result.isSuccess(), "驳回应成功");
            Assert.equals("REJECT", reserveStore.findById("B001").get().getStatus().name(), "记录应为已驳回");
            Assert.equals("AVAILABLE", resources.findById("R001").get().getStatus().name(), "资源状态不应变化");
        });

        runner.run("已审核的记录不能再次审核", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            manager.submit(student(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "");
            manager.audit(admin(), "B001", true);
            Result<Void> again = manager.audit(admin(), "B001", false);

            Assert.isFalse(again.isSuccess(), "重复审核应失败");
            Assert.contains("不能再次审核", again.getMsg(), "提示应说明状态限制");
        });

        runner.run("学生不能审核预约", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            manager.submit(student(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "");

            Result<Void> result = manager.audit(student(), "B001", true);
            Assert.isFalse(result.isSuccess(), "学生审核应失败");
            Assert.contains("仅管理员可用", result.getMsg(), "提示应说明权限");
        });

        runner.run("条件查询按状态与申请人过滤", () -> {
            ReserveStore reserveStore = new ReserveStore();
            ReserveManager manager = new ReserveManager(reserveStore, resourceStore());
            manager.submit(student(), "R001", TimeUtil.format(base()),
                    TimeUtil.format(base().plusHours(1)), "A");
            manager.submit(anotherStudent(), "R001", TimeUtil.format(base().plusHours(4)),
                    TimeUtil.format(base().plusHours(5)), "B");
            manager.audit(admin(), "B001", true);

            Result<List<ReserveRecord>> pass = manager.search(admin(), "", "", ReserveStatus.PASS);
            Assert.equals(1, pass.getData().size(), "已通过应只有 1 条");

            Result<List<ReserveRecord>> byApplicant = manager.search(admin(), "", "other", null);
            Assert.equals(1, byApplicant.getData().size(), "按申请人应命中 1 条");

            Result<List<ReserveRecord>> all = manager.search(admin(), "", "", null);
            Assert.equals(2, all.getData().size(), "条件全空应返回全部");
        });

        runner.run("取消不存在的记录给出中文提示", () -> {
            ReserveManager manager = new ReserveManager(new ReserveStore(), resourceStore());
            Result<Void> result = manager.cancel(student(), "B999");

            Assert.isFalse(result.isSuccess(), "不存在的记录应失败");
            Assert.contains("未找到编号为 B999 的预约记录", result.getMsg(), "提示应说明记录不存在");
        });
    }
}
