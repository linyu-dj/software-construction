package campus.store;

import campus.entity.CampusResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内存资源存储，承担 GRASP 信息专家（Information Expert）职责。
 *
 * 与 UserStore 同一套路：集合在谁手里，查询与编号生成就归谁，
 * 业务层不遍历集合、不关心数据来源。编号生成置于此类，是因为「编号不复用」
 * 这条不变量依赖集合的当前状态，只有持有集合的对象才能保证它。
 *
 * V2.0 里 findById 仍然返回可能为 null 的对象，任务 8 会改成 Optional，
 * 这正好能对比两种写法。
 */
public class ResourceStore {

    /** 编号前缀，配合三位序号使用 */
    private static final String ID_PREFIX = "R";

    private final List<CampusResource> resources = new ArrayList<>();

    /** 启动时用文件加载结果填充 */
    public void replaceAll(List<CampusResource> loaded) {
        resources.clear();
        if (loaded != null) {
            resources.addAll(loaded);
        }
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }

    public int size() {
        return resources.size();
    }

    public void add(CampusResource resource) {
        resources.add(resource);
    }

    /** 按编号查找，找不到返回 null */
    public CampusResource findById(String id) {
        for (CampusResource resource : resources) {
            if (resource.getId().equals(id)) {
                return resource;
            }
        }
        return null;
    }

    /** 按编号移除，成功返回 true */
    public boolean removeById(String id) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId().equals(id)) {
                resources.remove(i);
                return true;
            }
        }
        return false;
    }

    /** 全部资源，按编号升序 */
    public List<CampusResource> findAllSortedById() {
        List<CampusResource> copy = new ArrayList<>(resources);
        copy.sort((a, b) -> a.getId().compareTo(b.getId()));
        return copy;
    }

    /** 只读快照，供持久化使用 */
    public List<CampusResource> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(resources));
    }

    /**
     * 生成下一个编号：取现有最大序号加一，格式 R001。
     * 不复用已删除的编号，避免"同一个编号先后指向两个资源"的歧义。
     */
    public String nextId() {
        int max = 0;
        for (CampusResource resource : resources) {
            int seq = parseSequence(resource.getId());
            if (seq > max) {
                max = seq;
            }
        }
        return ID_PREFIX + String.format("%03d", max + 1);
    }

    /** 从 R012 这样的编号里取出 12，解析失败按 0 处理 */
    private static int parseSequence(String id) {
        if (id == null || !id.startsWith(ID_PREFIX)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(ID_PREFIX.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
