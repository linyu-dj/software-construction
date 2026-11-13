package campus.test;

import campus.biz.ResourceManager;
import campus.common.Result;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ReserveRecord;
import campus.entity.ReserveStatus;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.store.ReserveStore;
import campus.store.ResourceStore;

import java.time.LocalDateTime;
import java.util.List;

/** 资源业务单元测试：权限、CRUD、状态过滤、删除校验 */
public class ResourceManagerTest {

    private static User admin() {
        return new User("admin", "123456", Roles.ADMIN, "2026-11-13 00:00:00");
    }

    private static User student() {
        return new User("student", "123456", Roles.STUDENT, "2026-11-13 00:00:00");
    }

    private static ResourceStore seededStore() {
        ResourceStore store = new ResourceStore();
        store.add(new CampusResource("R001", "一号会议室", "教室",
                ResourceStatus.AVAILABLE, "教学楼 3 层", "可容纳 30 人"));
        store.add(new CampusResource("R002", "302 实验室", "实验室",
                ResourceStatus.OCCUPIED, "实验楼 4 层", "需提前预约"));
        store.add(new CampusResource("R003", "投影仪 A", "设备",
                ResourceStatus.AVAILABLE, "器材室", "借用需登记"));
        return store;
    }

    private static ReserveRecord passRecord(String recordId, String resourceId) {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        return new ReserveRecord(recordId, resourceId, "student", LocalDateTime.now(),
                start, start.plusHours(2), ReserveStatus.PASS, "");
    }

    public static void runAll(TestRunner runner) {
        runner.run("学生无权新增资源", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<CampusResource> result = manager.create(student(), "研讨室B", "教室", "图书馆 4 层", "");

            Assert.isFalse(result.isSuccess(), "学生新增资源应失败");
            Assert.contains("仅管理员可用", result.getMsg(), "提示应说明权限");
        });

        runner.run("管理员新增资源编号顺延", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<CampusResource> result = manager.create(admin(), "研讨室B", "教室", "图书馆 4 层", "小组讨论用");

            Assert.isTrue(result.isSuccess(), "管理员新增资源应成功");
            Assert.equals("R004", result.getData().getId(), "编号应为 R004");
            Assert.equals("AVAILABLE", result.getData().getStatus().name(), "默认状态应为可预约");
            Assert.equals(4, store.size(), "资源数应为 4");
        });

        runner.run("资源名称为空被拒绝", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<CampusResource> result = manager.create(admin(), "  ", "教室", "教学楼", "");

            Assert.isFalse(result.isSuccess(), "空名称应失败");
            Assert.contains("资源名称不能为空", result.getMsg(), "提示应说明名称不能为空");
        });

        runner.run("学生列表只含可预约资源", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<List<CampusResource>> result = manager.listFor(student());

            Assert.isTrue(result.isSuccess(), "查询应成功");
            Assert.equals(2, result.getData().size(), "学生应看到 2 条可预约资源");
            result.getData().forEach(resource ->
                    Assert.equals("AVAILABLE", resource.getStatus().name(), "学生列表不应出现非可预约资源"));
        });

        runner.run("管理员列表含全部资源", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<List<CampusResource>> result = manager.listFor(admin());

            Assert.equals(3, result.getData().size(), "管理员应看到 3 条");
        });

        runner.run("学生按编号查占用资源被拒", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<CampusResource> result = manager.findById(student(), "R002");

            Assert.isFalse(result.isSuccess(), "学生不应看到占用资源详情");
            Assert.contains("可预约资源", result.getMsg(), "提示按可预约资源口径");
        });

        runner.run("管理员按编号查占用资源成功", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<CampusResource> result = manager.findById(admin(), "R002");

            Assert.isTrue(result.isSuccess(), "管理员应能查看占用资源");
            Assert.equals("R002", result.getData().getId(), "查到的资源应为 R002");
        });

        runner.run("学生无权修改资源", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<Void> result = manager.update(student(), "R001", "新名称", "教室", "新位置", ResourceStatus.DISABLED);

            Assert.isFalse(result.isSuccess(), "学生修改应失败");
            Assert.contains("仅管理员可用", result.getMsg(), "提示应说明权限");
        });

        runner.run("管理员修改状态生效", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<Void> result = manager.update(admin(), "R001", "一号会议室",
                    "教室", "教学楼 3 层", ResourceStatus.OCCUPIED);

            Assert.isTrue(result.isSuccess(), "修改应成功");
            Assert.equals("OCCUPIED", store.findById("R001").get().getStatus().name(), "状态应变为占用");
        });

        runner.run("存在已通过预约的资源禁止删除", () -> {
            ResourceStore store = seededStore();
            ReserveStore reserveStore = new ReserveStore();
            reserveStore.add(passRecord("B001", "R001"));
            ResourceManager manager = new ResourceManager(store, reserveStore);
            Result<Void> result = manager.delete(admin(), "R001");

            Assert.isFalse(result.isSuccess(), "有 PASS 预约的资源不应删除");
            Assert.contains("禁止删除", result.getMsg(), "提示应说明禁止删除");
            Assert.equals(3, store.size(), "资源数不应变化");
        });

        runner.run("仅有待审核预约的资源可以删除", () -> {
            ResourceStore store = seededStore();
            ReserveStore reserveStore = new ReserveStore();
            LocalDateTime start = LocalDateTime.now().plusDays(3);
            reserveStore.add(new ReserveRecord("B002", "R003", "student", LocalDateTime.now(),
                    start, start.plusHours(1), ReserveStatus.PENDING, ""));
            ResourceManager manager = new ResourceManager(store, reserveStore);
            Result<Void> result = manager.delete(admin(), "R003");

            Assert.isTrue(result.isSuccess(), "只有 PENDING 预约的资源应可删除");
            Assert.equals(2, store.size(), "资源数应减为 2");
        });

        runner.run("删除不存在的资源给出中文提示", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());
            Result<Void> result = manager.delete(admin(), "R999");

            Assert.isFalse(result.isSuccess(), "不存在的资源删除应失败");
            Assert.contains("未找到编号为 R999 的资源", result.getMsg(), "提示应说明资源不存在");
        });

        runner.run("名称模糊查询按编号排序", () -> {
            ResourceStore store = seededStore();
            ResourceManager manager = new ResourceManager(store, new ReserveStore());

            Result<List<CampusResource>> meeting = manager.searchByName(admin(), "会议");
            Assert.equals(1, meeting.getData().size(), "「会议」应命中 1 条");
            Assert.equals("R001", meeting.getData().get(0).getId(), "命中编号应为 R001");

            Result<List<CampusResource>> studentLab = manager.searchByName(student(), "实验");
            Assert.equals(0, studentLab.getData().size(), "学生搜索占用资源应无结果");

            Result<List<CampusResource>> all = manager.searchByName(admin(), "");
            Assert.equals(3, all.getData().size(), "空关键词应返回全部");
        });
    }
}
