package campus.test;

import campus.biz.ResourceManager;
import campus.common.Result;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.store.ResourceStore;

import java.util.List;

/**
 * 资源业务测试（V2.1）。
 *
 * 与 V2.0 相比业务规则没变，变的是返回形态：所有方法统一返回 Result，
 * 越权与查找失败都不再以异常形式穿过业务层。
 * 测试的重点因此从「抛哪个异常」转为「失败是否被转成可读提示、成功是否带数据」。
 */
public class ResourceManagerTest {

    private static User admin() {
        return new User("admin", "admin888", Roles.ADMIN, "2026-11-12 08:00:00");
    }

    private static User student() {
        return new User("alice", "123456", Roles.STUDENT, "2026-11-12 08:00:00");
    }

    private static ResourceManager managerWithSeeds() {
        ResourceStore store = new ResourceStore();
        store.add(new CampusResource("R001", "一号会议室", "教室", ResourceStatus.AVAILABLE, "教学楼 3 层", ""));
        store.add(new CampusResource("R002", "302 实验室", "实验室", ResourceStatus.OCCUPIED, "实验楼 3 层", ""));
        return new ResourceManager(store);
    }

    public static void runAll(TestRunner runner) {
        runner.run("管理员新增资源返回成功与数据", () -> {
            Result<CampusResource> result = managerWithSeeds().create(admin(), "四号会议室", "教室", "教学楼 4 层", "");
            Assert.isTrue(result.isSuccess(), "管理员新增应成功");
            Assert.equals("R003", result.getData().getId(), "应在最大编号后顺延");
            Assert.contains("新增成功", result.getMsg(), "提示应说明结果");
        });

        runner.run("学生新增资源返回失败", () -> {
            Result<CampusResource> result = managerWithSeeds().create(student(), "五号会议室", "教室", "教学楼", "");
            Assert.isFalse(result.isSuccess(), "学生新增应失败");
            Assert.contains("无权", result.getMsg(), "提示应说明操作被权限拒绝");
        });

        runner.run("学生列表只返回可预约资源", () -> {
            Result<List<CampusResource>> result = managerWithSeeds().listFor(student());
            Assert.isTrue(result.isSuccess(), "查询应成功");
            Assert.equals(1, result.getData().size(), "两条资源中只有一条可预约");
        });

        runner.run("管理员列表返回全部资源", () -> {
            Assert.equals(2, managerWithSeeds().listFor(admin()).getData().size(), "管理员应看到两条");
        });

        runner.run("学生查占用资源返回失败", () -> {
            Result<CampusResource> result = managerWithSeeds().findById(student(), "R002");
            Assert.isFalse(result.isSuccess(), "学生不应看到占用资源");
            Assert.contains("可预约", result.getMsg(), "提示应说明只开放可预约资源");
        });

        runner.run("查询不存在编号返回失败", () -> {
            Result<CampusResource> result = managerWithSeeds().findById(admin(), "R999");
            Assert.isFalse(result.isSuccess(), "不存在编号应失败");
            Assert.isTrue(result.getData() == null, "失败时不携带数据");
        });

        runner.run("管理员修改资源成功", () -> {
            ResourceManager manager = managerWithSeeds();
            Result<Void> result = manager.update(admin(), "R001", "一号会议室（改）", "会议室", "教学楼 2 层",
                    ResourceStatus.DISABLED);
            Assert.isTrue(result.isSuccess(), "修改应成功");
            Assert.equals(ResourceStatus.DISABLED,
                    manager.findById(admin(), "R001").getData().getStatus(), "状态应已更新");
        });

        runner.run("学生修改资源返回失败", () -> {
            Result<Void> result = managerWithSeeds().update(student(), "R001", "改", "教室", "教学楼",
                    ResourceStatus.AVAILABLE);
            Assert.isFalse(result.isSuccess(), "学生修改应失败");
        });

        runner.run("管理员删除资源成功", () -> {
            ResourceManager manager = managerWithSeeds();
            Result<Void> result = manager.delete(admin(), "R002");
            Assert.isTrue(result.isSuccess(), "删除应成功");
            Assert.equals(1, manager.listFor(admin()).getData().size(), "删除后应剩一条");
        });

        runner.run("删除不存在编号返回失败", () -> {
            Result<Void> result = managerWithSeeds().delete(admin(), "R999");
            Assert.isFalse(result.isSuccess(), "不存在编号应失败");
            Assert.contains("R999", result.getMsg(), "提示应包含编号");
        });
    }
}
