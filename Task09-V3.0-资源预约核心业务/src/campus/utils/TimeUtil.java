package campus.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 时间工具类。
 *
 * 全项目只在这里定义时间格式：
 * 存储与展示统一用 yyyy-MM-dd HH:mm:ss，用户输入允许省略秒（yyyy-MM-dd HH:mm）。
 * 这样界面、文件、日志三处的时间字符串一定长得一样。
 */
public final class TimeUtil {

    /** 存储与展示统一格式 */
    private static final DateTimeFormatter STORAGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 用户输入允许省略秒 */
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeUtil() {
        // 工具类不允许实例化
    }

    /** 当前时间，格式 yyyy-MM-dd HH:mm:ss */
    public static String now() {
        return format(LocalDateTime.now());
    }

    /** 当前时间对象 */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }

    /** 格式化为存储格式 */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(STORAGE_FORMATTER);
    }

    /** 格式化为分钟精度，提示语里用这个更简洁 */
    public static String formatShort(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(INPUT_FORMATTER);
    }

    /**
     * 解析用户输入：先按带秒的格式试，再按不带秒的格式试。
     * 两种都失败返回 null，由调用方给出提示——工具类不替业务决定报什么错。
     */
    public static LocalDateTime parse(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{STORAGE_FORMATTER, INPUT_FORMATTER}) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // 换下一个格式再试
            }
        }
        return null;
    }

    /** 用户输入格式说明，界面上直接拼这句话 */
    public static String inputPatternHint() {
        return "yyyy-MM-dd HH:mm";
    }
}
