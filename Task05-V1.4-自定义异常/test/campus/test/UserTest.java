package campus.test;

import campus.common.Roles;
import campus.entity.User;

/**
 * User 实体契约测试（V1.4）。
 *
 * 这一轮给 User 补了 equals 与 hashCode，规则是按账号判断同一性：
 * 同一账号无论密码是否改动，都视为同一个用户。
 * 这条契约直接影响查重、去重与后续集合容器的正确性，所以单独作为测试点。
 */
public class UserTest {

    public static void runAll(TestRunner runner) {
        runner.run("账号相同即视为同一用户", () -> {
            User first = new User("alice", "123456", Roles.STUDENT, "2026-11-12 10:00:00");
            User second = new User("alice", "999999", Roles.STUDENT, "2026-11-13 08:00:00");
            Assert.isTrue(first.equals(second), "账号相同应相等，密码与时间不参与判断");
        });

        runner.run("账号不同则不相等", () -> {
            User first = new User("alice", "123456", Roles.STUDENT, "2026-11-12 10:00:00");
            User second = new User("bob", "123456", Roles.STUDENT, "2026-11-12 10:00:00");
            Assert.isFalse(first.equals(second), "账号不同应不相等");
        });

        runner.run("相等对象的 hash 值一致", () -> {
            User first = new User("carol", "123456", Roles.STUDENT, "2026-11-12 10:00:00");
            User second = new User("carol", "abcdef", Roles.ADMIN, "2026-11-13 09:00:00");
            Assert.equals(first.hashCode(), second.hashCode(), "equals 为真时 hashCode 必须相同");
        });

        runner.run("与 null 和非 User 类型比较不抛异常", () -> {
            User user = new User("dave", "123456", Roles.STUDENT, "2026-11-12 10:00:00");
            Assert.isFalse(user.equals(null), "与 null 比较应返回 false");
            Assert.isFalse(user.equals("dave"), "与字符串比较应返回 false");
        });

        runner.run("角色常量取值为 admin 与 student", () -> {
            Assert.equals("admin", Roles.ADMIN, "管理员角色取值应保持小写英文");
            Assert.equals("student", Roles.STUDENT, "学生角色取值应保持小写英文");
        });
    }
}
