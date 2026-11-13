package campus.exception;

/** 时间段冲突：同一资源在重叠时段已有已通过的预约 */
public class TimeConflictException extends BusinessException {

    public TimeConflictException(String message) {
        super(message);
    }
}
