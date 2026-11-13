package campus.exception;

/**
 * V1.4 业务异常父类。
 *
 * 继承 RuntimeException（非受检异常），原因是业务规则失败在程序的任何一层都可能发生，
 * 如果声明为受检异常，每一层方法签名都要挂 throws，调用链会被异常声明污染。
 *
 * 约定：message 直接写"给用户看的中文句子"，因为入口捕获后只打印 message，
 * 不再拼接技术细节，也不打印堆栈。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
