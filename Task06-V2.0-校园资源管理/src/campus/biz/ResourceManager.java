package campus.biz;

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
 * 资源业务类（V2.0），对应 GRASP 中的控制器（Controller）。
 *
 * 职责是编排：校验权限与参数 → 改内存 → 同步文件。控制器的定位是接收请求、
 * 协调协作对象并返回结果，不实现界面细节，也不直接管理数据结构。
 * 集合与编号生成交给 ResourceStore，文件读写交给 FileUtil，
 * 本类不直接操作 List、不接触 IO 流：集合细节属存储类的信息专家职责，
 * 文件格式属工具类的纯虚构职责。三方各守边界，构成模块的低耦合基础。
 *
 * 说明：任务 10 才做三层分包，本类暂时放在 biz 包，承担「服务层」的职责，
 * 到 V3.1 会整体搬进 service 包，业务逻辑不需要重写。
 */
public class ResourceManager {

    private final ResourceStore store;

    public ResourceManager(ResourceStore store) {
        this.store = store;
    }

    /** 新增资源：管理员专属，编号自动生成，状态默认可预约 */
    public CampusResource create(User operator, String name, String type, String location, String description) {
        Validator.requireAdmin(operator, "新增校园资源");
        requireValidFields(name, type, location, description);

        CampusResource resource = new CampusResource(
                store.nextId(), name.trim(), type.trim(), ResourceStatus.AVAILABLE,
                location.trim(), description == null ? "" : description.trim());
        store.add(resource);
        FileUtil.appendResource(resource);
        return resource;
    }

    /** 查询资源：管理员看全部，学生只看可预约的 */
    public List<CampusResource> listFor(User operator) {
        if (isAdmin(operator)) {
            return store.findAllSortedById();
        }
        List<CampusResource> available = new ArrayList<>();
        for (CampusResource resource : store.findAllSortedById()) {
            if (resource.getStatus() == ResourceStatus.AVAILABLE) {
                available.add(resource);
            }
        }
        return available;
    }

    /** 按编号查询：学生只能看可预约资源，停用与占用的详情不对学生开放 */
    public CampusResource findById(User operator, String id) {
        Validator.requireNonBlank(id, "资源编号");
        CampusResource resource = store.findById(id.trim());
        if (resource == null) {
            throw new ResourceNotFoundException("未找到编号为 " + id + " 的资源。");
        }
        if (!isAdmin(operator) && resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new ResourceNotFoundException("未找到编号为 " + id + " 的可预约资源。");
        }
        return resource;
    }

    /** 修改资源：名称、类型、位置、状态四项，管理员专属 */
    public void update(User operator, String id, String name, String type,
                       String location, ResourceStatus status) {
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
    }

    /** 删除资源：管理员专属；存在已通过预约的资源禁止删除（任务 9 接入真实校验） */
    public void delete(User operator, String id) {
        Validator.requireAdmin(operator, "删除校园资源");
        String trimmedId = id == null ? null : id.trim();
        CampusResource resource = store.findById(trimmedId);
        if (resource == null) {
            throw new ResourceNotFoundException("未找到编号为 " + id + " 的资源。");
        }
        if (hasBookingRecord(trimmedId)) {
            throw new BusinessException(
                    "资源 " + trimmedId + " 存在预约记录，禁止删除。");
        }
        store.removeById(trimmedId);
        FileUtil.saveResources(store.snapshot());
    }

    /**
     * 判断资源是否有预约记录。
     * V2.0 还没有预约模块，先留成钩子返回 false；
     * 任务 9 会把这里替换成真实查询，而不是删掉校验逻辑。
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
