package campus.test;

import campus.common.Roles;
import campus.entity.User;
import campus.store.UserStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户存储测试（V1.4）。
 *
 * UserStore 是「信息专家」：集合由它持有，按账号查询就由它负责。
 * 测试覆盖集合增删查、整体替换后的内容一致性，以及快照的只读性——
 * 快照如果可写，调用方就能绕过 add 直接改集合，封装形同虚设。
 */
public class UserStoreTest {

    private static User user(String username) {
        return new User(username, "123456", Roles.STUDENT, "2026-11-12 08:00:00");
    }

    public static void runAll(TestRunner runner) {
        runner.run("新增后数量与内容同步", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            store.add(user("bob"));
            Assert.equals(2, store.size(), "应有两条记录");
            Assert.isFalse(store.isEmpty(), "非空集合 isEmpty 应为 false");
        });

        runner.run("按账号查找返回对应对象", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            User found = store.findByUsername("alice");
            Assert.notNull(found, "已存在的账号应能查到");
            Assert.equals("alice", found.getUsername(), "查到的账号应一致");
        });

        runner.run("查找不存在的账号返回 null", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            Assert.isTrue(store.findByUsername("nobody") == null, "不存在的账号应返回 null");
        });

        runner.run("exists 与 findByUsername 结论一致", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            Assert.isTrue(store.exists("alice"), "已存在账号 exists 应为 true");
            Assert.isFalse(store.exists("nobody"), "不存在账号 exists 应为 false");
        });

        runner.run("replaceAll 先清空再填充", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            List<User> loaded = new ArrayList<>();
            loaded.add(user("bob"));
            store.replaceAll(loaded);
            Assert.equals(1, store.size(), "替换后只保留新数据");
            Assert.isTrue(store.findByUsername("alice") == null, "旧数据应被清空");
            Assert.notNull(store.findByUsername("bob"), "新数据应可查到");
        });

        runner.run("replaceAll 传入 null 视为清空", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            store.replaceAll(null);
            Assert.equals(0, store.size(), "传入 null 后集合应为空");
        });

        runner.run("快照不可被修改", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            try {
                store.snapshot().add(user("bob"));
                Assert.fail("快照应拒绝写操作");
            } catch (UnsupportedOperationException expected) {
                Assert.equals(1, store.size(), "快照写入失败后原集合不应变化");
            }
        });

        runner.run("快照内容与存储一致", () -> {
            UserStore store = new UserStore();
            store.add(user("alice"));
            store.add(user("bob"));
            List<User> snapshot = store.snapshot();
            Assert.equals(2, snapshot.size(), "快照应包含全部记录");
        });
    }
}
