package campus.exception;

/** 预约记录不存在：按记录编号查找失败时抛出 */
public class ReserveNotFoundException extends BusinessException {

    public ReserveNotFoundException(String message) {
        super(message);
    }
}
