package campus;

import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.UserExistsException;
import campus.exception.UserNotFoundException;
import campus.exception.WrongPasswordException;
import campus.utils.FileUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * V1.4 自定义异常 —— 业务失败改用异常表达，入口统一捕获并输出中文提示。
 *
 * 本版本改动：
 * 1. 新增 campus.exception.BusinessException 作为统一父类（继承 RuntimeException）
 * 2. 新增四个子异常：用户已存在、用户不存在、密码错误、参数为空
 * 3. 校验方法与登录流程改为主动抛出异常，不再返回错误字符串
 * 4. main 的菜单循环统一捕获，只打印 message，不调用 printStackTrace
 *
 * 版本约束：业务异常与系统异常分开处理，系统异常不向用户暴露堆栈。
 */
public class SystemMain {

    private static final List<User> USERS = new ArrayList<>();

    private static final int MIN_PASSWORD_LENGTH = 6;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
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

                try {
                    if ("1".equals(choice)) {
                        loggedIn = doLogin(scanner);
                    } else if ("2".equals(choice)) {
                        doRegister(scanner);
                    } else {
                        System.out.println("无效选项，请输入 0、1 或 2。");
                    }
                } catch (BusinessException e) {
                    // 业务失败：只输出中文提示，堆栈对用户没有意义
                    System.out.println("操作未完成：" + e.getMessage());
                } catch (Exception e) {
                    // 非业务异常：同样不打印堆栈，避免暴露内部实现
                    System.out.println("系统异常，请稍后重试。");
                }
            }
        } finally {
            scanner.close();
        }
    }

    // ==================== 存储初始化 ====================

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

    /** 注册流程：校验失败抛异常，成功后内存与文件同时更新 */
    private static void doRegister(Scanner scanner) {
        System.out.println();
        System.out.println("---- 用户注册 ----");
        System.out.print("请输入新账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入新密码（不少于 " + MIN_PASSWORD_LENGTH + " 位）：");
        String password = scanner.nextLine().trim();

        validateUsername(username, true);
        validatePassword(password, true);

        User user = new User(username, password, "student", now());
        USERS.add(user);
        FileUtil.appendUser(user);

        System.out.println("注册成功，账号 " + username + " 已创建并保存到 " + FileUtil.USER_FILE + "。");
        System.out.println("注册信息：" + user);
    }

    /** 登录流程：账号不存在、密码错误各自抛出对应异常 */
    private static boolean doLogin(Scanner scanner) {
        System.out.println();
        System.out.println("---- 用户登录 ----");
        System.out.print("账号：");
        String username = scanner.nextLine().trim();
        System.out.print("密码：");
        String password = scanner.nextLine().trim();

        validateUsername(username, false);
        validatePassword(password, false);

        User user = findUser(username);
        if (user == null) {
            throw new UserNotFoundException("账号 " + username + " 尚未注册，请先注册。");
        }
        if (!user.getPassword().equals(password)) {
            throw new WrongPasswordException("密码错误，请重新输入。");
        }

        printWelcome(user);
        return true;
    }

    // ==================== 校验（失败即抛出） ====================

    /**
     * 账号校验：空值、非法字符、重复各自抛出不同异常。
     *
     * @param checkUnique 注册场景需要查重，登录场景不需要
     */
    private static void validateUsername(String username, boolean checkUnique) {
        if (username.isEmpty()) {
            throw new EmptyParamException("账号不能为空。");
        }
        if (username.contains("|")) {
            throw new EmptyParamException("账号不能包含竖线符号 |。");
        }
        if (checkUnique && findUser(username) != null) {
            throw new UserExistsException("账号 " + username + " 已存在，请更换。");
        }
    }

    /**
     * 密码校验：空值抛出参数为空异常；长度不足暂用父类异常表达，
     * 后续若这类规则增多，可再补一个 PasswordInvalidException 子类，
     * 此处新增不需要改动调用方。
     *
     * @param checkStrength 注册场景校验长度，登录场景只校验非空
     */
    private static void validatePassword(String password, boolean checkStrength) {
        if (password.isEmpty()) {
            throw new EmptyParamException("密码不能为空。");
        }
        if (checkStrength && password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。");
        }
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
        System.out.println("  版本 V1.4  自定义异常");
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
