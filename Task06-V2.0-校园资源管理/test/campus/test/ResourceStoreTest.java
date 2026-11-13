package campus.test;

import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.store.ResourceStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源存储测试（V2.0）。
 *
 * 存储类承担两件事：集合管理与编号生成。编号生成规则是「取现有最大序号加一」，
 * 因此删掉中间编号不会回收空号——测试专门覆盖这一点，因为编号复用会让
 * 历史记录与新资源指向同一个标识，属于数据层面的隐患。
 */
public class ResourceStoreTest {

    private static CampusResource resource(String id, ResourceStatus status) {
        return new CampusResource(id, "资源" + id, "教室", status, "教学楼", "");
    }

    public static void runAll(TestRunner runner) {
        runner.run("空集合首个编号为 R001", () -> {
            Assert.equals("R001", new ResourceStore().nextId(), "空集合应从 R001 开始");
        });

        runner.run("编号按最大序号加一顺延", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            Assert.equals("R002", store.nextId(), "已有 R001 时下一个应为 R002");
            store.add(resource("R007", ResourceStatus.DISABLED));
            Assert.equals("R008", store.nextId(), "已有 R007 时下一个应为 R008");
        });

        runner.run("删除中间编号后不复用空号", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            store.add(resource("R002", ResourceStatus.AVAILABLE));
            store.removeById("R001");
            Assert.equals("R003", store.nextId(), "删掉 R001 后仍应给出 R003");
        });

        runner.run("按编号查找，不存在返回 null", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            Assert.equals("R001", store.findById("R001").getId(), "存在的编号应查到");
            Assert.isTrue(store.findById("R999") == null, "不存在的编号应返回 null");
            Assert.isTrue(store.findById(null) == null, "null 编号不应抛异常");
        });

        runner.run("列表按编号升序返回", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R003", ResourceStatus.AVAILABLE));
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            store.add(resource("R002", ResourceStatus.OCCUPIED));
            List<CampusResource> all = store.findAllSortedById();
            Assert.equals("R001", all.get(0).getId(), "第一位应为 R001");
            Assert.equals("R002", all.get(1).getId(), "第二位应为 R002");
            Assert.equals("R003", all.get(2).getId(), "第三位应为 R003");
        });

        runner.run("removeById 返回是否删除成功", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            Assert.isTrue(store.removeById("R001"), "删除存在编号应返回 true");
            Assert.isFalse(store.removeById("R001"), "重复删除应返回 false");
        });

        runner.run("快照不可被修改", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            try {
                store.snapshot().add(resource("R002", ResourceStatus.AVAILABLE));
                Assert.fail("快照应拒绝写操作");
            } catch (UnsupportedOperationException expected) {
                Assert.equals(1, store.size(), "拒绝写入后集合大小不变");
            }
        });

        runner.run("replaceAll 覆盖原有内容", () -> {
            ResourceStore store = new ResourceStore();
            store.add(resource("R001", ResourceStatus.AVAILABLE));
            List<CampusResource> loaded = new ArrayList<>();
            loaded.add(resource("R005", ResourceStatus.DISABLED));
            store.replaceAll(loaded);
            Assert.equals(1, store.size(), "替换后只保留新数据");
            Assert.isTrue(store.findById("R001") == null, "旧数据应被清空");
        });
    }
}
