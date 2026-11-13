package campus.utils;

import campus.common.Roles;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.PermissionDeniedException;
import campus.exception.UserExistsException;
import campus.store.UserStore;

/**
 * 参数与权限校验器。
 *
 * 校验规则属于业务规则，不属于流程控制，所以从 SystemMain 抽出来。
 * 约定：方法名用 require 开头，失败即抛异常，成功静默返回——
 * 调用方不需要判断返回值，「校验不过就停下」由异常保证。
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
     * 密码校验：空值抛参数为空；长度不足抛业务异常。
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

    /** 必填文本：不能为 null、不能为空串 */
    public static void requireNonBlank(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new EmptyParamException(label + "不能为空。");
        }
    }

    /** 允许为空的文本（如资源描述）：只挡会破坏文件格式的竖线 */
    public static void requireTextWithoutPipe(String value, String label) {
        if (value != null && value.contains(FORBIDDEN_CHARS)) {
            throw new EmptyParamException(label + "不能包含竖线符号 " + FORBIDDEN_CHARS + "。");
        }
    }

    /**
     * 要求当前操作者是管理员。
     * 身份判断收在校验器里，业务方法开头一行调用即可，不用每个方法各写一遍 if。
     */
    public static void requireAdmin(User operator, String action) {
        if (operator == null || !Roles.ADMIN.equals(operator.getRole())) {
            throw new PermissionDeniedException("当前身份无权" + action + "，该操作仅管理员可用。");
        }
    }

    /**
     * 要求当前操作者是学生。
     * 预约的提交、查看、取消都属于学生自己的动作，管理员不能代办。
     */
    public static void requireStudent(User operator, String action) {
        if (operator == null || !Roles.STUDENT.equals(operator.getRole())) {
            throw new PermissionDeniedException("当前身份无权" + action + "，该操作仅学生可用。");
        }
    }
}
