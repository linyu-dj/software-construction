package campus.test;

/**
 * 极简断言工具。
 *
 * 不引 JUnit 的原因：课程要求只用 JDK 自带能力，jar 依赖不进仓库。
 * 断言失败直接抛 AssertionError，由 TestRunner 捕获并记一条失败用例。
 */
public final class Assert {

    private Assert() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void isFalse(boolean condition, String message) {
        isTrue(!condition, message);
    }

    public static void notNull(Object value, String message) {
        isTrue(value != null, message);
    }

    /** 期望 text 中包含 fragment，用于校验中文提示 */
    public static void contains(String fragment, String text, String message) {
        isTrue(text != null && text.contains(fragment), message + "（实际：" + text + "）");
    }

    /** 期望两个值相等，字符串比较按内容 */
    public static void equals(Object expected, Object actual, String message) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        isTrue(same, message + "（期望：" + expected + "，实际：" + actual + "）");
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
