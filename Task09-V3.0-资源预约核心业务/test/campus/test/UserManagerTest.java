package campus.test;

import campus.biz.UserManager;
import campus.common.Result;
import campus.entity.User;
import campus.store.UserStore;

/** 用户业务单元测试：注册校验、登录匹配、按账号查询 */
public class UserManagerTest {

    public static void runAll(TestRunner runner) {
        runner.run("注册成功并写入内存", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            Result<User> result = manager.register("zhangsan", "123456");

            Assert.isTrue(result.isSuccess(), "注册应成功");
            Assert.contains("注册成功", result.getMsg(), "提示应包含注册成功");
            Assert.equals("zhangsan", result.getData().getUsername(), "返回用户账号应为 zhangsan");
            Assert.equals(1, store.size(), "内存中应有 1 个用户");
        });

        runner.run("重复账号注册失败", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("zhangsan", "123456");
            Result<User> second = manager.register("zhangsan", "654321");

            Assert.isFalse(second.isSuccess(), "重复账号不应注册成功");
            Assert.contains("已存在", second.getMsg(), "提示应说明账号已存在");
            Assert.equals(1, store.size(), "失败后用户数不应增加");
        });

        runner.run("密码长度不足被拒绝", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            Result<User> result = manager.register("lisi", "123");

            Assert.isFalse(result.isSuccess(), "密码过短应失败");
            Assert.contains("密码长度不能少于", result.getMsg(), "提示应说明密码长度");
        });

        runner.run("账号为空被拒绝", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            Result<User> result = manager.register("", "123456");

            Assert.isFalse(result.isSuccess(), "空账号应失败");
            Assert.contains("账号不能为空", result.getMsg(), "提示应说明账号为空");
        });

        runner.run("登录成功返回用户", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("wangwu", "123456");
            Result<User> result = manager.login("wangwu", "123456");

            Assert.isTrue(result.isSuccess(), "正确账号密码应登录成功");
            Assert.equals("wangwu", result.getData().getUsername(), "登录应返回对应用户");
        });

        runner.run("登录密码错误", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("wangwu", "123456");
            Result<User> result = manager.login("wangwu", "000000");

            Assert.isFalse(result.isSuccess(), "密码错误应失败");
            Assert.contains("密码错误", result.getMsg(), "提示应说明密码错误");
        });

        runner.run("登录账号不存在", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            Result<User> result = manager.login("nobody", "123456");

            Assert.isFalse(result.isSuccess(), "未注册账号应失败");
            Assert.contains("尚未注册", result.getMsg(), "提示应说明账号未注册");
        });

        runner.run("按账号查询成功与失败", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("zhaoliu", "123456");

            Result<User> found = manager.findByUsername("zhaoliu");
            Assert.isTrue(found.isSuccess(), "已注册账号应能查到");
            Assert.equals("zhaoliu", found.getData().getUsername(), "查到的账号应一致");

            Result<User> missing = manager.findByUsername("ghost");
            Assert.isFalse(missing.isSuccess(), "未注册账号查询应失败");
            Assert.contains("尚未注册", missing.getMsg(), "提示应说明账号未注册");
        });
    }
}
