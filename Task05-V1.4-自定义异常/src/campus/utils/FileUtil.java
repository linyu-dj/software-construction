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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据文件工具类。
 *
 * 职责边界：只负责「文件与对象列表之间的转换」，不判断账号是否合法、不打印业务提示。
 * 业务代码通过本类读写数据，不直接接触 IO 流，换存储介质时只改本类。
 *
 * 文件格式：一行一个用户，字段用竖线分隔
 * username|password|role|registerTime
 *
 * 异常策略：IO 失败属于系统故障，不是业务规则失败，不能用"返回空集合"糊过去——
 * 那会让程序带着空数据继续跑，问题被推迟到很久以后才暴露。这里统一包装成
 * UncheckedIOException 抛出，由入口决定如何提示用户。
 */
public class FileUtil {

    /** 用户数据文件名，位于程序运行目录 */
    public static final String USER_FILE = "user.txt";

    /** 字段分隔符 */
    private static final String SEPARATOR = "|";

    /** 一个用户行的字段数 */
    private static final int FIELD_COUNT = 4;

    private FileUtil() {
        // 工具类不允许实例化
    }

    /**
     * 文件不存在时创建空文件。
     *
     * @return true 表示本次新建了文件（调用方据此决定是否提示）
     */
    public static boolean ensureFile() {
        File file = new File(USER_FILE);
        if (file.exists()) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            throw wrap(e, "创建用户数据文件失败");
        }
    }

    /**
     * 从文件加载全部用户。
     * 坏行（字段不足、空行）会被跳过，一条脏数据不该拖垮整个启动过程。
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
            throw wrap(e, "读取用户数据失败");
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
            throw wrap(e, "保存用户数据失败");
        }
    }

    /** 追加单个用户，注册成功后调用，避免为了一个新人重写整个文件 */
    public static void appendUser(User user) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(USER_FILE, true), StandardCharsets.UTF_8))) {
            writer.write(formatLine(user));
            writer.newLine();
        } catch (IOException e) {
            throw wrap(e, "写入用户数据失败");
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
        String[] parts = trimmed.split("\\" + SEPARATOR, -1);
        if (parts.length < FIELD_COUNT) {
            return null;
        }
        return new User(parts[0], parts[1], parts[2], parts[3]);
    }

    /** 把受检 IO 异常统一包装成非受检异常，保留原始异常作为 cause 便于排查 */
    private static RuntimeException wrap(IOException e, String action) {
        return new UncheckedIOException(action + "：" + e.getMessage(), e);
    }
}
