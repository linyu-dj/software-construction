package campus.store;

import campus.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内存用户存储（信息专家）。
 *
 * 「谁持有数据，谁负责查询」——集合在本类手里，所以按用户名查找也放在这里，
 * 视图层不需要自己遍历，更不需要知道数据从文件来还是从内存来。
 *
 * 职责边界：只做集合管理与查询。
 * 不做参数校验（那是 Validator 的事），不碰文件（那是 FileUtil 的事）。
 * 这种「为了职责清晰而造出来的类」，在设计模式语境里属于纯虚构（Pure Fabrication）。
 */
public class UserStore {

    private final List<User> users = new ArrayList<>();

    /** 启动时用文件加载结果填充，会先清空当前内容，保证内存与文件一致 */
    public void replaceAll(List<User> loaded) {
        users.clear();
        if (loaded != null) {
            users.addAll(loaded);
        }
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public int size() {
        return users.size();
    }

    /** 新增用户：调用前必须已完成校验 */
    public void add(User user) {
        users.add(user);
    }

    /** 按用户名查找，找不到返回 null（null 由调用方决定转成哪种异常） */
    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public boolean exists(String username) {
        return findByUsername(username) != null;
    }

    /**
     * 返回只读快照，供持久化使用。
     * 不直接对外暴露内部 List，避免调用方绕过 add 直接修改集合（防御性封装）。
     */
    public List<User> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(users));
    }
}
