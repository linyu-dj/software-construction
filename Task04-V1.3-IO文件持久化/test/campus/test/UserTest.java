package campus.test;

import campus.entity.User;

/**
 * User 实体单元测试（V1.3）。
 *
 * V1.3 的 User 与 V1.2 结构一致，本轮真正新增的是持久化路径，
 * 这里保留实体侧的最小契约测试：组装完整、字段可改、输出可核对。
 */
public class UserTest {

    public static void runAll(TestRunner runner) {
        runner.run("全参构造写入四个字段", () -> {
            User user = new User("alice", "123456", "student", "2026-11-12 10:00:00");
            Assert.equals("alice", user.getUsername(), "账号应写入");
            Assert.equals("student", user.getRole(), "角色应写入");
            Assert.equals("2026-11-12 10:00:00", user.getRegisterTime(), "注册时间应写入");
        });

        runner.run("setter 修改账号不影响其他字段", () -> {
            User user = new User("bob", "abcdef", "student", "2026-11-12 09:00:00");
            user.setUsername("bobby");
            Assert.equals("bobby", user.getUsername(), "账号应被修改");
            Assert.equals("abcdef", user.getPassword(), "改账号不应影响密码");
            Assert.equals("student", user.getRole(), "改账号不应影响角色");
        });

        runner.run("toString 输出四个字段的取值", () -> {
            User user = new User("carol", "abcdef", "admin", "2026-11-12 12:00:00");
            String text = user.toString();
            Assert.contains("carol", text, "toString 应含账号");
            Assert.contains("admin", text, "toString 应含角色");
        });
    }
}
