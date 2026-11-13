# Task05 · V1.4 自定义异常

## 本任务目标

把"业务失败靠返回字符串"改成"业务失败抛异常"，并在程序入口统一捕获，只输出中文提示，不向用户暴露堆栈。

## 目录结构

```
Task05-V1.4-自定义异常/
├─ src/
│  └─ campus/
│     ├─ SystemMain.java            主类：流程编排，统一捕获
│     ├─ common/
│     │  └─ Roles.java              角色常量（admin / student）
│     ├─ entity/
│     │  └─ User.java               实体类，含 equals/hashCode
│     ├─ store/
│     │  └─ UserStore.java          内存用户持有者（信息专家）
│     ├─ utils/
│     │  ├─ FileUtil.java           文件读写，IO 失败抛 UncheckedIOException
│     │  ├─ TimeUtil.java           时间格式化
│     │  └─ Validator.java          参数校验（失败即抛）
│     └─ exception/
│        ├─ BusinessException.java            业务异常父类
│        ├─ UserExistsException.java          用户已存在
│        ├─ UserNotFoundException.java        用户不存在
│        ├─ WrongPasswordException.java       密码错误
│        └─ EmptyParamException.java          参数为空
├─ user.txt                         运行时自动生成（已 gitignore）
└─ run.bat
```

### 分层职责

| 类 | 只做什么 | 不做什么 |
|---|---|---|
| `SystemMain` | 读输入、调校验、组装对象、落盘、展示 | 不持有用户集合、不遍历查找 |
| `UserStore` | 持有 `List<User>`、按用户名查找 | 不校验、不读文件 |
| `Validator` | 判断参数是否合法，不合法抛异常 | 不打印提示、不写文件 |
| `FileUtil` | 文件与对象列表之间转换 | 不判断业务规则、不打印业务提示 |
| `User` | 承载四个字段 | 不写任何业务判断 |

## 异常体系

```
RuntimeException
└─ BusinessException               ← 统一父类，message 即用户可见中文
   ├─ UserExistsException
   ├─ UserNotFoundException
   ├─ WrongPasswordException
   └─ EmptyParamException
```

## 抛出位置

| 异常 | 抛出位置 | 触发条件 |
|---|---|---|
| `EmptyParamException` | `Validator.requireValidUsername` / `requireValidPassword` | 账号或密码为空、账号含 `\|` |
| `UserExistsException` | `Validator.requireValidUsername`（注册场景） | 账号已被占用 |
| `UserNotFoundException` | `SystemMain.login` | 用户名在 `UserStore` 中查不到 |
| `WrongPasswordException` | `SystemMain.login` | 密码与记录不一致 |
| `BusinessException`（父类） | `Validator.requireValidPassword`（注册场景） | 密码长度不足 6 位 |

## 本版本改了什么

| 项 | V1.3 | V1.4 |
|---|---|---|
| 校验方法 | 返回 `String`，通过返回 `null` | 返回 `void`，失败直接 `throw` |
| 登录失败 | 打印提示后 `return false` | 抛出对应子异常 |
| 异常处理位置 | 每个分支各自打印 | `main` 菜单循环统一 `catch` |
| 用户看到的内容 | 各自拼接的提示串 | `异常.getMessage()`，统一加"操作未完成："前缀 |
| 堆栈 | 未处理（若异常逃逸会打印） | 捕获后不输出，业务异常与非业务异常都不打 |

## 运行方式

```powershell
cd "Task05-V1.4-自定义异常"
.\run.bat
```

## 验收用例

| 用例 | 输入 | 预期输出 |
|---|---|---|
| 正常注册 | `2` → `zhangsan` → `123456` | 注册成功并写入 user.txt |
| 重启后登录 | `1` → `zhangsan` → `123456` | 登录成功，四字段完整 |
| 用户已存在 | `2` → `zhangsan` → `654321` | `操作未完成：账号 zhangsan 已存在，请更换。` |
| 参数为空 | `2` → 空账号 → `123456` | `操作未完成：账号不能为空。` |
| 密码过短 | `2` → `lisi` → `abc12` | `操作未完成：密码长度不能少于 6 位。` |
| 用户不存在 | `1` → `wangwu` → `123456` | `操作未完成：账号 wangwu 尚未注册，请先注册。` |
| 密码错误 | `1` → `admin` → `111111` | `操作未完成：密码错误，请重新输入。` |
| 无效选项 | `3` | `无效选项，请输入 0、1 或 2。` |

上述八组均无 `Exception in thread`、无 `at campus.` 堆栈行，退出码为 0。

## 设计原则落地

**开闭原则**：新增业务错误类型时，只需在 `exception` 包下加一个子类并在抛出点换用，入口的 `catch (BusinessException e)` 不用改。反过来，如果入口写的是 `catch (UserExistsException e)` 这种按类型逐个列举的写法，每加一种错误都要改入口。

**统一异常出口**：业务异常与系统异常分开两层 `catch`，用户看到的中文提示来自异常的 `message`，技术细节（类名、行号、调用链）全部留在程序内部。

## 定稿后的工程加固（v1.4.1）

功能定稿后再做了一轮结构整理，业务行为与上面八组验收完全一致，只改"代码放哪儿、叫什么、异常往哪走"。

| 改动 | 改之前 | 改之后 | 对应课程概念 |
|---|---|---|---|
| 用户集合归属 | `SystemMain` 持有 `List<User>` 并自己遍历查找 | 抽 `UserStore`，集合与查询都在它手里 | 信息专家（GRASP）、低耦合 |
| 参数校验 | `SystemMain` 内部私有方法 | `Validator` 静态方法，命名 `requireXxx` | 单一职责 |
| 角色字面量 | `"admin"` 散落在注册、初始化、展示三处 | `Roles.ADMIN` / `Roles.STUDENT` | 消除魔法值 |
| 时间格式 | `SystemMain` 里的 `DateTimeFormatter` | `TimeUtil.now()` | 工具类封装重复逻辑 |
| 实体相等性 | 只有 `toString`，同名对象 `equals` 为 `false` | 按 `username` 实现 `equals` / `hashCode` | 对象相等性契约 |
| IO 失败 | `catch` 里打印一行，返回空集合继续跑 | 抛 `UncheckedIOException`，由入口兜底 | 异常不吞、系统故障与业务失败分离 |
| 方法命名 | `doLogin` / `doRegister` | `login` / `register` | 命名表达意图，`do` 前缀无信息量 |
| 字面量 | `"0"` / `"1"` / `"2"`、`"123456"` 直接写在逻辑里 | 统一为类常量 | 消除魔法值 |

**为什么这么改值得单独说明**：`FileUtil` 原先在 `catch` 里打印后返回空列表，程序会带着"0 个用户"继续运行——文件权限错误、磁盘满这类问题要等到用户注册失败才发现。改成抛异常后，入口第一次就能打印"系统异常，请稍后重试"，问题暴露在发生的那一刻，这是快速失败（fail-fast）的意义。

**没有做的事**：没有提前引入 `Result<T>` 统一返回（属于 V2.1），也没有拆 `service` / `dao` 分层（属于任务10）。加固只做"把已有代码放对位置"，不越过后续迭代的边界。

## 已知局限

- `catch (Exception e)` 目前只打印"系统异常，请稍后重试"，没有写日志。真实项目需要把堆栈落到日志文件，否则线上问题无从排查；本课程范围外。
- 密码长度不足暂用父类 `BusinessException` 表达，语义上不如独立子类精确；当前只有一条规则，等规则增多再拆分。

## 单元测试

本目录包含 27 条单元测试，使用纯 Java 断言实现，不引入第三方测试框架，源码位于 `test/campus/test/`：

- `UserTest` 5 条、`ValidatorTest` 9 条、`UserStoreTest` 8 条、`FileUtilTest` 5 条
- 基础设施：`Assert` 提供断言与失败信息，`TestRunner` 负责逐条执行、汇总并通过退出码返回结果

运行方式：双击 `test.bat`，或在该目录下执行 `test.bat`。测试在 `testtmp` 临时目录中运行，不会修改演示数据文件；全部通过时输出 `ALL UNIT TESTS PASSED`。
