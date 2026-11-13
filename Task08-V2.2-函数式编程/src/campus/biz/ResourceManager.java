package campus.biz;

import campus.common.Result;
import campus.common.Roles;
import campus.entity.CampusResource;
import campus.entity.ResourceStatus;
import campus.entity.User;
import campus.exception.BusinessException;
import campus.exception.EmptyParamException;
import campus.exception.ResourceNotFoundException;
import campus.store.ResourceStore;
import campus.utils.FileUtil;
import campus.utils.Validator;

import java.util.List;

/**
 * 资源业务类（V2.2）。
 *
 * 这一轮只改查询：列表筛选、名称模糊查询、按编号查找全部换成 Stream + Optional。
 * 新增、修改、删除的业务逻辑与持久化时机没有动，权限规则也没动。
 *
 * Optional 的用法集中在这里：store 返回 Optional，业务层用 orElseThrow 把「找不到」
 * 转成一句中文提示，再用 Result.fail 交出去——这样就不会出现 null 对象被直接取属性的情况。
 */
public class ResourceManager {

    private final ResourceStore store;

    public ResourceManager(ResourceStore store) {
        this.store = store;
    }

    /** 新增资源：管理员专属，编号自动生成，状态默认可预约 */
    public Result<CampusResource> create(User operator, String name, String type,
                                         String location, String description) {
        try {
            Validator.requireAdmin(operator, "新增校园资源");
            requireValidFields(name, type, location, description);

            CampusResource resource = new CampusResource(
                    store.nextId(), name.trim(), type.trim(), ResourceStatus.AVAILABLE,
                    location.trim(), description == null ? "" : description.trim());
            store.add(resource);
            FileUtil.appendResource(resource);
            return Result.success("资源新增成功：" + resource.getId() + " " + resource.getName(), resource);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询资源列表：管理员看全部，学生只看可预约的。
     *
     * 旧写法（V2.1）：new ArrayList 后 for 循环逐条判断状态再 add。
     * 现在筛选交给 ResourceStore，业务层只决定「用哪个条件」。
     */
    public Result<List<CampusResource>> listFor(User operator) {
        try {
            List<CampusResource> resources = isAdmin(operator)
                    ? store.findAllSortedById()
                    : store.findByStatus(ResourceStatus.AVAILABLE);
            return Result.success("查询成功，共 " + resources.size() + " 条资源。", resources);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 名称模糊查询：管理员在全部资源里搜，学生只在可预约资源里搜。
     * 组合条件交给 store.search(status, keyword)，这个方法是新增的，行为上等价于
     * 「先按状态筛，再按名称筛，最后按编号排序」。
     */
    public Result<List<CampusResource>> searchByName(User operator, String keyword) {
        try {
            ResourceStatus status = isAdmin(operator) ? null : ResourceStatus.AVAILABLE;
            List<CampusResource> resources = store.search(status, keyword);
            return Result.success("查询成功，匹配「" + (keyword == null ? "" : keyword.trim())
                    + "」的资源共 " + resources.size() + " 条。", resources);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 按编号查询。
     *
     * Optional 链：findById 找不到时 orElseThrow 抛 ResourceNotFoundException，
     * 抛出的异常被下面的 catch 转成 Result.fail。整条链上没有 null 被直接使用。
     *
     * 学生查非可预约资源时同样抛出「未找到可预约资源」——不透露资源是否存在。
     */
    public Result<CampusResource> findById(User operator, String id) {
        try {
            Validator.requireNonBlank(id, "资源编号");
            CampusResource resource = store.findById(id.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到编号为 " + id + " 的资源。"));
            if (!isAdmin(operator) && resource.getStatus() != ResourceStatus.AVAILABLE) {
                throw new ResourceNotFoundException("未找到编号为 " + id + " 的可预约资源。");
            }
            return Result.success("查询成功。", resource);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 修改资源：名称、类型、位置、状态四项，管理员专属 */
    public Result<Void> update(User operator, String id, String name, String type,
                               String location, ResourceStatus status) {
        try {
            Validator.requireAdmin(operator, "修改校园资源");
            CampusResource resource = store.findById(id == null ? null : id.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到编号为 " + id + " 的资源。"));
            requireValidFields(name, type, location, "");
            if (status == null) {
                throw new EmptyParamException("资源状态不能为空。");
            }

            resource.setName(name.trim());
            resource.setType(type.trim());
            resource.setLocation(location.trim());
            resource.setStatus(status);
            FileUtil.saveResources(store.snapshot());
            return Result.success("资源修改成功：" + id + " 已更新为 " + status.getLabel() + "。");
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 删除资源：管理员专属；存在已通过预约的资源禁止删除（任务 9 接入真实校验） */
    public Result<Void> delete(User operator, String id) {
        try {
            Validator.requireAdmin(operator, "删除校园资源");
            String trimmedId = id == null ? null : id.trim();
            CampusResource resource = store.findById(trimmedId)
                    .orElseThrow(() -> new ResourceNotFoundException("未找到编号为 " + id + " 的资源。"));
            if (hasBookingRecord(resource.getId())) {
                throw new BusinessException("资源 " + trimmedId + " 存在预约记录，禁止删除。");
            }
            store.removeById(trimmedId);
            FileUtil.saveResources(store.snapshot());
            return Result.success("删除成功：" + trimmedId + " 已从资源列表中移除。");
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 判断资源是否有预约记录。
     * V2.0 起先留成钩子返回 false；任务 9 会替换为真实查询，不动调用点。
     */
    private boolean hasBookingRecord(String resourceId) {
        return false;
    }

    private static boolean isAdmin(User operator) {
        return operator != null && Roles.ADMIN.equals(operator.getRole());
    }

    private void requireValidFields(String name, String type, String location, String description) {
        Validator.requireNonBlank(name, "资源名称");
        Validator.requireNonBlank(type, "资源类型");
        Validator.requireNonBlank(location, "资源位置");
        Validator.requireTextWithoutPipe(name, "资源名称");
        Validator.requireTextWithoutPipe(type, "资源类型");
        Validator.requireTextWithoutPipe(location, "资源位置");
        Validator.requireTextWithoutPipe(description, "资源描述");
    }
}
