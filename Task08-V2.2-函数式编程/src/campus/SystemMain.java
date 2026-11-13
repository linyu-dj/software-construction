package campus;

import campus.biz.ResourceManager;
import campus.biz.UserManager;
import campus.common.Result;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.store.ResourceStore;
import campus.store.UserStore;
import campus.utils.FileUtil;
import campus.utils.Validator;

import java.util.List;
import java.util.Scanner;

/**
 * V2.1 统一返回 Result —— 所有业务方法的返回值标准化。
 *
 * 本版本改动：
 * 1. 新增泛型 Result<T>，提供 success / fail 静态工厂
 * 2. 用户业务从主类搬进 UserManager，注册、登录、查询统一返回 Result
 * 3. ResourceManager 五个方法全部改为返回 Result
 * 4. 控制台只判断 isSuccess 并打印 msg，不再认识具体异常类型
 *
 * 行为与 V2.0 完全一致：同样的输入，同样的提示，同样的落盘结果。
 * 本阶段仍不做分层，Manager 继续留在 biz 包，为任务 10 重构保留素材。
 */
public class SystemMain {

    private static final UserStore USER_STORE = new UserStore();
    private static final ResourceStore RESOURCE_STORE = new ResourceStore();
    private static final UserManager USER_MANAGER = new UserManager(USER_STORE);
    private static final ResourceManager RESOURCE_MANAGER = new ResourceManager(RESOURCE_STORE);

    /** 游客菜单选项 */
    private static final String CHOICE_EXIT = "0";
    private static final String CHOICE_LOGIN = "1";
    private static final String CHOICE_REGISTER = "2";

    /** 管理员菜单选项 */
    private static final String ADMIN_CREATE = "1";
    private static final String ADMIN_LIST = "2";
    private static final String ADMIN_SEARCH = "3";
    private static final String ADMIN_UPDATE = "4";
    private static final String ADMIN_DELETE = "5";

    /** 学生菜单选项 */
    private static final String STUDENT_LIST = "1";
    private static final String STUDENT_SEARCH = "2";

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
                    if (user == null) {
                        continue;
                    }
                    runSession(scanner, user);
                    return;
                }
                if (CHOICE_REGISTER.equals(choice)) {
                    register(scanner);
                } else {
                    System.out.println("无效选项，请输入 " + CHOICE_EXIT + "、" + CHOICE_LOGIN
                            + " 或 " + CHOICE_REGISTER + "。");
                }
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    /** 登录：业务结果全部在 Result 里，成功才拿到 User */
    private static User login(Scanner scanner) {
        printSection("用户登录");
        String username = readLine(scanner, "账号：");
        String password = readLine(scanner, "密码：");

        Result<User> result = USER_MANAGER.login(username, password);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return null;
        }
        printWelcome(result.getData());
        return result.getData();
    }

    private static void register(Scanner scanner) {
        printSection("用户注册");
        String username = readLine(scanner, "请输入新账号：");
        String password = readLine(scanner, "请输入新密码（不少于 " + Validator.MIN_PASSWORD_LENGTH + " 位）：");

        Result<User> result = USER_MANAGER.register(username, password);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        System.out.println(result.getMsg());
        System.out.println("注册信息：" + result.getData());
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
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    private static void dispatchAdmin(String choice, Scanner scanner, User operator) {
        if (ADMIN_CREATE.equals(choice)) {
            createResource(scanner, operator);
        } else if (ADMIN_LIST.equals(choice)) {
            showResourceList(operator);
        } else if (ADMIN_SEARCH.equals(choice)) {
            searchResource(scanner, operator);
        } else if (ADMIN_UPDATE.equals(choice)) {
            updateResource(scanner, operator);
        } else if (ADMIN_DELETE.equals(choice)) {
            deleteResource(scanner, operator);
        } else {
            System.out.println("无效选项，请输入 1~5 或 0。");
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
                    showResourceList(operator);
                } else if (STUDENT_SEARCH.equals(choice)) {
                    searchResource(scanner, operator);
                } else {
                    System.out.println("无效选项，请输入 1、2 或 0。");
                }
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

        Result<CampusResource> result = RESOURCE_MANAGER.create(operator, name, type, location, description);
        printResult(result);
    }

    private static void updateResource(Scanner scanner, User operator) {
        printSection("修改校园资源");
        String id = readLine(scanner, "资源编号：");
        String name = readLine(scanner, "新名称：");
        String type = readLine(scanner, "新类型：");
        String location = readLine(scanner, "新位置：");
        ResourceStatus status = readStatus(scanner);
        if (status == null) {
            return;
        }

        printResult(RESOURCE_MANAGER.update(operator, id, name, type, location, status));
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
        System.out.println("操作未完成：状态序号只能是 1、2 或 3。");
        return null;
    }

    private static void deleteResource(Scanner scanner, User operator) {
        printSection("删除校园资源");
        String id = readLine(scanner, "资源编号：");
        printResult(RESOURCE_MANAGER.delete(operator, id));
    }

    private static void showResourceList(User operator) {
        Result<List<CampusResource>> result = RESOURCE_MANAGER.listFor(operator);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        String title = Roles.ADMIN.equals(operator.getRole()) ? "全部资源" : "可预约资源";
        printResources(result.getData(), title);
    }

    /** 名称模糊查询：管理员搜全部资源，学生只搜可预约资源 */
    private static void searchResource(Scanner scanner, User operator) {
        printSection("按名称查询资源");
        String keyword = readLine(scanner, "请输入名称关键词（留空表示全部）：");

        Result<List<CampusResource>> result = RESOURCE_MANAGER.searchByName(operator, keyword);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        printResources(result.getData(), "匹配「" + keyword + "」的资源");
    }

    /** Result 的统一打印口：成功打 msg，失败加前缀 */
    private static void printResult(Result<?> result) {
        if (result.isSuccess()) {
            System.out.println(result.getMsg());
        } else {
            System.out.println("操作未完成：" + result.getMsg());
        }
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
        // 旧写法是 for 循环逐条 println，这里换成 forEach，纯粹为了让遍历风格统一
        resources.forEach(resource -> System.out.println(String.format("%-6s %-14s %-10s %-8s %s",
                resource.getId(), resource.getName(), resource.getType(),
                resource.getStatus().getLabel(), resource.getLocation())));
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
        System.out.println("  版本 V2.2  Stream + Optional");
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
                + ADMIN_SEARCH + "] 按名称查询");
        System.out.println("[" + ADMIN_UPDATE + "] 修改资源    [" + ADMIN_DELETE + "] 删除资源    ["
                + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    private static void printStudentMenu() {
        System.out.println();
        System.out.println("---- 学生菜单 ----");
        System.out.println("[" + STUDENT_LIST + "] 查询可预约资源    [" + STUDENT_SEARCH + "] 按名称查询    ["
                + CHOICE_EXIT + "] 退出");
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
