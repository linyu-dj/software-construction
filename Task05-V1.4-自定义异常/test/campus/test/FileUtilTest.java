package campus.test;

import campus.common.Roles;
import campus.entity.User;
import campus.utils.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 持久化测试（V1.4）。
 *
 * 与 V1.3 相比，这一轮的差别在异常策略：IO 故障不再打印后吞掉，
 * 而是包装成 UncheckedIOException 抛出。测试因此除了验证行格式往返，
 * 还验证「加载遇到脏行仍能继续」这条防御式约定。
 */
public class FileUtilTest {

    private static void deleteUserFile() {
        File file = new File(FileUtil.USER_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    private static void writeRawFile(String content) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(FileUtil.USER_FILE), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    public static void runAll(TestRunner runner) {
        runner.run("保存后加载字段一致", () -> {
            try {
                deleteUserFile();
                List<User> users = new ArrayList<>();
                users.add(new User("alice", "123456", Roles.STUDENT, "2026-11-12 10:00:00"));
                users.add(new User("admin", "admin888", Roles.ADMIN, "2026-11-12 10:05:00"));
                FileUtil.saveUsers(users);

                List<User> loaded = FileUtil.loadUsers();
                Assert.equals(2, loaded.size(), "应读回两条记录");
                Assert.equals("alice", loaded.get(0).getUsername(), "账号应一致");
                Assert.equals(Roles.ADMIN, loaded.get(1).getRole(), "角色应一致");
                Assert.equals("2026-11-12 10:05:00", loaded.get(1).getRegisterTime(), "时间应一致");
            } catch (Exception e) {
                Assert.fail("读写抛出异常：" + e.getMessage());
            }
        });

        runner.run("追加写入保留已有记录", () -> {
            try {
                deleteUserFile();
                FileUtil.appendUser(new User("bob", "abcdef", Roles.STUDENT, "2026-11-12 11:00:00"));
                FileUtil.appendUser(new User("carol", "abcdef", Roles.STUDENT, "2026-11-12 11:10:00"));

                List<User> loaded = FileUtil.loadUsers();
                Assert.equals(2, loaded.size(), "追加两次应得到两条记录");
                Assert.equals("bob", loaded.get(0).getUsername(), "顺序应与写入一致");
            } catch (Exception e) {
                Assert.fail("追加抛出异常：" + e.getMessage());
            }
        });

        runner.run("脏行与空行被跳过", () -> {
            try {
                deleteUserFile();
                writeRawFile("broken|line\r\n\r\nalice|123456|student|2026-11-12 10:00:00\r\n");
                List<User> loaded = FileUtil.loadUsers();
                Assert.equals(1, loaded.size(), "只保留一条有效记录");
            } catch (Exception e) {
                Assert.fail("读取抛出异常：" + e.getMessage());
            }
        });

        runner.run("文件缺失时返回空列表", () -> {
            deleteUserFile();
            Assert.equals(0, FileUtil.loadUsers().size(), "无文件时返回空集合");
        });

        runner.run("ensureFile 首次创建、再次返回 false", () -> {
            deleteUserFile();
            Assert.isTrue(FileUtil.ensureFile(), "首次调用应创建文件并返回 true");
            Assert.isFalse(FileUtil.ensureFile(), "文件已存在时应返回 false");
            Assert.isTrue(new File(FileUtil.USER_FILE).exists(), "文件应真实存在");
        });
    }
}
