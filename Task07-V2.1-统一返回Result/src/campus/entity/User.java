package campus.entity;

/**
 * 用户实体类。
 *
 * 只负责承载数据：账号、密码、角色、注册时间。
 * 不包含任何业务判断（例如密码是否够长、账号是否重复），
 * 这类逻辑属于校验与服务层，放在实体里会造成职责混淆。
 */
public class User {

    /** 账号（业务上的唯一标识） */
    private String username;

    /** 密码 */
    private String password;

    /** 角色，取值见 campus.common.Roles */
    private String role;

    /** 注册时间，格式 yyyy-MM-dd HH:mm:ss */
    private String registerTime;

    /** 无参构造：供框架或反射场景实例化 */
    public User() {
    }

    /** 全参构造：注册成功时一次性组装完整对象 */
    public User(String username, String password, String role, String registerTime) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.registerTime = registerTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(String registerTime) {
        this.registerTime = registerTime;
    }

    /**
     * 按账号判等：账号是业务唯一标识，同名即同一用户。
     * 密码不参与比较——同一用户改密码后仍应是"同一个人"。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User other = (User) o;
        return username != null && username.equals(other.username);
    }

    /** 与 equals 保持一致：只对账号取哈希 */
    @Override
    public int hashCode() {
        return username == null ? 0 : username.hashCode();
    }

    /** 四个字段全部输出，便于在控制台核对数据是否完整 */
    @Override
    public String toString() {
        return "User{username='" + username + "', password='" + password + "', role='" + role
                + "', registerTime='" + registerTime + "'}";
    }
}
