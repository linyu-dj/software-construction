package campus;

import campus.common.Roles;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.UserNotFoundException;
import campus.exception.WrongPasswordException;
import campus.store.UserStore;
import campus.utils.FileUtil;
import campus.utils.TimeUtil;
import campus.utils.Validator;

import java.util.Scanner;

/**
 * V1.4 自定义异常 —— 业务失败改用异常表达，入口统一捕获并输出中文提示。
 *
 * 本版本改动（功能定稿）：
 * 1. 新增 campus.exception.BusinessException 作为统一父类（继承 RuntimeException）
 * 2. 新增四个子异常：用户已存在、用户不存在、密码错误、参数为空
 * 3. 校验方法与登录流程改为主动抛出异常，不再返回错误字符串
 * 4. main 的菜单循环统一捕获，只打印 message，不调用 printStackTrace
 *
 * 定稿后的工程加固（tag v1.4.1）：
 * 1. 用户集合从本类抽出为 campus.store.UserStore（信息专家），本类不再直接操作 List
 * 2. 校验规则抽出为 campus.utils.Validator，本类只负责流程编排
 * 3. 角色、密码长度、菜单选项等字面量收敛为常量，消除魔法值
 * 4. FileUtil 不再吞 IO 异常，改为抛出由入口兜底；本类不再打印文件路径细节
 * 5. 本类职责收敛为：读输入 → 调校验 → 组装对象 → 落盘 → 展示
 */
public class SystemMain {

    /** 内存用户存储（信息专家：集合与查询都归它） */
    private static final UserStore USER_STORE = new UserStore();

    /** 菜单选项取值 */
    private static final String CHOICE_EXIT = "0";
    private static final String CHOICE_LOGIN = "1";
    private static final String CHOICE_REGISTER = "2";

    /** 初始化账号的默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    /** 初始化账号的注册时间：版本初始化时刻，不代表真实注册行为 */
    private static final String DEFAULT_REGISTER_TIME = "2026-11-13 00:00:00";

    public static void main(String[] args) {
        // Scanner 实现 AutoCloseable，用 try-with-resources 保证任何退出路径都会关闭
        try (Scanner scanner = new Scanner(System.in)) {
            try {
                initStorage();
                printBanner();
                runMenuLoop(scanner);
            } catch (Exception e) {
                // 未被业务捕获的异常一律兜在这里：不打印堆栈，避免暴露内部实现
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    // ==================== 存储初始化 ====================

    /** 确保文件存在 → 加载历史数据 → 空则写入初始化账号 */
    private static void initStorage() {
        if (FileUtil.ensureFile()) {
            System.out.println("未找到用户数据文件，已自动创建 " + FileUtil.USER_FILE);
        }

        USER_STORE.replaceAll(FileUtil.loadUsers());

        if (USER_STORE.isEmpty()) {
            seedDefaultUsers();
            FileUtil.saveUsers(USER_STORE.snapshot());
            System.out.println("用户数据文件为空，已写入 " + USER_STORE.size() + " 个初始化账号。");
        } else {
            System.out.println("已从 " + FileUtil.USER_FILE + " 加载 " + USER_STORE.size() + " 个用户。");
        }
    }

    /** 预置演示账号：管理员与学生各一个 */
    private static void seedDefaultUsers() {
        USER_STORE.add(new User(Roles.ADMIN, DEFAULT_PASSWORD, Roles.ADMIN, DEFAULT_REGISTER_TIME));
        USER_STORE.add(new User(Roles.STUDENT, DEFAULT_PASSWORD, Roles.STUDENT, DEFAULT_REGISTER_TIME));
    }

    // ==================== 菜单循环 ====================

    /** 主循环：读选项 → 分发 → 捕获业务异常后继续，登录成功或退出才结束 */
    private static void runMenuLoop(Scanner scanner) {
        boolean loggedIn = false;
        while (!loggedIn) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            if (CHOICE_EXIT.equals(choice)) {
                System.out.println("已退出系统，再见。");
                return;
            }

            try {
                loggedIn = dispatch(choice, scanner);
            } catch (BusinessException e) {
                // 业务失败：message 本身就是给用户看的中文句子，堆栈对用户没有意义
                System.out.println("操作未完成：" + e.getMessage());
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    /** 选项分发。返回 true 表示本轮已登录成功，主循环据此结束 */
    private static boolean dispatch(String choice, Scanner scanner) {
        if (CHOICE_LOGIN.equals(choice)) {
            return login(scanner);
        }
        if (CHOICE_REGISTER.equals(choice)) {
            register(scanner);
            return false;
        }
        System.out.println("无效选项，请输入 " + CHOICE_EXIT + "、" + CHOICE_LOGIN + " 或 " + CHOICE_REGISTER + "。");
        return false;
    }

    // ==================== 业务流程 ====================

    /** 登录：校验 → 查用户 → 比对密码 → 展示身份。失败路径全部抛异常 */
    private static boolean login(Scanner scanner) {
        printSection("用户登录");
        String username = readLine(scanner, "账号：");
        String password = readLine(scanner, "密码：");

        Validator.requireValidUsername(USER_STORE, username, false);
        Validator.requireValidPassword(password, false);

        User user = USER_STORE.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("账号 " + username + " 尚未注册，请先注册。");
        }
        if (!user.getPassword().equals(password)) {
            throw new WrongPasswordException("密码错误，请重新输入。");
        }

        printWelcome(user);
        return true;
    }

    /** 注册：校验 → 组装对象 → 进内存 → 追加落盘 */
    private static void register(Scanner scanner) {
        printSection("用户注册");
        String username = readLine(scanner, "请输入新账号：");
        String password = readLine(scanner, "请输入新密码（不少于 " + Validator.MIN_PASSWORD_LENGTH + " 位）：");

        Validator.requireValidUsername(USER_STORE, username, true);
        Validator.requireValidPassword(password, true);

        User user = new User(username, password, Roles.STUDENT, TimeUtil.now());
        USER_STORE.add(user);
        FileUtil.appendUser(user);

        System.out.println("注册成功，账号 " + username + " 已创建并保存到 " + FileUtil.USER_FILE + "。");
        System.out.println("注册信息：" + user);
    }

    // ==================== 输入与展示 ====================

    /** 打印提示并读取一行，统一去掉首尾空白 */
    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("---- " + title + " ----");
    }

    private static void printBanner() {
        System.out.println("================================================");
        System.out.println("  校园资源预约系统  Campus Resource Reservation System");
        System.out.println("  版本 V1.4  自定义异常（含工程加固）");
        System.out.println("================================================");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("请选择操作：[" + CHOICE_LOGIN + "] 登录    ["
                + CHOICE_REGISTER + "] 注册    [" + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    /** 登录成功后的身份提示：角色取自 User 字段，不再靠用户名猜测 */
    private static void printWelcome(User user) {
        boolean isAdmin = Roles.ADMIN.equals(user.getRole());
        System.out.println("------------------------------------------------");
        System.out.println("登录成功！欢迎您，" + user.getUsername() + "。");
        System.out.println("当前身份：" + (isAdmin ? "ADMIN" : "STUDENT"));
        System.out.println("注册时间：" + user.getRegisterTime());
        if (isAdmin) {
            System.out.println("可用权限：新增 / 查询 / 修改 / 删除校园资源（业务菜单将在后续版本开放）");
        } else {
            System.out.println("可用权限：查询可预约资源、提交预约申请（业务菜单将在后续版本开放）");
        }
        System.out.println("用户信息：" + user);
        System.out.println("------------------------------------------------");
    }
}
