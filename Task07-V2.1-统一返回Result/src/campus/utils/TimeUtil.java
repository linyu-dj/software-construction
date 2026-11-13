package campus.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类。
 *
 * 把注册时间的格式串收在一处：调用方写 TimeUtil.now() 即可，
 * 不需要知道具体格式，也就不会出现两处 pattern 不一致的问题。
 */
public final class TimeUtil {

    /** 全项目统一的展示/存储格式 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
        // 工具类不允许实例化
    }

    /** 当前时间，格式 yyyy-MM-dd HH:mm:ss */
    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
