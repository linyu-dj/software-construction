package campus.entity;

import java.time.LocalDateTime;

/**
 * 预约记录实体。
 *
 * 只承载数据：字段、构造器、get/set、toString。
 * 「能不能约、能不能审」这些判断全部放在业务层，实体不写业务逻辑。
 *
 * 时间统一用 LocalDateTime，而不是字符串：
 * 字符串没法比较大小，冲突校验要一直解析来解析去；用时间类型后比较就是 isBefore / isAfter。
 */
public class ReserveRecord {

    /** 预约记录编号，全局唯一，格式 B001 */
    private String recordId;

    /** 关联的资源编号 */
    private String resourceId;

    /** 申请人账号 */
    private String applicantUsername;

    /** 提交申请的时间 */
    private LocalDateTime applyTime;

    /** 预约使用开始时间 */
    private LocalDateTime reserveStartTime;

    /** 预约使用结束时间 */
    private LocalDateTime reserveEndTime;

    /** 预约状态 */
    private ReserveStatus status;

    /** 备注，可空 */
    private String remark;

    public ReserveRecord() {
    }

    public ReserveRecord(String recordId, String resourceId, String applicantUsername,
                         LocalDateTime applyTime, LocalDateTime reserveStartTime,
                         LocalDateTime reserveEndTime, ReserveStatus status, String remark) {
        this.recordId = recordId;
        this.resourceId = resourceId;
        this.applicantUsername = applicantUsername;
        this.applyTime = applyTime;
        this.reserveStartTime = reserveStartTime;
        this.reserveEndTime = reserveEndTime;
        this.status = status;
        this.remark = remark;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getApplicantUsername() {
        return applicantUsername;
    }

    public void setApplicantUsername(String applicantUsername) {
        this.applicantUsername = applicantUsername;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public LocalDateTime getReserveStartTime() {
        return reserveStartTime;
    }

    public void setReserveStartTime(LocalDateTime reserveStartTime) {
        this.reserveStartTime = reserveStartTime;
    }

    public LocalDateTime getReserveEndTime() {
        return reserveEndTime;
    }

    public void setReserveEndTime(LocalDateTime reserveEndTime) {
        this.reserveEndTime = reserveEndTime;
    }

    public ReserveStatus getStatus() {
        return status;
    }

    public void setStatus(ReserveStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ReserveRecord{recordId='" + recordId + "', resourceId='" + resourceId
                + "', applicant='" + applicantUsername + "', applyTime=" + applyTime
                + ", start=" + reserveStartTime + ", end=" + reserveEndTime
                + ", status=" + status + ", remark='" + remark + "'}";
    }
}
