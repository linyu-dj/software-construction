package campus.exception;

/**
 * 权限不足。
 *
 * 属于业务规则失败：操作本身合法，只是当前身份没有这个权限。
 * 从 BusinessException 派生，入口按父类捕获即可，不需要为此改任何调用方——开闭原则的又一次应用。
 */
public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
