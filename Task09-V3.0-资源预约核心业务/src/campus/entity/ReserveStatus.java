package campus.entity;

/**
 * 预约状态枚举。
 *
 * 四个状态构成一条明确的流转线：待审核 → 通过 / 驳回；待审核或通过 → 取消。
 * 用枚举而不是字符串，是为了让「合法状态」在编译期就被限定，落盘值也稳定。
 */
public enum ReserveStatus {

    PENDING("待审核"),
    PASS("已通过"),
    REJECT("已驳回"),
    CANCEL("已取消");

    /** 界面展示用中文标签 */
    private final String label;

    ReserveStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 从落盘值还原状态，不认识返回 null。
     * 解析失败由读取方决定跳过整行，枚举本身不猜默认值。
     */
    public static ReserveStatus parse(String text) {
        if (text == null) {
            return null;
        }
        for (ReserveStatus status : values()) {
            if (status.name().equals(text.trim())) {
                return status;
            }
        }
        return null;
    }

    /** 该状态是否已经走到终点，不能再变更 */
    public boolean isFinal() {
        return this == REJECT || this == CANCEL;
    }

    @Override
    public String toString() {
        return label;
    }
}
