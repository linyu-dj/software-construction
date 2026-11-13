# Task03 · V1.2 User 实体类重构

## 本任务目标

解决 V1.1 只能存"用户名 → 密码"、无法扩展用户信息的问题：新增 `User` 实体类，把用户数据升级为对象，存储结构由 `HashMap` 改为 `List<User>`。

## 目录结构

```
Task03-V1.2-用户实体类重构/
├─ src/
│  └─ campus/
│     ├─ SystemMain.java        主类：菜单、登录、注册、校验、展示
│     └─ entity/
│        └─ User.java           实体类：只承载数据，不含业务逻辑
└─ run.bat
```

## User 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `username` | String | 账号 |
| `password` | String | 密码 |
| `role` | String | 角色：`admin` / `student` |
| `registerTime` | String | 注册时间，格式 `yyyy-MM-dd HH:mm:ss` |

构造与方法：无参构造、全参构造、四个字段的 get/set、重写 `toString()`。

## 本版本改了什么

| 项 | V1.1 | V1.2 |
|---|---|---|
| 存储 | `HashMap<String, String>`（用户名 → 密码） | `List<User>` |
| 用户信息 | 只有账号和密码 | 账号、密码、角色、注册时间 |
| 身份判断 | 靠用户名是否为 `admin` 推断 | 直接读 `user.getRole()` |
| 注册结果 | 只打印"注册成功" | 同时打印完整用户对象 |

## 运行方式

```powershell
cd "Task03-V1.2-用户实体类重构"
.\run.bat
```

IDEA 里打开本文件夹，把 `src` 标为 Sources Root 后运行 `SystemMain` 即可。

## 验收用例

| 用例 | 输入 | 预期输出 |
|---|---|---|
| 注册并查看对象 | `2` → `zhangsan` → `123456` | 注册信息含 `role='student'` 与 `registerTime='yyyy-MM-dd HH:mm:ss'` |
| 注册后登录 | `1` → `zhangsan` → `123456` | 身份 STUDENT，打印注册时间与完整用户信息 |
| 管理员登录 | `1` → `admin` → `123456` | 身份 ADMIN，`role='admin'` |
| 用户名查重 | `2` → `student` → `654321` | 注册失败：账号 student 已存在 |
| 密码长度不足 | `2` → `lisi` → `abc12` | 注册失败：密码长度不能少于 6 位 |

## 设计原则落地

- **封装**：`User` 的四个字段全部私有，只通过 get/set 暴露，`toString()` 统一对外表述。
- **单一职责**：`User` 只负责承载数据；"密码够不够长""账号是否重复"仍留在 `SystemMain` 的校验方法里。实体一旦掺入业务判断，后面做文件持久化时就要被迫处理 `IOException`，职责会彻底混掉。

## 已知局限（后续版本接手）

- 数据仍在内存里，退出即丢 → V1.3 落盘到 `user.txt`
- 业务失败仍返回字符串提示，无法按错误类型分流 → V1.4 引入自定义异常
- `findUser` 按用户名线性遍历，用户量大时是 O(n)；本课程规模可接受，V3.x 三层架构时会改为按 ID 索引

## 单元测试

本目录包含 5 条单元测试，使用纯 Java 断言实现，不引入第三方测试框架，源码位于 `test/campus/test/`：

- `UserTest` 5 条（全参 / 无参构造、setter 独立性、角色变更、`toString`）
- 基础设施：`Assert` 提供断言与失败信息，`TestRunner` 负责逐条执行、汇总并通过退出码返回结果

运行方式：双击 `test.bat`，或在该目录下执行 `test.bat`。测试在 `testtmp` 临时目录中运行，不会修改演示数据文件；全部通过时输出 `ALL UNIT TESTS PASSED`。
