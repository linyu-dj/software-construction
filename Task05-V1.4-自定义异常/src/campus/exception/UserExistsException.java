package campus.exception;

/** 注册时账号已被占用 */
public class UserExistsException extends BusinessException {

    public UserExistsException(String message) {
        super(message);
    }
}
