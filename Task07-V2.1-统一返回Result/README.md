# Task07 · V2.1 统一返回 Result

## 本任务目标

把上一版五花八门的返回值（对象、`boolean`、`null`）统一成 `Result<T>`，让调用方只看一个结构就能判断成败，并顺手把用户业务从主类搬进 `UserManager`。

## 目录结构（只列新增与改动）

```
Task07-V2.1-统一返回Result/
├─ src/campus/
│  ├─ SystemMain.java            只判断 Result，不再认识异常类型
│  ├─ common/
│  │  ├─ Result.java             新增：泛型结果封装
│  │  └─ Roles.java
│  ├─ biz/
│  │  ├─ UserManager.java        新增：注册 / 登录 / 查询
│  │  └─ ResourceManager.java    五个方法改为返回 Result
│  ├─ entity/ · store/ · utils/ · exception/   沿用 V2.0
```

## Result 设计

```java
public class Result<T> {
    private boolean success;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data)
    public static <T> Result<T> success(String msg)
    public static <T> Result<T> success(String msg, T data)
    public static <T> Result<T> fail(String msg)
}
```

四条约束：

1. 构造器私有，只能走静态工厂，避免出现 `success=true, msg=null` 这种半成品
2. `fail` 的 `data` 一定为 `null`，调用方拿到失败结果就不该再去取数据
3. 类里没有任何业务判断、不碰 IO、不打印
4. 无数据返回时用 `Result<Void>`

## 改造范围

| 模块 | 方法 | 改造前 | 改造后 |
|---|---|---|---|
| 用户 | 注册 | `void` | `Result<User>` |
| 用户 | 登录 | 返回 `User` 或抛异常 | `Result<User>` |
| 用户 | 按账号查询 | 返回 `User` 或 `null` | `Result<User>` |
| 资源 | 新增 | 返回 `CampusResource` | `Result<CampusResource>` |
| 资源 | 查询列表 | 返回 `List` | `Result<List<CampusResource>>` |
| 资源 | 按编号查询 | 返回对象或 `null` | `Result<CampusResource>` |
| 资源 | 修改 / 删除 | `void` | `Result<Void>` |

## 异常与 Result 怎么配合

任务书要求两套机制都保留。做法是：校验失败照旧抛自定义异常，业务方法内部捕获后转成 `Result.fail(e.getMessage())`。

```java
public Result<User> login(String username, String password) {
    try {
        Validator.requireValidUsername(store, username, false);
        ...
        return Result.success("登录成功，欢迎您 " + username + "。", user);
    } catch (BusinessException e) {
        return Result.fail(e.getMessage());
    }
}
```

界面层因此只需要判断 `isSuccess()`，不用 `catch` 七八种异常；系统故障（`UncheckedIOException`）仍然向上抛，由入口统一打印"系统异常，请稍后重试。"

## 界面层怎么用

```java
Result<User> result = USER_MANAGER.login(username, password);
if (!result.isSuccess()) {
    System.out.println("操作未完成：" + result.getMsg());
    return null;
}
printWelcome(result.getData());
```

`printResult` 是统一打印口：成功打印 `msg`，失败加"操作未完成："前缀。

## 验收

V2.0 的 11 组场景原样重跑，全部通过，输出文案与改前一致（新增/修改/删除的成功提示多带了"资源"两字，属于提示措辞，不影响流程）：

- 首启写 2 账号 + 3 资源
- 管理员查到 3 条，学生只看到 2 条可预约
- 修改 R002 为占用后，学生列表里不再出现
- 新增 R004、重启加载 4 条、删除 R004、查 R999 失败、状态序号 4 被拒、空名称被拒
- 堆栈扫描零命中

## 设计原则落地

**单一职责**：`Result` 只封装结果，不做业务判断。
**迪米特法则**：界面层只读 `success / msg / data` 三个属性，不需要知道校验器、存储、文件的存在。

## 已知局限

- 每个业务方法都写了一遍 `try/catch (BusinessException)`，重复度较高。任务书要求本阶段不做分层，所以先保留；等任务 10 之后可以收敛到统一入口。
- `Result.success(String msg)` 与 `Result.success(T data)` 在 `T` 为 `String` 时存在重载歧义，当前代码里没有这种用法，写的时候注意别踩。

## 单元测试

本目录包含 25 条单元测试，使用纯 Java 断言实现，不引入第三方测试框架，源码位于 `test/campus/test/`：

- `ResultTest` 6 条、`UserManagerTest` 9 条、`ResourceManagerTest` 10 条
- 基础设施：`Assert` 提供断言与失败信息，`TestRunner` 负责逐条执行、汇总并通过退出码返回结果

运行方式：双击 `test.bat`，或在该目录下执行 `test.bat`。测试在 `testtmp` 临时目录中运行，不会修改演示数据文件；全部通过时输出 `ALL UNIT TESTS PASSED`。
