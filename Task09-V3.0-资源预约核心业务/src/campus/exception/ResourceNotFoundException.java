package campus.exception;

/** 资源不存在：按编号查找失败时抛出 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
