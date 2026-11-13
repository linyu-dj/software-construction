package campus;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * V1.1 用户注册功能 —— 账号从硬编码改为集合管理，新增注册入口。
 *
 * 本版本改动：
 * 1. 主菜单由「1 登录 / 0 退出」改为「1 登录 / 2 注册 / 0 退出」
 * 2. 账号密码由四个局部变量改为 HashMap&lt;String, String&gt; 内存存储
 * 3. 新增注册流程：用户名唯一查重、非空校验、密码长度 >= 6
 * 4. 账号校验与密码合法性校验抽成独立方法，主流程只负责输入输出与调度
 *
 * 版本约束：本阶段仍不做分层，实体、持久化、异常留到 V1.2 起的迭代处理。
 */
public class SystemMain {

    /** 内存用户表：用户名 -> 密码 */
    private static final Map<String, String> USERS = new HashMap<>();

    /** 密码最短长度 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        initDefaultUsers();
        printBanner();

        boolean loggedIn = false;
        while (!loggedIn) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            if ("0".equals(choice)) {
                System.out.println("已退出系统，再见。");
                break;
            }
            if ("1".equals(choice)) {
                loggedIn = doLogin(scanner);
            } else if ("2".equals(choice)) {
                doRegister(scanner);
            } else {
                System.out.println("无效选项，请输入 0、1 或 2。");
            }
        }

        scanner.close();
    }

    // ==================== 流程 ====================

    /** 注册流程：读入账号密码，校验通过后写入内存用户表 */
    private static void doRegister(Scanner scanner) {
        System.out.println();
        System.out.println("---- 用户注册 ----");
        System.out.print("请输入新账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入新密码（不少于 " + MIN_PASSWORD_LENGTH + " 位）：");
        String password = scanner.nextLine().trim();

        String usernameError = validateUsername(username, true);
        if (usernameError != null) {
            System.out.println("注册失败：" + usernameError);
            return;
        }
        String passwordError = validatePassword(password, true);
        if (passwordError != null) {
            System.out.println("注册失败：" + passwordError);
            return;
        }

        USERS.put(username, password);
        System.out.println("注册成功，账号 " + username + " 已创建，请使用该账号登录。");
    }

    /** 登录流程：校验输入后比对内存用户表，成功返回 true */
    private static boolean doLogin(Scanner scanner) {
        System.out.println();
        System.out.println("---- 用户登录 ----");
        System.out.print("账号：");
        String username = scanner.nextLine().trim();
        System.out.print("密码：");
        String password = scanner.nextLine().trim();

        String usernameError = validateUsername(username, false);
        if (usernameError != null) {
            System.out.println("登录失败：" + usernameError);
            return false;
        }
        String passwordError = validatePassword(password, false);
        if (passwordError != null) {
            System.out.println("登录失败：" + passwordError);
            return false;
        }

        String storedPassword = USERS.get(username);
        if (storedPassword == null) {
            System.out.println("登录失败：该账号尚未注册。");
            return false;
        }
        if (!storedPassword.equals(password)) {
            System.out.println("登录失败：密码错误，请重新输入。");
            return false;
        }

        printWelcome(username);
        return true;
    }

    // ==================== 校验（单一职责：只判断，不打印） ====================

    /**
     * 账号校验。
     *
     * @param username    待校验账号
     * @param checkUnique 注册场景需要查重，登录场景不需要
     * @return 校验不通过时返回错误描述，通过返回 null
     */
    private static String validateUsername(String username, boolean checkUnique) {
        if (username.isEmpty()) {
            return "账号不能为空。";
        }
        if (checkUnique && USERS.containsKey(username)) {
            return "账号 " + username + " 已存在，请更换。";
        }
        return null;
    }

    /**
     * 密码合法性校验。
     *
     * @param password      待校验密码
     * @param checkStrength 注册场景校验长度，登录场景只校验非空
     * @return 校验不通过时返回错误描述，通过返回 null
     */
    private static String validatePassword(String password, boolean checkStrength) {
        if (password.isEmpty()) {
            return "密码不能为空。";
        }
        if (checkStrength && password.length() < MIN_PASSWORD_LENGTH) {
            return "密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。";
        }
        return null;
    }

    // ==================== 展示 ====================

    /** 预置两个演示账号，仅用于首次运行，注册的新账号同样存在内存用户表里 */
    private static void initDefaultUsers() {
        USERS.put("admin", "123456");
        USERS.put("student", "123456");
    }

    private static void printBanner() {
        System.out.println("================================================");
        System.out.println("  校园资源预约系统  Campus Resource Reservation System");
        System.out.println("  版本 V1.1  用户注册功能");
        System.out.println("================================================");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("请选择操作：[1] 登录    [2] 注册    [0] 退出");
        System.out.print("请输入选项：");
    }

    /** 登录成功后的身份提示：管理员账号显示 ADMIN，其余按学生处理（角色字段在 V1.2 引入） */
    private static void printWelcome(String username) {
        String role = "admin".equals(username) ? "ADMIN" : "STUDENT";
        System.out.println("------------------------------------------------");
        System.out.println("登录成功！欢迎您，" + username + "。");
        System.out.println("当前身份：" + role);
        if ("ADMIN".equals(role)) {
            System.out.println("可用权限：新增 / 查询 / 修改 / 删除校园资源（业务菜单将在后续版本开放）");
        } else {
            System.out.println("可用权限：查询可预约资源、提交预约申请（业务菜单将在后续版本开放）");
        }
        System.out.println("------------------------------------------------");
    }
}
