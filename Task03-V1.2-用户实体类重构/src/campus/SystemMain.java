package campus;

import campus.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * V1.2 面向对象封装 —— 用户数据由「用户名-密码」的扁平映射升级为 User 对象集合。
 *
 * 本版本改动：
 * 1. 新增 campus.entity.User，承载 username / password / role / registerTime 四个字段
 * 2. 存储由 HashMap&lt;String, String&gt; 改为 List&lt;User&gt;
 * 3. 注册时组装完整对象：角色固定为 student，注册时间取当前时间
 * 4. 登录成功后打印完整用户信息，便于核对角色与注册时间
 *
 * 版本约束：本阶段仍不持久化、不抛自定义异常，分别由 V1.3、V1.4 接手。
 */
public class SystemMain {

    /** 内存用户表：存放全部 User 对象 */
    private static final List<User> USERS = new ArrayList<>();

    /** 密码最短长度 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /** 注册时间格式 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    /** 注册流程：读入账号密码，校验通过后组装 User 对象放入集合 */
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

        System.out.println("注册成功，账号 " + username + " 已创建。");
        System.out.println("注册信息：" + user);
    }

    /** 登录流程：按用户名检索 User 对象，密码比对通过则返回 true */
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

    // ==================== 校验与查询（只判断，不打印） ====================

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
        if (checkUnique && findUser(username) != null) {
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

    /** 按用户名在集合中查找用户，找不到返回 null */
    private static User findUser(String username) {
        for (User user : USERS) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    // ==================== 展示 ====================

    /** 预置两个演示账号：管理员与学生各一个，注册时间固定为版本初始化时间 */
    private static void initDefaultUsers() {
        USERS.add(new User("admin", "123456", "admin", "2026-11-13 00:00:00"));
        USERS.add(new User("student", "123456", "student", "2026-11-13 00:00:00"));
    }

    private static void printBanner() {
        System.out.println("================================================");
        System.out.println("  校园资源预约系统  Campus Resource Reservation System");
        System.out.println("  版本 V1.2  面向对象封装：User 实体类重构");
        System.out.println("================================================");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("请选择操作：[1] 登录    [2] 注册    [0] 退出");
        System.out.print("请输入选项：");
    }

    /** 登录成功后的身份提示：角色直接取自 User 字段，不再靠用户名猜测 */
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

    /** 当前时间，统一在此格式化，避免多处重复写 pattern */
    private static String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
