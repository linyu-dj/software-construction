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

import java.util.ArrayList;
import java.util.List;

/**
 * 资源业务类（V2.1）。
 *
 * 与 V2.0 的差别只有返回值：全部改成 Result，不再返回对象或 boolean。
 * 业务逻辑、校验顺序、落盘时机都没有变化——这一轮不动行为，只统一"怎么把结果交出去"。
 *
 * 每个方法内部把 BusinessException 转成 Result.fail：
 * 好处是界面层不用认识七八种异常类型，只判断 isSuccess；
 * 代价是每个方法都有一段 try/catch，等任务 10 分层后再考虑收敛。
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

    /** 查询资源：管理员看全部，学生只看可预约的 */
    public Result<List<CampusResource>> listFor(User operator) {
        try {
            List<CampusResource> resources;
            if (isAdmin(operator)) {
                resources = store.findAllSortedById();
            } else {
                resources = new ArrayList<>();
                for (CampusResource resource : store.findAllSortedById()) {
                    if (resource.getStatus() == ResourceStatus.AVAILABLE) {
                        resources.add(resource);
                    }
                }
            }
            return Result.success("查询成功，共 " + resources.size() + " 条资源。", resources);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 按编号查询：学生只能看可预约资源，停用与占用的详情不对学生开放 */
    public Result<CampusResource> findById(User operator, String id) {
        try {
            Validator.requireNonBlank(id, "资源编号");
            CampusResource resource = store.findById(id.trim());
            if (resource == null) {
                throw new ResourceNotFoundException("未找到编号为 " + id + " 的资源。");
            }
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
            CampusResource resource = store.findById(id == null ? null : id.trim());
            if (resource == null) {
                throw new ResourceNotFoundException("未找到编号为 " + id + " 的资源。");
            }
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
            CampusResource resource = store.findById(trimmedId);
            if (resource == null) {
                throw new ResourceNotFoundException("未找到编号为 " + id + " 的资源。");
            }
            if (hasBookingRecord(trimmedId)) {
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
