package campus.common;

/**
 * V2.1 统一返回结果，作为业务层与视图层之间的结果契约。
 *
 * 之前各业务方法的返回值很杂：有的返回对象、有的返回 boolean、查不到返回 null。
 * 调用方必须记住每个方法的约定，漏判一次就是空指针或者"操作成功了但没人知道"。
 *
 * 这个类只做一件事：把「成功与否 / 给用户看的话 / 业务数据」装在一起。
 * 它不做业务判断，不读写文件，也不打印任何东西——打印是视图层的事。
 *
 * 设计约束：构造器私有，只能经静态工厂创建，避免出现「成功但无内容」的实例；
 * 失败实例的 data 恒为 null，防止调用方在失败分支取到残留数据。
 * 类内不含业务判断、不做 IO、不输出控制台文本，属单一职责的边界。
 *
 * @param <T> 业务数据类型；无返回数据时用 Void
 */
public class Result<T> {

    /** 本次操作是否成功 */
    private boolean success;

    /** 业务提示，成功或失败都要有一句用户能读懂的中文 */
    private String msg;

    /** 业务数据，失败时为 null */
    private T data;

    private Result(boolean success, String msg, T data) {
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    /** 成功并携带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data);
    }

    /**
     * 成功，无返回数据，只有一句提示。
     *
     * 重载注意：实参为字符串且类型参数被推断为 String 时，编译器按「更具体的
     * 重载优先」选中本方法，字符串会被当作提示而非业务数据。该行为由
     * ResultTest 中的用例固定；若要消除歧义，可将工厂方法改名为 successWithMsg。
     */
    public static <T> Result<T> success(String msg) {
        return new Result<>(true, msg, null);
    }

    /** 成功，自定义提示与数据 */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(true, msg, data);
    }

    /** 失败，只带错误提示，data 一定是 null */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(false, msg, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Result{success=" + success + ", msg='" + msg + "', data=" + data + "}";
    }
}
