package campus.utils;

import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.UserExistsException;
import campus.store.UserStore;

/**
 * 参数校验器。
 *
 * 从 SystemMain 里抽出来的原因：校验规则是业务规则，不是流程控制。
 * 抽出来之后 SystemMain 只剩「读输入 → 调校验 → 组装对象 → 落盘」四步编排。
 *
 * 约定：方法名用 require 开头，失败即抛异常，成功静默返回。
 * 这样调用方不用判断返回值，「校验不过就停下」由异常保证。
 */
public final class Validator {

    /** 密码最短长度，注册与提示文案共用同一常量 */
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** 用户名禁止字符：会破坏 user.txt 的竖线分隔格式 */
    private static final String FORBIDDEN_CHARS = "|";

    private Validator() {
        // 工具类不允许实例化
    }

    /**
     * 账号校验：空值、非法字符、重复各抛对应异常。
     *
     * @param store         用户数据持有者，查重时需要
     * @param requireUnique 注册场景为 true；登录只校验非空，传 false
     */
    public static void requireValidUsername(UserStore store, String username, boolean requireUnique) {
        if (username == null || username.isEmpty()) {
            throw new EmptyParamException("账号不能为空。");
        }
        if (username.contains(FORBIDDEN_CHARS)) {
            throw new EmptyParamException("账号不能包含竖线符号 " + FORBIDDEN_CHARS + "。");
        }
        if (requireUnique && store.exists(username)) {
            throw new UserExistsException("账号 " + username + " 已存在，请更换。");
        }
    }

    /**
     * 密码校验：空值抛参数为空；长度不足抛业务异常（规则未细分到独立子类时先用父类）。
     *
     * @param requireStrength 注册场景为 true；登录只校验非空，传 false
     */
    public static void requireValidPassword(String password, boolean requireStrength) {
        if (password == null || password.isEmpty()) {
            throw new EmptyParamException("密码不能为空。");
        }
        if (requireStrength && password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。");
        }
    }
}
