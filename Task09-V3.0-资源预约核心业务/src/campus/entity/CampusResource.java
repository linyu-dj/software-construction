package campus.entity;

/**
 * V2.0 校园资源实体。
 *
 * 只承载数据：编号、名称、类型、状态、位置、描述。
 * 不做「能不能被学生查看」「能不能删除」这类判断——那是业务层的责任，
 * 实体一旦开始判断权限，就再也说不清它的边界在哪。
 */
public class CampusResource {

    /** 资源唯一编号，由存储层统一生成，例如 R001 */
    private String id;

    /** 资源名称，例如「一号会议室」 */
    private String name;

    /** 资源类型：教室 / 实验室 / 设备 / 场地 */
    private String type;

    /** 资源状态 */
    private ResourceStatus status;

    /** 位置描述 */
    private String location;

    /** 简要描述，可为空 */
    private String description;

    /** 无参构造：供框架或反射场景实例化 */
    public CampusResource() {
    }

    /** 全参构造：新增资源时一次性组装完整对象 */
    public CampusResource(String id, String name, String type, ResourceStatus status,
                          String location, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.location = location;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** 编号是资源的业务唯一标识，按编号判等 */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampusResource)) {
            return false;
        }
        CampusResource other = (CampusResource) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "CampusResource{id='" + id + "', name='" + name + "', type='" + type
                + "', status=" + status.name() + ", location='" + location
                + "', description='" + description + "'}";
    }
}
