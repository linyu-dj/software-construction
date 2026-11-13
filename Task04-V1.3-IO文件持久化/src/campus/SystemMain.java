package campus;

import campus.entity.User;
import campus.utils.FileUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * V1.3 IO 文件持久化 —— 用户数据落到 user.txt，重启后自动加载。
 *
 * 本版本改动：
 * 1. 新增 campus.utils.FileUtil，封装文件创建、读取、覆盖写入、追加写入
 * 2. 启动时加载 user.txt 到内存集合；文件为空则写入两个演示账号
 * 3. 注册成功后立即追加落盘，不再依赖程序退出时的统一保存
 * 4. 用户名禁止包含竖线分隔符，避免写入文件后行格式被破坏
 *
 * 版本约束：本阶段仍用字符串提示错误，自定义异常由 V1.4 引入。
 */
public class SystemMain {

    /** 内存用户表：内容与 user.txt 保持一致 */
    private static final List<User> USERS = new ArrayList<>();

    private static final int MIN_PASSWORD_LENGTH = 6;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        initStorage();
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

    /** 启动时准备存储：确保文件存在，加载历史数据，为空则初始化演示账号 */
    private static void initStorage() {
        FileUtil.ensureFile();
        USERS.addAll(FileUtil.loadUsers());

        if (USERS.isEmpty()) {
            initDefaultUsers();
            FileUtil.saveUsers(USERS);
            System.out.println("用户数据文件为空，已写入 " + USERS.size() + " 个初始化账号。");
        } else {
            System.out.println("已从 " + FileUtil.USER_FILE + " 加载 " + USERS.size() + " 个用户。");
        }
    }

    // ==================== 流程 ====================

    /** 注册流程：校验通过后既进内存集合，也立即追加到文件 */
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

        User user = new User(username, password, "student", now());
        USERS.add(user);
        FileUtil.appendUser(user);

        System.out.println("注册成功，账号 " + username + " 已创建并保存到 " + FileUtil.USER_FILE + "。");
        System.out.println("注册信息：" + user);
    }

    /** 登录流程：在内存集合里检索 User，密码比对通过则返回 true */
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

        User user = findUser(username);
        if (user == null) {
            System.out.println("登录失败：该账号尚未注册。");
            return false;
        }
        if (!user.getPassword().equals(password)) {
            System.out.println("登录失败：密码错误，请重新输入。");
            return false;
        }

        printWelcome(user);
        return true;
    }

    // ==================== 校验与查询 ====================

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
        if (username.contains("|")) {
            return "账号不能包含竖线符号 |。";
        }
        if (checkUnique && findUser(username) != null) {
            return "账号 " + username + " 已存在，请更换。";
        }
        return null;
    }

    private static String validatePassword(String password, boolean checkStrength) {
        if (password.isEmpty()) {
            return "密码不能为空。";
        }
        if (checkStrength && password.length() < MIN_PASSWORD_LENGTH) {
            return "密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。";
        }
        return null;
    }

    private static User findUser(String username) {
        for (User user : USERS) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    // ==================== 展示 ====================

    private static void initDefaultUsers() {
        USERS.add(new User("admin", "123456", "admin", "2026-11-13 00:00:00"));
        USERS.add(new User("student", "123456", "student", "2026-11-13 00:00:00"));
    }

    private static void printBanner() {
        System.out.println("================================================");
        System.out.println("  校园资源预约系统  Campus Resource Reservation System");
        System.out.println("  版本 V1.3  IO 文件持久化");
        System.out.println("================================================");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("请选择操作：[1] 登录    [2] 注册    [0] 退出");
        System.out.print("请输入选项：");
    }

    private static void printWelcome(User user) {
        String roleLabel = "admin".equals(user.getRole()) ? "ADMIN" : "STUDENT";
        System.out.println("------------------------------------------------");
        System.out.println("登录成功！欢迎您，" + user.getUsername() + "。");
        System.out.println("当前身份：" + roleLabel);
        System.out.println("注册时间：" + user.getRegisterTime());
        if ("ADMIN".equals(roleLabel)) {
            System.out.println("可用权限：新增 / 查询 / 修改 / 删除校园资源（业务菜单将在后续版本开放）");
        } else {
            System.out.println("可用权限：查询可预约资源、提交预约申请（业务菜单将在后续版本开放）");
        }
        System.out.println("用户信息：" + user);
        System.out.println("------------------------------------------------");
    }

    private static String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
