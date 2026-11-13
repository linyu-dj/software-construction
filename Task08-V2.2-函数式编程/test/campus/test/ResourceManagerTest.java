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
 * 资源业务测试（V2.2）。
 *
 * 本轮新增的是名称模糊查询，管理员的检索范围是全部资源、学生只限可预约资源。
 * 测试同时覆盖「查不到编号时 orElseThrow 转成失败」这条 Optional 链路，
 * 因为它替代了原先的 null 判断，是这一轮改造中最容易出错的路径。
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
        store.add(new CampusResource("R004", "二号会议室", "教室", ResourceStatus.AVAILABLE, "教学楼 4 层", ""));
        return new ResourceManager(store);
    }

    public static void runAll(TestRunner runner) {
        runner.run("学生搜索只在可预约资源内命中", () -> {
            Result<List<CampusResource>> result = managerWithSeeds().searchByName(student(), "室");
            Assert.isTrue(result.isSuccess(), "查询应成功");
            Assert.equals(2, result.getData().size(), "学生应只命中两条可预约资源");
        });

        runner.run("管理员搜索覆盖全部状态", () -> {
            Result<List<CampusResource>> result = managerWithSeeds().searchByName(admin(), "室");
            Assert.equals(3, result.getData().size(), "管理员应命中三条");
        });

        runner.run("搜索成功提示包含关键词与条数", () -> {
            String msg = managerWithSeeds().searchByName(admin(), " 会议 ").getMsg();
            Assert.contains("会议", msg, "提示应回显关键词");
            Assert.contains("2", msg, "提示应给出匹配条数");
        });

        runner.run("搜索无匹配时返回成功但列表为空", () -> {
            Result<List<CampusResource>> result = managerWithSeeds().searchByName(admin(), "不存在的名字");
            Assert.isTrue(result.isSuccess(), "无条件查询本身是成功的");
            Assert.equals(0, result.getData().size(), "列表应为空");
        });

        runner.run("学生查占用资源返回失败", () -> {
            Result<CampusResource> result = managerWithSeeds().findById(student(), "R002");
            Assert.isFalse(result.isSuccess(), "学生不应看到占用资源");
            Assert.contains("可预约", result.getMsg(), "提示应说明只开放可预约资源");
        });

        runner.run("编号为空白时返回失败", () -> {
            Result<CampusResource> result = managerWithSeeds().findById(admin(), "   ");
            Assert.isFalse(result.isSuccess(), "空白编号应失败");
            Assert.contains("不能为空", result.getMsg(), "提示应说明参数为空");
        });

        runner.run("查询不存在编号返回失败而非空指针", () -> {
            Result<CampusResource> result = managerWithSeeds().findById(admin(), "R999");
            Assert.isFalse(result.isSuccess(), "不存在编号应失败");
            Assert.isTrue(result.getData() == null, "失败时不携带数据");
        });

        runner.run("管理员新增资源后搜索可命中", () -> {
            ResourceManager manager = managerWithSeeds();
            Result<CampusResource> created = manager.create(admin(), "三号会议室", "教室", "教学楼 5 层", "");
            Assert.isTrue(created.isSuccess(), "新增应成功");
            Assert.equals(1, manager.searchByName(student(), "三号").getData().size(), "新资源应被学生搜到");
        });

        runner.run("管理员修改资源状态生效", () -> {
            ResourceManager manager = managerWithSeeds();
            Result<Void> result = manager.update(admin(), "R001", "一号会议室", "教室", "教学楼 3 层",
                    ResourceStatus.DISABLED);
            Assert.isTrue(result.isSuccess(), "修改应成功");
            Assert.equals(1, manager.searchByName(student(), "会议").getData().size(),
                    "停用后学生应只能搜到另一条会议室");
        });

        runner.run("管理员删除资源后列表减少", () -> {
            ResourceManager manager = managerWithSeeds();
            Assert.isTrue(manager.delete(admin(), "R004").isSuccess(), "删除应成功");
            Assert.equals(2, manager.listFor(admin()).getData().size(), "删除后应剩两条");
        });
    }
}
