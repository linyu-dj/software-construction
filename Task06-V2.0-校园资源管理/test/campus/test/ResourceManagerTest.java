package campus.test;

import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.biz.ResourceManager;
import campus.exception.EmptyParamException;
import campus.exception.PermissionDeniedException;
import campus.exception.ResourceNotFoundException;
import campus.store.ResourceStore;

import java.util.List;

/**
 * 资源业务测试（V2.0）。
 *
 * 本轮权限是第一道门（菜单），业务方法里的 requireAdmin 是第二道门。
 * 测试绕过菜单直接调用业务方法，验证「即使界面放过，业务层也不会执行越权操作」——
 * 这正是把权限放进业务层的意义：约束不依赖某一个调用入口。
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
        store.add(new CampusResource("R003", "投影仪 A", "设备", ResourceStatus.DISABLED, "器材室", ""));
        return new ResourceManager(store);
    }

    public static void runAll(TestRunner runner) {
        runner.run("学生新增资源被拒绝", () -> {
            try {
                managerWithSeeds().create(student(), "新资源", "教室", "教学楼", "");
                Assert.fail("学生不应能新增资源");
            } catch (PermissionDeniedException expected) {
                Assert.contains("新增校园资源", expected.getMessage(), "提示应说明被拒绝的操作");
            }
        });

        runner.run("管理员新增资源生成编号并置为可预约", () -> {
            ResourceManager manager = managerWithSeeds();
            CampusResource created = manager.create(admin(), " 四号会议室 ", "教室", " 教学楼 4 层 ", "");
            Assert.equals("R004", created.getId(), "应在最大编号后顺延");
            Assert.equals("四号会议室", created.getName(), "名称应去掉首尾空格");
            Assert.equals(ResourceStatus.AVAILABLE, created.getStatus(), "新增资源默认可预约");
        });

        runner.run("名称为空时新增被拒绝", () -> {
            try {
                managerWithSeeds().create(admin(), "   ", "教室", "教学楼", "");
                Assert.fail("空白名称应被拒绝");
            } catch (EmptyParamException expected) {
                Assert.isTrue(true, "异常类型正确");
            }
        });

        runner.run("学生列表只含可预约资源", () -> {
            List<CampusResource> list = managerWithSeeds().listFor(student());
            Assert.equals(1, list.size(), "三条种子资源中只有一条可预约");
            Assert.equals(ResourceStatus.AVAILABLE, list.get(0).getStatus(), "列表内状态应为可预约");
        });

        runner.run("管理员列表包含全部资源", () -> {
            Assert.equals(3, managerWithSeeds().listFor(admin()).size(), "管理员应看到三条");
        });

        runner.run("学生查占用资源得到未找到提示", () -> {
            try {
                managerWithSeeds().findById(student(), "R002");
                Assert.fail("学生不应看到占用资源");
            } catch (ResourceNotFoundException expected) {
                Assert.contains("可预约", expected.getMessage(), "提示应说明只开放可预约资源");
            }
        });

        runner.run("管理员可查占用资源", () -> {
            Assert.equals("302 实验室", managerWithSeeds().findById(admin(), "R002").getName(), "管理员应能查到");
        });

        runner.run("查询不存在的编号抛出未找到异常", () -> {
            try {
                managerWithSeeds().findById(admin(), "R999");
                Assert.fail("不存在的编号应抛出异常");
            } catch (ResourceNotFoundException expected) {
                Assert.contains("R999", expected.getMessage(), "提示应包含编号");
            }
        });

        runner.run("管理员修改资源字段与状态", () -> {
            ResourceManager manager = managerWithSeeds();
            manager.update(admin(), "R001", "一号会议室（改）", "会议室", "教学楼 2 层", ResourceStatus.DISABLED);
            CampusResource updated = manager.findById(admin(), "R001");
            Assert.equals("一号会议室（改）", updated.getName(), "名称应更新");
            Assert.equals("会议室", updated.getType(), "类型应更新");
            Assert.equals(ResourceStatus.DISABLED, updated.getStatus(), "状态应更新");
        });

        runner.run("学生修改资源被拒绝", () -> {
            try {
                managerWithSeeds().update(student(), "R001", "改", "教室", "教学楼", ResourceStatus.AVAILABLE);
                Assert.fail("学生不应能修改资源");
            } catch (PermissionDeniedException expected) {
                Assert.isTrue(true, "异常类型正确");
            }
        });

        runner.run("管理员删除资源后列表减少", () -> {
            ResourceManager manager = managerWithSeeds();
            manager.delete(admin(), "R003");
            Assert.equals(2, manager.listFor(admin()).size(), "删除后应剩两条");
        });

        runner.run("学生删除资源被拒绝", () -> {
            try {
                managerWithSeeds().delete(student(), "R001");
                Assert.fail("学生不应能删除资源");
            } catch (PermissionDeniedException expected) {
                Assert.isTrue(true, "异常类型正确");
            }
        });
    }
}
