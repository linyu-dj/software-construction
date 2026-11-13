# Task06 · V2.0 校园资源管理

## 本任务目标

在 V1.4 的登录注册之上加入第一个业务对象：校园资源。管理员能增删改查，学生只能看状态为可预约的资源，资源数据落到 `resource.txt`，重启不丢。

## 目录结构

```
Task06-V2.0-校园资源管理/
├─ src/
│  └─ campus/
│     ├─ SystemMain.java            主类：游客菜单 + 角色菜单
│     ├─ biz/
│     │  └─ ResourceManager.java    资源业务：权限、校验、增删改查
│     ├─ common/
│     │  └─ Roles.java              角色常量
│     ├─ entity/
│     │  ├─ CampusResource.java     资源实体
│     │  ├─ ResourceStatus.java     三态枚举
│     │  └─ User.java              用户实体
│     ├─ store/
│     │  ├─ ResourceStore.java      资源集合 + 编号生成 + 查询
│     │  └─ UserStore.java          用户集合
│     ├─ utils/
│     │  ├─ FileUtil.java           用户 / 资源两套读写
│     │  ├─ TimeUtil.java           时间格式化
│     │  └─ Validator.java          参数与权限校验
│     └─ exception/
│        ├─ BusinessException.java            业务异常父类
│        ├─ UserExistsException.java          用户已存在
│        ├─ UserNotFoundException.java        用户不存在
│        ├─ WrongPasswordException.java       密码错误
│        ├─ EmptyParamException.java          参数为空
│        ├─ PermissionDeniedException.java    权限不足
│        └─ ResourceNotFoundException.java    资源不存在
├─ resource.txt                     运行时生成（已 gitignore）
├─ user.txt                         运行时生成（已 gitignore）
└─ run.bat
```

## 新增内容

### 1. 资源实体与三态

`CampusResource` 六个字段：`id` / `name` / `type` / `status` / `location` / `description`。

`status` 用枚举 `ResourceStatus` 而不是字符串：

```java
AVAILABLE("可预约"), OCCUPIED("占用"), DISABLED("停用")
```

三态是封闭集合，用枚举能把非法状态挡在编译期。落盘用 `name()`，显示用中文标签——存储格式稳定，界面可以随便改。

### 2. 编号生成规则

`ResourceStore.nextId()` 取现有最大序号 + 1，格式 `R001`。不复用已删除的编号，否则同一个编号会先后指向两个不同资源，日志和预约记录都会对不上。

### 3. 角色菜单

| 身份 | 菜单 |
|---|---|
| 管理员 | 新增 / 查询全部 / 修改 / 删除 / 退出 |
| 学生 | 查询可预约资源 / 退出 |

学生菜单里根本没有增删改选项，这是第一道权限门；第二道门在 `ResourceManager` 里——`create` / `update` / `delete` 开头都调 `Validator.requireAdmin`，即使有人绕过菜单直接调方法也会被 `PermissionDeniedException` 拦住。

### 4. 学生看不到停用 / 占用详情

`listFor` 对学生的返回值过滤掉非 `AVAILABLE` 的资源；`findById` 也做了同样处理——不是显示"无权查看"，而是直接按"找不到该编号的可预约资源"回应，避免透露资源存在这件事本身。

## 文件格式

```
resource.txt   id|name|type|status|location|description
```

字段不足 6 个、状态值无法识别、空行，都按坏行跳过，一条脏数据不会让整个启动失败。

## 验收用例

| 用例 | 操作 | 预期 |
|---|---|---|
| 首启 | 直接退出 | 自动创建 `user.txt` / `resource.txt`，写入 2 个账号 + 3 个示例资源 |
| 管理员全量查询 | 登录 → `2` | 列出 R001~R003，状态均为可预约 |
| 修改为占用 | 登录 → `3` → R002 → 新字段 → 状态 `2` | R002 状态变为占用并落盘 |
| 学生查询 | 登录 student → `1` | 只显示 R001、R003，R002 不出现 |
| 新增 | 登录 → `1` → 填四字段 | 生成 R004，默认状态可预约 |
| 重启 | 退出后重开 | `已从 resource.txt 加载 4 个资源` |
| 删除 | 登录 → `4` → R004 | 删除成功，文件同步 |
| 删除不存在 | `4` → R999 | `操作未完成：未找到编号为 R999 的资源。` |
| 非法状态 | `3` → 状态 `4` | `操作未完成：状态序号只能是 1、2 或 3。` |
| 名称为空 | `1` → 名称留空 | `操作未完成：资源名称不能为空。` |

十组实测通过，无 `Exception in thread` / `at campus.` / `printStackTrace`。

## 设计原则落地

**单一职责**：用户实体与资源实体互不引用；`ResourceManager` 只做业务编排，集合操作在 `ResourceStore`，文件读写只走 `FileUtil`。

**信息专家**：编号生成和按编号查找都在持有集合的 `ResourceStore` 里，业务层不遍历。

**开闭原则**：新增 `PermissionDeniedException`、`ResourceNotFoundException` 时，入口的 `catch (BusinessException e)` 一行没改。

## 已知局限

- 学生尝试越权增删改，在控制台层面没有入口，这条只能通过直接调 `ResourceManager` 验证，留到单元测试覆盖。
- 删除校验里的"存在预约记录禁止删除"目前是钩子方法返回 `false`，任务 9 接入预约模块后替换为真实查询。
- 同名资源没有做唯一性校验（任务书标注为可选）。

## 单元测试

本目录包含 26 条单元测试，使用纯 Java 断言实现，不引入第三方测试框架，源码位于 `test/campus/test/`：

- `ResourceStatusTest` 6 条、`ResourceStoreTest` 8 条、`ResourceManagerTest` 12 条（含越权调用）
- 基础设施：`Assert` 提供断言与失败信息，`TestRunner` 负责逐条执行、汇总并通过退出码返回结果

运行方式：双击 `test.bat`，或在该目录下执行 `test.bat`。测试在 `testtmp` 临时目录中运行，不会修改演示数据文件；全部通过时输出 `ALL UNIT TESTS PASSED`。
