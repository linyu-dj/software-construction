package campus.entity;

/**
 * 资源状态。
 *
 * 任务书规定只有三种取值，属于封闭集合，用枚举比字符串安全：
 * 拼错、传进非法值都会在编译期或解析处被挡住，不会等到运行时才发现。
 *
 * 落盘时用 name()（AVAILABLE / OCCUPIED / DISABLED），显示时用中文标签。
 */
public enum ResourceStatus {

    /** 可预约 */
    AVAILABLE("可预约"),

    /** 占用中 */
    OCCUPIED("占用"),

    /** 已停用 */
    DISABLED("停用");

    private final String label;

    ResourceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 按枚举名解析，非法值返回 null，由调用方决定跳过还是报错 */
    public static ResourceStatus parse(String name) {
        if (name == null) {
            return null;
        }
        for (ResourceStatus status : values()) {
            if (status.name().equals(name.trim())) {
                return status;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
