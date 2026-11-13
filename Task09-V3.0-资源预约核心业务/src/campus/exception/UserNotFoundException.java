package campus.exception;

/** 登录时账号不存在 */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
