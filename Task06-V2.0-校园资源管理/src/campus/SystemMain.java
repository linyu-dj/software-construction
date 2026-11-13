package campus;

import campus.biz.ResourceManager;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.UserNotFoundException;
import campus.exception.WrongPasswordException;
import campus.store.ResourceStore;
import campus.store.UserStore;
import campus.utils.FileUtil;
import campus.utils.TimeUtil;
import campus.utils.Validator;

import java.util.List;
import java.util.Scanner;

/**
 * V2.0 校园资源管理 —— 在 V1.4 的用户体系上长出第一个真正的业务对象。
 *
 * 本版本新增：
 * 1. CampusResource 实体与 ResourceStatus 三态枚举
 * 2. ResourceStore 持有资源集合，并按 R001 规则生成唯一编号
 * 3. ResourceManager 承担资源业务：权限校验、增删改查、落盘
 * 4. 登录后按角色分菜单：管理员可增删改查，学生只能查可预约资源
 * 5. 新增 PermissionDeniedException，越权操作由父类统一兜底
 *
 * 版本约束：本阶段仍不分层，Manager 暂放 biz 包，为任务 10 的三层重构留素材。
 */
public class SystemMain {

    private static final UserStore USER_STORE = new UserStore();
    private static final ResourceStore RESOURCE_STORE = new ResourceStore();
    private static final ResourceManager RESOURCE_MANAGER = new ResourceManager(RESOURCE_STORE);

    /** 游客菜单选项 */
    private static final String CHOICE_EXIT = "0";
    private static final String CHOICE_LOGIN = "1";
    private static final String CHOICE_REGISTER = "2";

    /** 管理员菜单选项 */
    private static final String ADMIN_CREATE = "1";
    private static final String ADMIN_LIST = "2";
    private static final String ADMIN_UPDATE = "3";
    private static final String ADMIN_DELETE = "4";

    /** 学生菜单选项 */
    private static final String STUDENT_LIST = "1";

    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_REGISTER_TIME = "2026-11-13 00:00:00";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            try {
                initStorage();
                printBanner();
                runGuestLoop(scanner);
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    // ==================== 初始化 ====================

    private static void initStorage() {
        if (FileUtil.ensureUserFile()) {
            System.out.println("未找到用户数据文件，已自动创建 " + FileUtil.USER_FILE);
        }
        if (FileUtil.ensureResourceFile()) {
            System.out.println("未找到资源数据文件，已自动创建 " + FileUtil.RESOURCE_FILE);
        }

        USER_STORE.replaceAll(FileUtil.loadUsers());
        if (USER_STORE.isEmpty()) {
            seedDefaultUsers();
            FileUtil.saveUsers(USER_STORE.snapshot());
            System.out.println("用户数据文件为空，已写入 " + USER_STORE.size() + " 个初始化账号。");
        } else {
            System.out.println("已从 " + FileUtil.USER_FILE + " 加载 " + USER_STORE.size() + " 个用户。");
        }

        RESOURCE_STORE.replaceAll(FileUtil.loadResources());
        if (RESOURCE_STORE.isEmpty()) {
            seedDefaultResources();
            FileUtil.saveResources(RESOURCE_STORE.snapshot());
            System.out.println("资源数据文件为空，已写入 " + RESOURCE_STORE.size() + " 个示例资源。");
        } else {
            System.out.println("已从 " + FileUtil.RESOURCE_FILE + " 加载 " + RESOURCE_STORE.size() + " 个资源。");
        }
    }

    private static void seedDefaultUsers() {
        USER_STORE.add(new User(Roles.ADMIN, DEFAULT_PASSWORD, Roles.ADMIN, DEFAULT_REGISTER_TIME));
        USER_STORE.add(new User(Roles.STUDENT, DEFAULT_PASSWORD, Roles.STUDENT, DEFAULT_REGISTER_TIME));
    }

    private static void seedDefaultResources() {
        RESOURCE_STORE.add(new CampusResource("R001", "一号会议室", "教室",
                ResourceStatus.AVAILABLE, "教学楼 3 层", "可容纳 30 人"));
        RESOURCE_STORE.add(new CampusResource("R002", "302 实验室", "实验室",
                ResourceStatus.AVAILABLE, "实验楼 3 层", "需提前预约"));
        RESOURCE_STORE.add(new CampusResource("R003", "投影仪 A", "设备",
                ResourceStatus.AVAILABLE, "器材室", "借用需登记"));
    }

    // ==================== 游客环节 ====================

    private static void runGuestLoop(Scanner scanner) {
        while (true) {
            printGuestMenu();
            String choice = scanner.nextLine().trim();

            if (CHOICE_EXIT.equals(choice)) {
                System.out.println("已退出系统，再见。");
                return;
            }
            try {
                if (CHOICE_LOGIN.equals(choice)) {
                    User user = login(scanner);
                    runSession(scanner, user);
                    return;
                }
                if (CHOICE_REGISTER.equals(choice)) {
                    register(scanner);
                } else {
                    System.out.println("无效选项，请输入 " + CHOICE_EXIT + "、" + CHOICE_LOGIN
                            + " 或 " + CHOICE_REGISTER + "。");
                }
            } catch (BusinessException e) {
                System.out.println("操作未完成：" + e.getMessage());
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    private static User login(Scanner scanner) {
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
        return user;
    }

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

    // ==================== 登录后的角色菜单 ====================

    private static void runSession(Scanner scanner, User user) {
        if (Roles.ADMIN.equals(user.getRole())) {
            runAdminMenu(scanner, user);
        } else {
            runStudentMenu(scanner, user);
        }
    }

    private static void runAdminMenu(Scanner scanner, User operator) {
        while (true) {
            printAdminMenu();
            String choice = scanner.nextLine().trim();
            if (CHOICE_EXIT.equals(choice)) {
                System.out.println("已退出系统，再见。");
                return;
            }
            try {
                dispatchAdmin(choice, scanner, operator);
            } catch (BusinessException e) {
                System.out.println("操作未完成：" + e.getMessage());
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    private static void dispatchAdmin(String choice, Scanner scanner, User operator) {
        if (ADMIN_CREATE.equals(choice)) {
            createResource(scanner, operator);
        } else if (ADMIN_LIST.equals(choice)) {
            printResources(RESOURCE_MANAGER.listFor(operator), "全部资源");
        } else if (ADMIN_UPDATE.equals(choice)) {
            updateResource(scanner, operator);
        } else if (ADMIN_DELETE.equals(choice)) {
            deleteResource(scanner, operator);
        } else {
            System.out.println("无效选项，请输入 1~4 或 0。");
        }
    }

    private static void runStudentMenu(Scanner scanner, User operator) {
        while (true) {
            printStudentMenu();
            String choice = scanner.nextLine().trim();
            if (CHOICE_EXIT.equals(choice)) {
                System.out.println("已退出系统，再见。");
                return;
            }
            try {
                if (STUDENT_LIST.equals(choice)) {
                    printResources(RESOURCE_MANAGER.listFor(operator), "可预约资源");
                } else {
                    System.out.println("无效选项，请输入 1 或 0。");
                }
            } catch (BusinessException e) {
                System.out.println("操作未完成：" + e.getMessage());
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    // ==================== 资源操作 ====================

    private static void createResource(Scanner scanner, User operator) {
        printSection("新增校园资源");
        String name = readLine(scanner, "资源名称：");
        String type = readLine(scanner, "资源类型（教室/实验室/设备/场地）：");
        String location = readLine(scanner, "资源位置：");
        String description = readLine(scanner, "资源描述（可留空）：");

        CampusResource created = RESOURCE_MANAGER.create(operator, name, type, location, description);
        System.out.println("新增成功：" + created.getId() + " " + created.getName()
                + "，默认状态 " + created.getStatus().getLabel() + "。");
    }

    private static void updateResource(Scanner scanner, User operator) {
        printSection("修改校园资源");
        String id = readLine(scanner, "资源编号：");
        String name = readLine(scanner, "新名称：");
        String type = readLine(scanner, "新类型：");
        String location = readLine(scanner, "新位置：");
        ResourceStatus status = readStatus(scanner);

        RESOURCE_MANAGER.update(operator, id, name, type, location, status);
        System.out.println("修改成功：" + id + " 已更新为 " + status.getLabel() + "。");
    }

    private static ResourceStatus readStatus(Scanner scanner) {
        System.out.println("选择状态：[1] 可预约   [2] 占用   [3] 停用");
        String choice = readLine(scanner, "状态序号：");
        if ("1".equals(choice)) {
            return ResourceStatus.AVAILABLE;
        }
        if ("2".equals(choice)) {
            return ResourceStatus.OCCUPIED;
        }
        if ("3".equals(choice)) {
            return ResourceStatus.DISABLED;
        }
        throw new EmptyParamException("状态序号只能是 1、2 或 3。");
    }

    private static void deleteResource(Scanner scanner, User operator) {
        printSection("删除校园资源");
        String id = readLine(scanner, "资源编号：");
        RESOURCE_MANAGER.delete(operator, id);
        System.out.println("删除成功：" + id + " 已从资源列表中移除。");
    }

    // ==================== 展示 ====================

    private static void printResources(List<CampusResource> resources, String title) {
        System.out.println();
        System.out.println("---- " + title + "（共 " + resources.size() + " 条）----");
        if (resources.isEmpty()) {
            System.out.println("暂无可展示的资源。");
            return;
        }
        System.out.println(String.format("%-6s %-14s %-10s %-8s %s",
                "编号", "名称", "类型", "状态", "位置"));
        for (CampusResource resource : resources) {
            System.out.println(String.format("%-6s %-14s %-10s %-8s %s",
                    resource.getId(), resource.getName(), resource.getType(),
                    resource.getStatus().getLabel(), resource.getLocation()));
        }
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("---- " + title + " ----");
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void printBanner() {
        System.out.println("================================================");
        System.out.println("  校园资源预约系统  Campus Resource Reservation System");
        System.out.println("  版本 V2.0  校园资源管理");
        System.out.println("================================================");
    }

    private static void printGuestMenu() {
        System.out.println();
        System.out.println("请选择操作：[" + CHOICE_LOGIN + "] 登录    ["
                + CHOICE_REGISTER + "] 注册    [" + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    private static void printAdminMenu() {
        System.out.println();
        System.out.println("---- 管理员菜单 ----");
        System.out.println("[" + ADMIN_CREATE + "] 新增资源    [" + ADMIN_LIST + "] 查询全部资源    ["
                + ADMIN_UPDATE + "] 修改资源    [" + ADMIN_DELETE + "] 删除资源    [" + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    private static void printStudentMenu() {
        System.out.println();
        System.out.println("---- 学生菜单 ----");
        System.out.println("[" + STUDENT_LIST + "] 查询可预约资源    [" + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    private static void printWelcome(User user) {
        boolean isAdmin = Roles.ADMIN.equals(user.getRole());
        System.out.println("------------------------------------------------");
        System.out.println("登录成功！欢迎您，" + user.getUsername() + "。");
        System.out.println("当前身份：" + (isAdmin ? "ADMIN" : "STUDENT"));
        System.out.println("注册时间：" + user.getRegisterTime());
        if (isAdmin) {
            System.out.println("可用权限：新增 / 查询 / 修改 / 删除校园资源");
        } else {
            System.out.println("可用权限：查询可预约资源");
        }
        System.out.println("------------------------------------------------");
    }
}
