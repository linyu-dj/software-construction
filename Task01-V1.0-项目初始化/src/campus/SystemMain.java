package campus;

import java.util.Scanner;

/**
 * V1.0 项目初始化 —— 程序入口与身份登录。
 *
 * 版本约束（按任务书要求）：
 * 本阶段全部逻辑集中在主类中，不做分层、不抽工具类，刻意保留耦合，
 * 目的是为 V1.1 起的迭代（注册功能、User 实体化、IO 持久化、自定义异常、三层架构）
 * 留出明确的改造空间，并通过博客记录每一次重构的动因。
 *
 * 验收标准：支持管理员登录、学生登录、登录失败三种结果。
 */
public class SystemMain {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // V1.0 固定测试账号：直接写死在主方法里，
        // 缺点是不改代码就加不了新用户，V1.1 会用动态注册 + 集合替换掉这段。
        String adminUsername = "admin";
        String adminPassword = "123456";
        String studentUsername = "student";
        String studentPassword = "123456";

        System.out.println("================================================");
        System.out.println("  校园资源预约系统  Campus Resource Reservation System");
        System.out.println("  版本 V1.0  项目初始化");
        System.out.println("================================================");

        boolean loggedIn = false;

        while (!loggedIn) {
            System.out.println();
            System.out.println("请选择操作：[1] 登录    [0] 退出");
            System.out.print("请输入选项：");
            String choice = scanner.nextLine().trim();

            if ("0".equals(choice)) {
                System.out.println("已退出系统，再见。");
                break;
            }

            if (!"1".equals(choice)) {
                System.out.println("无效选项，请输入 1 或 0。");
                continue;
            }

            System.out.print("账号：");
            String username = scanner.nextLine().trim();
            System.out.print("密码：");
            String password = scanner.nextLine().trim();

            if (username.isEmpty() || password.isEmpty()) {
                System.out.println("登录失败：账号和密码都不能为空。");
                continue;
            }

            // 账号密码校验与身份判定写在一起：V1.1 会拆成独立的校验方法，
            // V1.4 会改为抛出自定义异常，这里先保留最原始的写法。
            if (username.equals(adminUsername) && password.equals(adminPassword)) {
                System.out.println("------------------------------------------------");
                System.out.println("登录成功！欢迎您，管理员 " + username + "。");
                System.out.println("当前身份：ADMIN");
                System.out.println("可用权限：新增 / 查询 / 修改 / 删除校园资源（业务菜单将在后续版本开放）");
                System.out.println("------------------------------------------------");
                loggedIn = true;
            } else if (username.equals(studentUsername) && password.equals(studentPassword)) {
                System.out.println("------------------------------------------------");
                System.out.println("登录成功！欢迎您，学生 " + username + "。");
                System.out.println("当前身份：STUDENT");
                System.out.println("可用权限：查询可预约资源、提交预约申请（业务菜单将在后续版本开放）");
                System.out.println("------------------------------------------------");
                loggedIn = true;
            } else {
                System.out.println("登录失败：账号或密码错误，请重新输入。");
            }
        }

        scanner.close();
    }
}
