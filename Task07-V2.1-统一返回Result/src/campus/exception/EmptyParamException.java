package campus.exception;

/** 必填参数为空 */
public class EmptyParamException extends BusinessException {

    public EmptyParamException(String message) {
        super(message);
    }
}
