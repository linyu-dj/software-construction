package campus;

import campus.biz.ReserveManager;
import campus.biz.ResourceManager;
import campus.biz.UserManager;
import campus.common.Result;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ReserveRecord;
import campus.entity.ReserveStatus;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.store.ReserveStore;
import campus.store.ResourceStore;
import campus.store.UserStore;
import campus.utils.FileUtil;
import campus.utils.TimeUtil;
import campus.utils.Validator;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * V3.0 资源预约核心业务。
 *
 * 这一版把整个闭环接起来：
 * 学生登录 → 浏览可预约资源 → 提交预约 → 管理员审核 → 资源状态联动 → 学生可取消。
 *
 * 菜单从"两三个选项"变成"八九个选项"，主类也就跟着长了。
 * 这里刻意没有拆界面层：任务 10 才会做三层架构，现在先让职责混在一起，
 * 好在重构时能说清楚"混在哪、为什么要拆"。
 */
public class SystemMain {

    private static final UserStore USER_STORE = new UserStore();
    private static final ResourceStore RESOURCE_STORE = new ResourceStore();
    private static final ReserveStore RESERVE_STORE = new ReserveStore();

    private static final UserManager USER_MANAGER = new UserManager(USER_STORE);
    private static final ResourceManager RESOURCE_MANAGER = new ResourceManager(RESOURCE_STORE, RESERVE_STORE);
    private static final ReserveManager RESERVE_MANAGER = new ReserveManager(RESERVE_STORE, RESOURCE_STORE);

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
    private static final String ADMIN_RESERVE_LIST = "6";
    private static final String ADMIN_RESERVE_AUDIT = "7";
    private static final String ADMIN_RESERVE_SEARCH = "8";

    /** 学生菜单选项 */
    private static final String STUDENT_LIST = "1";
    private static final String STUDENT_SEARCH = "2";
    private static final String STUDENT_SUBMIT = "3";
    private static final String STUDENT_MINE = "4";
    private static final String STUDENT_CANCEL = "5";

    /** 审核子菜单 */
    private static final String AUDIT_PASS = "1";
    private static final String AUDIT_REJECT = "2";

    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_REGISTER_TIME = "2026-11-13 00:00:00";

    /** 预约表格里的时间只显示到分钟，避免一行挤满 */
    private static final DateTimeFormatter TABLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
        if (FileUtil.ensureReserveFile()) {
            System.out.println("未找到预约数据文件，已自动创建 " + FileUtil.RESERVE_FILE);
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

        RESERVE_STORE.replaceAll(FileUtil.loadReserves());
        System.out.println("已从 " + FileUtil.RESERVE_FILE + " 加载 " + RESERVE_STORE.size() + " 条预约记录。");
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
        } else if (ADMIN_RESERVE_LIST.equals(choice)) {
            showAllReserves(operator);
        } else if (ADMIN_RESERVE_AUDIT.equals(choice)) {
            auditReserve(scanner, operator);
        } else if (ADMIN_RESERVE_SEARCH.equals(choice)) {
            searchReserves(scanner, operator);
        } else {
            System.out.println("无效选项，请输入 1~8 或 0。");
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
                dispatchStudent(choice, scanner, operator);
            } catch (Exception e) {
                System.out.println("系统异常，请稍后重试。");
            }
        }
    }

    private static void dispatchStudent(String choice, Scanner scanner, User operator) {
        if (STUDENT_LIST.equals(choice)) {
            showResourceList(operator);
        } else if (STUDENT_SEARCH.equals(choice)) {
            searchResource(scanner, operator);
        } else if (STUDENT_SUBMIT.equals(choice)) {
            submitReserve(scanner, operator);
        } else if (STUDENT_MINE.equals(choice)) {
            showMyReserves(operator);
        } else if (STUDENT_CANCEL.equals(choice)) {
            cancelReserve(scanner, operator);
        } else {
            System.out.println("无效选项，请输入 1~5 或 0。");
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

    // ==================== 预约操作（学生） ====================

    private static void submitReserve(Scanner scanner, User operator) {
        printSection("提交预约申请");
        showResourceListQuietly(operator);

        String resourceId = readLine(scanner, "请输入要预约的资源编号：");
        String startText = readLine(scanner, "预约开始时间（" + TimeUtil.inputPatternHint() + "）：");
        String endText = readLine(scanner, "预约结束时间（" + TimeUtil.inputPatternHint() + "）：");
        String remark = readLine(scanner, "备注（可留空）：");

        Result<ReserveRecord> result = RESERVE_MANAGER.submit(operator, resourceId, startText, endText, remark);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        System.out.println(result.getMsg());
        printReserves(Collections.singletonList(result.getData()), "本次提交的预约");
    }

    private static void showMyReserves(User operator) {
        Result<List<ReserveRecord>> result = RESERVE_MANAGER.listMine(operator);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        printReserves(result.getData(), "我的预约");
    }

    private static void cancelReserve(Scanner scanner, User operator) {
        printSection("取消预约申请");
        String recordId = readLine(scanner, "请输入预约记录编号（如 B001）：");
        printResult(RESERVE_MANAGER.cancel(operator, recordId));
    }

    // ==================== 预约操作（管理员） ====================

    private static void showAllReserves(User operator) {
        Result<List<ReserveRecord>> result = RESERVE_MANAGER.listAll(operator);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        printReserves(result.getData(), "全部预约记录");
    }

    private static void auditReserve(Scanner scanner, User operator) {
        printSection("预约审核");
        showAllReserves(operator);

        String recordId = readLine(scanner, "请输入要审核的记录编号：");
        System.out.println("请选择审核结果：[1] 通过    [2] 驳回    [0] 放弃");
        String choice = readLine(scanner, "审核结果：");
        if (CHOICE_EXIT.equals(choice)) {
            System.out.println("已放弃本次审核。");
            return;
        }
        if (!AUDIT_PASS.equals(choice) && !AUDIT_REJECT.equals(choice)) {
            System.out.println("操作未完成：审核结果只能是 1 或 2。");
            return;
        }
        printResult(RESERVE_MANAGER.audit(operator, recordId, AUDIT_PASS.equals(choice)));
    }

    private static void searchReserves(Scanner scanner, User operator) {
        printSection("按条件查询预约");
        String resourceId = readLine(scanner, "资源编号（留空表示不限）：");
        String applicant = readLine(scanner, "申请人账号（留空表示不限）：");
        System.out.println("预约状态：[1] 待审核   [2] 已通过   [3] 已驳回   [4] 已取消   [0] 不限");
        String statusChoice = readLine(scanner, "状态序号：");
        ReserveStatus status = parseStatusChoice(statusChoice);

        Result<List<ReserveRecord>> result = RESERVE_MANAGER.search(operator, resourceId, applicant, status);
        if (!result.isSuccess()) {
            System.out.println("操作未完成：" + result.getMsg());
            return;
        }
        printReserves(result.getData(), "符合条件的预约");
    }

    private static ReserveStatus parseStatusChoice(String choice) {
        if ("1".equals(choice)) {
            return ReserveStatus.PENDING;
        }
        if ("2".equals(choice)) {
            return ReserveStatus.PASS;
        }
        if ("3".equals(choice)) {
            return ReserveStatus.REJECT;
        }
        if ("4".equals(choice)) {
            return ReserveStatus.CANCEL;
        }
        return null;
    }

    // ==================== 展示 ====================

    /** 提交预约前先展示可选资源 */
    private static void showResourceListQuietly(User operator) {
        Result<List<CampusResource>> result = RESOURCE_MANAGER.listFor(operator);
        if (result.isSuccess()) {
            printResources(result.getData(), "可预约资源");
        }
    }

    private static void printResources(List<CampusResource> resources, String title) {
        System.out.println();
        System.out.println("---- " + title + "（共 " + resources.size() + " 条）----");
        if (resources.isEmpty()) {
            System.out.println("暂无可展示的资源。");
            return;
        }
        System.out.println(String.format("%-6s %-14s %-10s %-8s %s",
                "编号", "名称", "类型", "状态", "位置"));
        resources.forEach(resource -> System.out.println(String.format("%-6s %-14s %-10s %-8s %s",
                resource.getId(), resource.getName(), resource.getType(),
                resource.getStatus().getLabel(), resource.getLocation())));
    }

    private static void printReserves(List<ReserveRecord> records, String title) {
        System.out.println();
        System.out.println("---- " + title + "（共 " + records.size() + " 条）----");
        if (records.isEmpty()) {
            System.out.println("暂无预约记录。");
            return;
        }
        System.out.println(String.format("%-6s %-6s %-10s %-17s %-17s %-8s %s",
                "记录号", "资源", "申请人", "开始时间", "结束时间", "状态", "备注"));
        records.forEach(record -> System.out.println(String.format("%-6s %-6s %-10s %-17s %-17s %-8s %s",
                record.getRecordId(), record.getResourceId(), record.getApplicantUsername(),
                record.getReserveStartTime().format(TABLE_TIME),
                record.getReserveEndTime().format(TABLE_TIME),
                record.getStatus().getLabel(),
                record.getRemark() == null || record.getRemark().isEmpty() ? "-" : record.getRemark())));
    }

    /** Result 的统一打印口：成功打 msg，失败加前缀 */
    private static void printResult(Result<?> result) {
        if (result.isSuccess()) {
            System.out.println(result.getMsg());
        } else {
            System.out.println("操作未完成：" + result.getMsg());
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
        System.out.println("  版本 V3.0  资源预约核心业务");
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
        System.out.println("[" + ADMIN_UPDATE + "] 修改资源    [" + ADMIN_DELETE + "] 删除资源");
        System.out.println("[" + ADMIN_RESERVE_LIST + "] 查询全部预约    [" + ADMIN_RESERVE_AUDIT + "] 审核预约    ["
                + ADMIN_RESERVE_SEARCH + "] 条件查询预约");
        System.out.println("[" + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    private static void printStudentMenu() {
        System.out.println();
        System.out.println("---- 学生菜单 ----");
        System.out.println("[" + STUDENT_LIST + "] 查询可预约资源    [" + STUDENT_SEARCH + "] 按名称查询");
        System.out.println("[" + STUDENT_SUBMIT + "] 提交预约    [" + STUDENT_MINE + "] 我的预约    ["
                + STUDENT_CANCEL + "] 取消预约");
        System.out.println("[" + CHOICE_EXIT + "] 退出");
        System.out.print("请输入选项：");
    }

    private static void printWelcome(User user) {
        boolean isAdmin = Roles.ADMIN.equals(user.getRole());
        System.out.println("------------------------------------------------");
        System.out.println("登录成功！欢迎您，" + user.getUsername() + "。");
        System.out.println("当前身份：" + (isAdmin ? "ADMIN" : "STUDENT"));
        System.out.println("注册时间：" + user.getRegisterTime());
        if (isAdmin) {
            System.out.println("可用权限：资源维护、预约审核与条件查询");
        } else {
            System.out.println("可用权限：浏览资源、提交与取消自己的预约");
        }
        System.out.println("------------------------------------------------");
    }
}
