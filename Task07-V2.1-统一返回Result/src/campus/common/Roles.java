package campus.common;

/**
 * 角色常量。
 *
 * 出现的背景：V1.1~V1.4 里 "admin" / "student" 这两个字面量散落在登录判断、
 * 默认账号初始化、展示逻辑中，改一处漏一处就会出错。集中在这里之后，
 * 全项目只有本文件出现这两个字符串。
 *
 * 为什么不用枚举：role 字段要按原样写入 user.txt（admin|student），
 * 枚举要么改字段类型、要么到处 name().toLowerCase()，读起来反而绕。
 * 等资源状态（AVAILABLE/OCCUPIED/DISABLED）那种封闭集合再上枚举更合适。
 */
public final class Roles {

    /** 管理员 */
    public static final String ADMIN = "admin";

    /** 学生 */
    public static final String STUDENT = "student";

    private Roles() {
        // 常量类不允许实例化
    }
}
