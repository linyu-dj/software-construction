package campus.biz;

import campus.common.Result;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.UserNotFoundException;
import campus.exception.WrongPasswordException;
import campus.store.UserStore;
import campus.utils.FileUtil;
import campus.utils.TimeUtil;
import campus.utils.Validator;

import campus.common.Roles;

/**
 * 用户业务类（V2.1）。
 *
 * 从 SystemMain 搬到这里的理由：注册和登录是业务，不是界面动作。
 * 搬走之后主类只剩下「读输入 → 调方法 → 打印 Result」。
 *
 * 返回值约定：全部返回 Result，不再出现 boolean、User 或 null。
 * 业务规则失败在方法内部捕获并转成 Result.fail，界面层不用再关心异常类型；
 * IO 这类系统故障仍然向上抛，由入口统一提示。
 */
public class UserManager {

    private final UserStore store;

    public UserManager(UserStore store) {
        this.store = store;
    }

    /** 注册：校验 → 组装 → 进内存 → 追加落盘 */
    public Result<User> register(String username, String password) {
        try {
            Validator.requireValidUsername(store, username, true);
            Validator.requireValidPassword(password, true);

            User user = new User(username, password, Roles.STUDENT, TimeUtil.now());
            store.add(user);
            FileUtil.appendUser(user);
            return Result.success("注册成功，账号 " + username + " 已创建并保存到 " + FileUtil.USER_FILE + "。", user);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 登录：校验非空 → 查账号 → 比密码 */
    public Result<User> login(String username, String password) {
        try {
            Validator.requireValidUsername(store, username, false);
            Validator.requireValidPassword(password, false);

            User user = store.findByUsername(username);
            if (user == null) {
                throw new UserNotFoundException("账号 " + username + " 尚未注册，请先注册。");
            }
            if (!user.getPassword().equals(password)) {
                throw new WrongPasswordException("密码错误，请重新输入。");
            }
            return Result.success("登录成功，欢迎您 " + username + "。", user);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 按账号查询单个用户 */
    public Result<User> findByUsername(String username) {
        try {
            Validator.requireValidUsername(store, username, false);
            User user = store.findByUsername(username);
            if (user == null) {
                throw new UserNotFoundException("账号 " + username + " 尚未注册。");
            }
            return Result.success("查询成功。", user);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }
}
