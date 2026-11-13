# Task05 · V1.4 自定义异常

## 本任务目标

把"业务失败靠返回字符串"改成"业务失败抛异常"，并在程序入口统一捕获，只输出中文提示，不向用户暴露堆栈。

## 目录结构

```
Task05-V1.4-自定义异常/
├─ src/
│  └─ campus/
│     ├─ SystemMain.java            主类：统一捕获，两级异常处理
│     ├─ entity/
│     │  └─ User.java               实体类
│     ├─ utils/
│     │  └─ FileUtil.java           文件读写
│     └─ exception/
│        ├─ BusinessException.java            业务异常父类
│        ├─ UserExistsException.java          用户已存在
│        ├─ UserNotFoundException.java        用户不存在
│        ├─ WrongPasswordException.java       密码错误
│        └─ EmptyParamException.java          参数为空
├─ user.txt                         运行时自动生成（已 gitignore）
└─ run.bat
```

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
| `EmptyParamException` | `validateUsername` / `validatePassword` | 账号或密码为空、账号含 `|` |
| `UserExistsException` | `validateUsername`（注册场景） | 账号已被占用 |
| `UserNotFoundException` | `doLogin` | 用户名在内存集合中查不到 |
| `WrongPasswordException` | `doLogin` | 密码与记录不一致 |
| `BusinessException`（父类） | `validatePassword`（注册场景） | 密码长度不足 6 位 |

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

## 已知局限

- `catch (Exception e)` 目前只打印"系统异常，请稍后重试"，没有写日志。真实项目需要把堆栈落到日志文件，否则线上问题无从排查；本课程范围外。
- 密码长度不足暂用父类 `BusinessException` 表达，语义上不如独立子类精确；当前只有一条规则，等规则增多再拆分。
