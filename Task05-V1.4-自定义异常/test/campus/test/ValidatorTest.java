package campus.test;

import campus.common.Roles;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.UserExistsException;
import campus.store.UserStore;
import campus.utils.Validator;

/**
 * 校验规则测试（V1.4）。
 *
 * 校验器是「契约的第一道门」：它在入口处把非法输入拦下来，
 * 使业务方法可以假定拿到的参数已经合法。测试要证明三件事：
 * 非法输入抛的是哪一个异常子类、合法输入不抛异常、登录场景不触发查重。
 */
public class ValidatorTest {

    private static UserStore storeWith(String username) {
        UserStore store = new UserStore();
        store.add(new User(username, "123456", Roles.STUDENT, "2026-11-12 08:00:00"));
        return store;
    }

    public static void runAll(TestRunner runner) {
        runner.run("空账号抛参数为空异常", () -> {
            try {
                Validator.requireValidUsername(new UserStore(), "", true);
                Assert.fail("空账号应被拒绝");
            } catch (EmptyParamException expected) {
                Assert.contains("不能为空", expected.getMessage(), "提示应说明原因为空");
            }
        });

        runner.run("null 账号抛参数为空异常", () -> {
            try {
                Validator.requireValidUsername(new UserStore(), null, false);
                Assert.fail("null 账号应被拒绝");
            } catch (EmptyParamException expected) {
                Assert.isTrue(true, "异常类型正确");
            }
        });

        runner.run("账号含竖线被拒绝", () -> {
            try {
                Validator.requireValidUsername(new UserStore(), "a|b", true);
                Assert.fail("含分隔符的账号应被拒绝");
            } catch (EmptyParamException expected) {
                Assert.contains("竖线", expected.getMessage(), "提示应指出非法字符");
            }
        });

        runner.run("重复账号在注册场景抛已存在异常", () -> {
            try {
                Validator.requireValidUsername(storeWith("alice"), "alice", true);
                Assert.fail("重复账号应被拒绝");
            } catch (UserExistsException expected) {
                Assert.contains("alice", expected.getMessage(), "提示应包含冲突账号");
            }
        });

        runner.run("登录场景不校验重复", () -> {
            Validator.requireValidUsername(storeWith("alice"), "alice", false);
        });

        runner.run("密码短于六位在注册场景被拒绝", () -> {
            try {
                Validator.requireValidPassword("12345", true);
                Assert.fail("五位密码应被拒绝");
            } catch (BusinessException expected) {
                Assert.contains("6", expected.getMessage(), "提示应说明长度要求");
            }
        });

        runner.run("登录场景不校验密码长度", () -> {
            Validator.requireValidPassword("12345", false);
        });

        runner.run("六位密码在注册场景通过", () -> {
            Validator.requireValidPassword("123456", true);
        });

        runner.run("空密码抛参数为空异常", () -> {
            try {
                Validator.requireValidPassword("", false);
                Assert.fail("空密码应被拒绝");
            } catch (EmptyParamException expected) {
                Assert.isTrue(true, "异常类型正确");
            }
        });
    }
}
