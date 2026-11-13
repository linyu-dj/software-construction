package campus.utils;

import campus.entity.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * V1.3 文件操作工具类。
 *
 * 职责边界：只负责「文件与对象列表之间的转换」，不判断账号是否合法、
 * 不打印业务提示。业务代码通过本类读写用户数据，不直接接触 IO 流，
 * 这样换存储介质时只需要改本类。
 *
 * 文件格式：一行一个用户，字段用竖线分隔
 * username|password|role|registerTime
 */
public class FileUtil {

    /** 用户数据文件名，位于程序运行目录 */
    public static final String USER_FILE = "user.txt";

    /** 字段分隔符 */
    private static final String SEPARATOR = "|";

    private FileUtil() {
        // 工具类不允许实例化
    }

    /** 文件不存在时自动创建空文件，保证后续读写不会因路径缺失失败 */
    public static void ensureFile() {
        File file = new File(USER_FILE);
        if (file.exists()) {
            return;
        }
        try {
            if (file.createNewFile()) {
                System.out.println("未找到用户数据文件，已自动创建 " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("创建用户数据文件失败：" + e.getMessage());
        }
    }

    /**
     * 从文件加载全部用户。
     * 坏行（字段不足）会被跳过，不让一条脏数据拖垮整个启动过程。
     */
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = parseLine(line);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (IOException e) {
            System.out.println("读取用户数据失败：" + e.getMessage());
        }
        return users;
    }

    /** 全量覆盖写入，用于初始化或批量变更后同步 */
    public static void saveUsers(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(USER_FILE), StandardCharsets.UTF_8))) {
            for (User user : users) {
                writer.write(formatLine(user));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存用户数据失败：" + e.getMessage());
        }
    }

    /** 追加单个用户，注册成功后调用，避免为了一个新人重写整个文件 */
    public static void appendUser(User user) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(USER_FILE, true), StandardCharsets.UTF_8))) {
            writer.write(formatLine(user));
            writer.newLine();
        } catch (IOException e) {
            System.out.println("写入用户数据失败：" + e.getMessage());
        }
    }

    // ==================== 行格式转换 ====================

    private static String formatLine(User user) {
        return user.getUsername() + SEPARATOR + user.getPassword() + SEPARATOR
                + user.getRole() + SEPARATOR + user.getRegisterTime();
    }

    private static User parseLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\|", -1);
        if (parts.length < 4) {
            return null;
        }
        return new User(parts[0], parts[1], parts[2], parts[3]);
    }
}
