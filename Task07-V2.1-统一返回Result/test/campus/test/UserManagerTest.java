package campus.test;

import campus.biz.UserManager;
import campus.common.Result;
import campus.common.Roles;
import campus.entity.User;
import campus.store.UserStore;

/**
 * 用户业务测试（V2.1）。
 *
 * 这一轮的关键变化是「异常不再抛给界面」：业务规则失败在方法内部转成 Result.fail，
 * 调用方只需判断 isSuccess。测试因此逐条验证「哪种输入返回失败、失败提示是否可读」，
 * 并覆盖一条成功路径以确认数据确实进入存储。
 */
public class UserManagerTest {

    private static UserManager emptyManager() {
        return new UserManager(new UserStore());
    }

    public static void runAll(TestRunner runner) {
        runner.run("注册成功后账号进入存储", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            Result<User> result = manager.register("alice", "123456");
            Assert.isTrue(result.isSuccess(), "合法输入应注册成功");
            Assert.contains("alice", result.getMsg(), "提示应包含账号");
            Assert.equals(1, store.size(), "存储应新增一条记录");
            Assert.equals(Roles.STUDENT, result.getData().getRole(), "注册用户默认是学生");
        });

        runner.run("重复账号注册返回失败", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("alice", "123456");
            Result<User> second = manager.register("alice", "654321");
            Assert.isFalse(second.isSuccess(), "重复账号应失败");
            Assert.contains("已存在", second.getMsg(), "提示应说明冲突原因");
            Assert.equals(1, store.size(), "失败不应写入存储");
        });

        runner.run("密码过短注册返回失败", () -> {
            Result<User> result = emptyManager().register("bob", "12345");
            Assert.isFalse(result.isSuccess(), "五位密码应失败");
            Assert.contains("6", result.getMsg(), "提示应给出长度要求");
        });

        runner.run("登录成功返回用户对象", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("carol", "123456");
            Result<User> login = manager.login("carol", "123456");
            Assert.isTrue(login.isSuccess(), "账号密码正确应登录成功");
            Assert.equals("carol", login.getData().getUsername(), "应返回对应用户");
        });

        runner.run("密码错误登录返回失败", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("dave", "123456");
            Result<User> login = manager.login("dave", "000000");
            Assert.isFalse(login.isSuccess(), "密码错误应失败");
            Assert.contains("密码", login.getMsg(), "提示应指向密码");
        });

        runner.run("未注册账号登录返回失败", () -> {
            Result<User> login = emptyManager().login("nobody", "123456");
            Assert.isFalse(login.isSuccess(), "未注册账号应失败");
            Assert.contains("尚未注册", login.getMsg(), "提示应说明账号不存在");
        });

        runner.run("账号为空登录返回失败", () -> {
            Result<User> login = emptyManager().login("", "123456");
            Assert.isFalse(login.isSuccess(), "空账号应失败");
            Assert.contains("不能为空", login.getMsg(), "提示应说明为空");
        });

        runner.run("按账号查询成功返回用户", () -> {
            UserStore store = new UserStore();
            UserManager manager = new UserManager(store);
            manager.register("erin", "123456");
            Result<User> found = manager.findByUsername("erin");
            Assert.isTrue(found.isSuccess(), "已注册账号应查到");
            Assert.equals("erin", found.getData().getUsername(), "返回账号应一致");
        });

        runner.run("按账号查询不存在返回失败", () -> {
            Result<User> found = emptyManager().findByUsername("nobody");
            Assert.isFalse(found.isSuccess(), "未注册账号应失败");
            Assert.isTrue(found.getData() == null, "失败时不携带数据");
        });
    }
}
