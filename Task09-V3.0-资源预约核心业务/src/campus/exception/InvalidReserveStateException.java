package campus.exception;

/** 预约状态不允许当前操作：例如已驳回的记录再次取消 */
public class InvalidReserveStateException extends BusinessException {

    public InvalidReserveStateException(String message) {
        super(message);
    }
}
