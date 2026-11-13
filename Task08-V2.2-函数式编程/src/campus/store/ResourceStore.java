package campus.store;

import campus.entity.CampusResource;
import campus.entity.ResourceStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 内存资源存储，承担信息专家（Information Expert）职责，V2.2 改为函数式查询。
 *
 * 与 UserStore 同一套路：集合在谁手里，查询与编号生成就归谁。
 *
 * 这一轮把集合遍历全部换成 Stream：
 * 一是去掉手写的循环和临时变量，二是把「筛选什么、按什么排序」写在一条链上，
 * 读代码时不用在脑子里模拟下标。
 *
 * 每个方法里都保留了旧 for 循环写法（注释形式），方便对照两种写法的差别。
 * 约束：查询方法只读集合，不修改原集合、不做 IO；返回列表为新建副本，
 * 调用方修改副本不影响容器状态。该约束使同一查询可重复调用且结果稳定。
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

    /**
     * 按编号查找，返回 Optional，把「可能找不到」写进类型里。
     *
     * 旧写法（V2.1）：
     *   for (CampusResource resource : resources) {
     *       if (resource.getId().equals(id)) {
     *           return resource;
     *       }
     *   }
     *   return null;
     * 旧写法的问题不在循环本身，而在返回值可以是 null —— 调用方漏判一次就是空指针。
     */
    public Optional<CampusResource> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return resources.stream()
                .filter(resource -> resource.getId().equals(id))
                .findFirst();
    }

    /**
     * 按状态筛选，返回副本。
     *
     * 旧写法：
     *   List<CampusResource> result = new ArrayList<>();
     *   for (CampusResource resource : resources) {
     *       if (resource.getStatus() == status) {
     *           result.add(resource);
     *       }
     *   }
     *   return result;
     */
    public List<CampusResource> findByStatus(ResourceStatus status) {
        return resources.stream()
                .filter(resource -> resource.getStatus() == status)
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
    }

    /**
     * 名称模糊查询，忽略大小写，结果按编号升序。
     *
     * 旧写法：
     *   List<CampusResource> result = new ArrayList<>();
     *   for (CampusResource resource : resources) {
     *       if (resource.getName().toLowerCase().contains(keyword)) {
     *           result.add(resource);
     *       }
     *   }
     *   result.sort(...);
     *   return result;
     */
    public List<CampusResource> findByNameKeyword(String keyword) {
        String normalized = normalizeKeyword(keyword);
        return resources.stream()
                .filter(resource -> resource.getName().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
    }

    /**
     * 多条件组合查询：状态可空、关键词可空，两者同时生效。
     * 条件为空表示「不限」，所以用 status == null 与空字符串分别代表不筛选。
     */
    public List<CampusResource> search(ResourceStatus status, String keyword) {
        String normalized = normalizeKeyword(keyword);
        return resources.stream()
                .filter(resource -> status == null || resource.getStatus() == status)
                .filter(resource -> normalized.isEmpty()
                        || resource.getName().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
    }

    /** 全部资源，按编号升序 */
    public List<CampusResource> findAllSortedById() {
        // 旧写法：new ArrayList<>(resources) 后 copy.sort(...)
        return resources.stream()
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
    }

    /** 按编号移除，成功返回 true */
    public boolean removeById(String id) {
        // 旧写法：for 找下标，resources.remove(i)，再 return true
        return resources.removeIf(resource -> resource.getId().equals(id));
    }

    /** 只读快照，供持久化使用 */
    public List<CampusResource> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(resources));
    }

    /**
     * 生成下一个编号：取现有最大序号加一，格式 R001。
     * 不复用已删除的编号，避免「同一个编号先后指向两个资源」的歧义。
     *
     * 旧写法：循环比较 max；现在一行 mapToInt().max()。
     */
    public String nextId() {
        int max = resources.stream()
                .mapToInt(resource -> parseSequence(resource.getId()))
                .max()
                .orElse(0);
        return ID_PREFIX + String.format("%03d", max + 1);
    }

    /** 关键词统一处理：null 当空串，去首尾空格，转小写 */
    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
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
