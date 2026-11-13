package campus.test;

import campus.entity.User;
import campus.utils.FileUtil;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 持久化单元测试（V1.3）。
 *
 * 测的是「对象列表 ↔ 文本行」这一层等价关系：写进去的字段，读回来必须一字不差；
 * 脏行不能让整次加载失败，只能被跳过。
 *
 * 用例都在 testtmp 目录里跑（由 test.bat 切换工作目录），
 * 目的是让测试产生的 user.txt 与演示数据隔离——测试代码不应该修改被测系统的运行数据。
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
        runner.run("保存后加载，字段逐项一致", () -> {
            try {
                deleteUserFile();
                List<User> users = new ArrayList<>();
                users.add(new User("alice", "123456", "student", "2026-11-12 10:00:00"));
                users.add(new User("admin", "admin888", "admin", "2026-11-12 10:05:00"));
                FileUtil.saveUsers(users);

                List<User> loaded = FileUtil.loadUsers();
                Assert.equals(2, loaded.size(), "应读回两条记录");
                User first = loaded.get(0);
                Assert.equals("alice", first.getUsername(), "第一条账号应一致");
                Assert.equals("student", first.getRole(), "第一条角色应一致");
                Assert.equals("2026-11-12 10:00:00", first.getRegisterTime(), "第一条时间应一致");
                Assert.equals("admin", loaded.get(1).getUsername(), "第二条账号应一致");
            } catch (Exception e) {
                Assert.fail("读写过程抛出异常：" + e.getMessage());
            }
        });

        runner.run("追加写入不覆盖已有记录", () -> {
            try {
                deleteUserFile();
                FileUtil.appendUser(new User("bob", "abcdef", "student", "2026-11-12 11:00:00"));
                FileUtil.appendUser(new User("carol", "abcdef", "student", "2026-11-12 11:10:00"));

                List<User> loaded = FileUtil.loadUsers();
                Assert.equals(2, loaded.size(), "两次追加应得到两条记录");
                Assert.equals("bob", loaded.get(0).getUsername(), "先写入的在前");
                Assert.equals("carol", loaded.get(1).getUsername(), "后写入的在后");
            } catch (Exception e) {
                Assert.fail("追加过程抛出异常：" + e.getMessage());
            }
        });

        runner.run("字段不足的脏行被跳过", () -> {
            try {
                deleteUserFile();
                writeRawFile("broken|line\r\nalice|123456|student|2026-11-12 10:00:00\r\n");
                List<User> loaded = FileUtil.loadUsers();
                Assert.equals(1, loaded.size(), "脏行应被跳过，只留下一条有效记录");
                Assert.equals("alice", loaded.get(0).getUsername(), "有效记录应保留");
            } catch (Exception e) {
                Assert.fail("读取脏行时抛出未预期异常：" + e.getMessage());
            }
        });

        runner.run("文件不存在时加载返回空列表", () -> {
            deleteUserFile();
            List<User> loaded = FileUtil.loadUsers();
            Assert.equals(0, loaded.size(), "无文件时应返回空集合而不是 null");
        });

        runner.run("ensureFile 在缺失时创建数据文件", () -> {
            deleteUserFile();
            FileUtil.ensureFile();
            File file = new File(FileUtil.USER_FILE);
            Assert.isTrue(file.exists(), "调用后文件应存在");
        });
    }
}
