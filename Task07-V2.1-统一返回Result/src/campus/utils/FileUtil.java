package campus.utils;

import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
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
 * 数据文件工具类：用户与资源两套读写。
 *
 * 职责边界：只负责「文件与对象列表之间的转换」，不判断业务规则、不打印业务提示。
 * 异常策略：IO 失败属于系统故障，统一包装成 UncheckedIOException 抛出，由入口兜底。
 *
 * 文件格式：
 * user.txt      username|password|role|registerTime
 * resource.txt  id|name|type|status|location|description
 */
public class FileUtil {

    /** 用户数据文件名，位于程序运行目录 */
    public static final String USER_FILE = "user.txt";

    /** 资源数据文件名，位于程序运行目录 */
    public static final String RESOURCE_FILE = "resource.txt";

    /** 字段分隔符 */
    private static final String SEPARATOR = "|";

    /** 用户行字段数 */
    private static final int USER_FIELD_COUNT = 4;

    /** 资源行字段数 */
    private static final int RESOURCE_FIELD_COUNT = 6;

    private FileUtil() {
        // 工具类不允许实例化
    }

    // ==================== 用户 ====================

    /**
     * 用户文件不存在时创建空文件。
     *
     * @return true 表示本次新建了文件
     */
    public static boolean ensureUserFile() {
        return ensureFile(USER_FILE, "创建用户数据文件失败");
    }

    /** 从文件加载全部用户，坏行跳过 */
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return users;
        }
        try (BufferedReader reader = openReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = parseUserLine(line);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (IOException e) {
            throw wrap(e, "读取用户数据失败");
        }
        return users;
    }

    /** 全量覆盖写入用户 */
    public static void saveUsers(List<User> users) {
        try (BufferedWriter writer = openWriter(USER_FILE, false)) {
            for (User user : users) {
                writer.write(formatUserLine(user));
                writer.newLine();
            }
        } catch (IOException e) {
            throw wrap(e, "保存用户数据失败");
        }
    }

    /** 追加单个用户，注册成功后调用 */
    public static void appendUser(User user) {
        try (BufferedWriter writer = openWriter(USER_FILE, true)) {
            writer.write(formatUserLine(user));
            writer.newLine();
        } catch (IOException e) {
            throw wrap(e, "写入用户数据失败");
        }
    }

    private static String formatUserLine(User user) {
        return user.getUsername() + SEPARATOR + user.getPassword() + SEPARATOR
                + user.getRole() + SEPARATOR + user.getRegisterTime();
    }

    private static User parseUserLine(String line) {
        String[] parts = splitLine(line, USER_FIELD_COUNT);
        if (parts == null) {
            return null;
        }
        return new User(parts[0], parts[1], parts[2], parts[3]);
    }

    // ==================== 资源 ====================

    /** 资源文件不存在时创建空文件，返回 true 表示本次新建 */
    public static boolean ensureResourceFile() {
        return ensureFile(RESOURCE_FILE, "创建资源数据文件失败");
    }

    /** 从文件加载全部资源，状态非法或字段不足的行跳过 */
    public static List<CampusResource> loadResources() {
        List<CampusResource> resources = new ArrayList<>();
        File file = new File(RESOURCE_FILE);
        if (!file.exists()) {
            return resources;
        }
        try (BufferedReader reader = openReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                CampusResource resource = parseResourceLine(line);
                if (resource != null) {
                    resources.add(resource);
                }
            }
        } catch (IOException e) {
            throw wrap(e, "读取资源数据失败");
        }
        return resources;
    }

    /** 全量覆盖写入资源，用于初始化或批量变更后同步 */
    public static void saveResources(List<CampusResource> resources) {
        try (BufferedWriter writer = openWriter(RESOURCE_FILE, false)) {
            for (CampusResource resource : resources) {
                writer.write(formatResourceLine(resource));
                writer.newLine();
            }
        } catch (IOException e) {
            throw wrap(e, "保存资源数据失败");
        }
    }

    /** 追加单个资源，新增成功后调用 */
    public static void appendResource(CampusResource resource) {
        try (BufferedWriter writer = openWriter(RESOURCE_FILE, true)) {
            writer.write(formatResourceLine(resource));
            writer.newLine();
        } catch (IOException e) {
            throw wrap(e, "写入资源数据失败");
        }
    }

    private static String formatResourceLine(CampusResource resource) {
        return resource.getId() + SEPARATOR + resource.getName() + SEPARATOR
                + resource.getType() + SEPARATOR + resource.getStatus().name() + SEPARATOR
                + resource.getLocation() + SEPARATOR + nullToEmpty(resource.getDescription());
    }

    private static CampusResource parseResourceLine(String line) {
        String[] parts = splitLine(line, RESOURCE_FIELD_COUNT);
        if (parts == null) {
            return null;
        }
        ResourceStatus status = ResourceStatus.parse(parts[3]);
        if (status == null) {
            // 状态值不认识，说明这一行已被外部改坏，跳过比猜一个默认值安全
            return null;
        }
        return new CampusResource(parts[0], parts[1], parts[2], status, parts[4], parts[5]);
    }

    // ==================== 通用 ====================

    private static boolean ensureFile(String fileName, String errorAction) {
        File file = new File(fileName);
        if (file.exists()) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            throw wrap(e, errorAction);
        }
    }

    private static BufferedReader openReader(File file) throws IOException {
        return new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }

    private static BufferedWriter openWriter(String fileName, boolean append) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fileName, append), StandardCharsets.UTF_8));
    }

    /** 按分隔符切分并校验字段数；空行或字段不足返回 null */
    private static String[] splitLine(String line, int expectedFieldCount) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\" + SEPARATOR, -1);
        if (parts.length < expectedFieldCount) {
            return null;
        }
        return parts;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 把受检 IO 异常统一包装成非受检异常，保留原始异常便于排查 */
    private static RuntimeException wrap(IOException e, String action) {
        return new UncheckedIOException(action + "：" + e.getMessage(), e);
    }
}
