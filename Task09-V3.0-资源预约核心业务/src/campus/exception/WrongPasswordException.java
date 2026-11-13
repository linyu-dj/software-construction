package campus.exception;

/** 登录时密码与账号不匹配 */
public class WrongPasswordException extends BusinessException {

    public WrongPasswordException(String message) {
        super(message);
    }
}
