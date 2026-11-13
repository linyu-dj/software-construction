package campus.test;

import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.store.ResourceStore;

import java.util.List;
import java.util.Optional;

/**
 * 资源查询测试（V2.2）。
 *
 * 这一轮把查询改成 Stream 与 Optional。测试要证明两件事：
 * 返回 Optional 的方法在「找不到」时不可能是 null；组合查询的条件语义
 * 与旧版逐条循环一致（状态为空表示不限、关键词为空表示不限）。
 */
public class ResourceStoreTest {

    private static ResourceStore seeded() {
        ResourceStore store = new ResourceStore();
        store.add(new CampusResource("R001", "一号会议室", "教室", ResourceStatus.AVAILABLE, "教学楼 3 层", ""));
        store.add(new CampusResource("R002", "302 实验室", "实验室", ResourceStatus.OCCUPIED, "实验楼 3 层", ""));
        store.add(new CampusResource("R004", "二号会议室", "教室", ResourceStatus.AVAILABLE, "教学楼 4 层", ""));
        store.add(new CampusResource("R005", "投影仪 A", "设备", ResourceStatus.DISABLED, "器材室", ""));
        return store;
    }

    public static void runAll(TestRunner runner) {
        runner.run("按编号查找命中返回 Optional 中的对象", () -> {
            Optional<CampusResource> found = seeded().findById("R002");
            Assert.isTrue(found.isPresent(), "存在的编号应有值");
            Assert.equals("302 实验室", found.get().getName(), "对象应正确");
        });

        runner.run("按编号查找未命中返回空 Optional", () -> {
            Assert.isFalse(seeded().findById("R999").isPresent(), "不存在编号应为空");
        });

        runner.run("编号为 null 时返回空 Optional 而非抛异常", () -> {
            Assert.isFalse(seeded().findById(null).isPresent(), "null 编号应安全返回空值");
        });

        runner.run("按状态筛选只返回匹配项且编号升序", () -> {
            List<CampusResource> available = seeded().findByStatus(ResourceStatus.AVAILABLE);
            Assert.equals(2, available.size(), "可预约资源应为两条");
            Assert.equals("R001", available.get(0).getId(), "应按编号升序");
            Assert.equals("R004", available.get(1).getId(), "第二条编号应为 R004");
        });

        runner.run("名称模糊查询忽略大小写", () -> {
            List<CampusResource> matched = seeded().findByNameKeyword("投影仪");
            Assert.equals(1, matched.size(), "应命中一条");
            Assert.equals("R005", matched.get(0).getId(), "命中的应是投影仪");
        });

        runner.run("名称查询自动去首尾空格", () -> {
            Assert.equals(1, seeded().findByNameKeyword("  投影仪 A  ").size(), "空格不应影响命中");
        });

        runner.run("关键词为空时返回全部", () -> {
            Assert.equals(4, seeded().findByNameKeyword(null).size(), "null 关键词视为不限");
            Assert.equals(4, seeded().findByNameKeyword("   ").size(), "空白关键词视为不限");
        });

        runner.run("组合查询同时生效", () -> {
            List<CampusResource> result = seeded().search(ResourceStatus.AVAILABLE, "会议");
            Assert.equals(2, result.size(), "可预约且名称含会议应为两条");
            Assert.equals("R001", result.get(0).getId(), "应按编号升序");
        });

        runner.run("组合查询状态为空表示不限", () -> {
            Assert.equals(3, seeded().search(null, "室").size(), "名称含室的三条均应返回");
        });

        runner.run("组合查询关键词为空表示不限", () -> {
            Assert.equals(2, seeded().search(ResourceStatus.AVAILABLE, "").size(), "可预约两条均应返回");
        });

        runner.run("两个条件都为空时返回全部", () -> {
            Assert.equals(4, seeded().search(null, null).size(), "无条件时应返回四条");
        });

        runner.run("编号生成不复用已删除的中间编号", () -> {
            ResourceStore store = seeded();
            store.removeById("R002");
            Assert.equals("R006", store.nextId(), "最大编号为 R005，下一个应为 R006");
        });

        runner.run("removeById 使用条件删除并返回结果", () -> {
            ResourceStore store = seeded();
            Assert.isTrue(store.removeById("R001"), "删除存在编号应返回 true");
            Assert.isFalse(store.removeById("R001"), "重复删除应返回 false");
            Assert.equals(3, store.size(), "删除后应剩三条");
        });
    }
}
