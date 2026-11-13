package campus.test;

import campus.entity.User;

/**
 * User 实体单元测试（V1.2）。
 *
 * 这一轮 User 只有字段与访问方法，测的重点是「对象的契约」：
 * 构造完成后字段齐全、setter 只改目标字段、toString 能反映真实数据。
 * 契约稳定，后面几轮给 User 加 equals/hashCode、加角色常量时才不会牵动调用方。
 */
public class UserTest {

    public static void runAll(TestRunner runner) {
        runner.run("全参构造按顺序写入四个字段", () -> {
            User user = new User("alice", "123456", "student", "2026-11-12 10:00:00");
            Assert.equals("alice", user.getUsername(), "账号应写入");
            Assert.equals("123456", user.getPassword(), "密码应写入");
            Assert.equals("student", user.getRole(), "角色应写入");
            Assert.equals("2026-11-12 10:00:00", user.getRegisterTime(), "注册时间应写入");
        });

        runner.run("无参构造创建空对象", () -> {
            User user = new User();
            Assert.isTrue(user.getUsername() == null, "无参构造不预置账号");
            Assert.isTrue(user.getPassword() == null, "无参构造不预置密码");
            Assert.isTrue(user.getRole() == null, "无参构造不预置角色");
            Assert.isTrue(user.getRegisterTime() == null, "无参构造不预置注册时间");
        });

        runner.run("setter 只改目标字段", () -> {
            User user = new User("bob", "abcdef", "student", "2026-11-12 09:00:00");
            user.setPassword("654321");
            Assert.equals("654321", user.getPassword(), "密码应被修改");
            Assert.equals("bob", user.getUsername(), "改密码不应影响账号");
            Assert.equals("student", user.getRole(), "改密码不应影响角色");
            Assert.equals("2026-11-12 09:00:00", user.getRegisterTime(), "改密码不应影响注册时间");
        });

        runner.run("角色可被提升为管理员", () -> {
            User user = new User("carol", "abcdef", "student", "2026-11-12 09:30:00");
            user.setRole("admin");
            Assert.equals("admin", user.getRole(), "角色应被修改");
        });

        runner.run("toString 包含账号、角色与注册时间", () -> {
            User user = new User("dave", "abcdef", "student", "2026-11-12 11:00:00");
            String text = user.toString();
            Assert.contains("dave", text, "toString 应含账号");
            Assert.contains("student", text, "toString 应含角色");
            Assert.contains("2026-11-12 11:00:00", text, "toString 应含注册时间");
        });
    }
}
