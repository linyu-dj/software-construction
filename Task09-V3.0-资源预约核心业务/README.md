# Task09 · V3.0 资源预约核心业务

## 本任务目标

把「资源」和「用户」两条线串起来，做出第一个真正的业务闭环：

学生选一个可预约资源、填一段起止时间提交申请 → 管理员审核 → 通过则资源置为占用 → 学生可以取消 → 资源回到可预约。

这一轮新增的四类事实（预约记录、预约状态、时间冲突、审核联动）都要落到 `reserve.txt`，重启后能还原。所有业务方法继续返回 `Result`，查询继续用 Stream + Optional。

## 目录结构

```
Task09-V3.0-资源预约核心业务/
├── src/campus/
│   ├── SystemMain.java                    控制台菜单与输入输出
│   ├── biz/
│   │   ├── UserManager.java               注册、登录、按账号查询
│   │   ├── ResourceManager.java           资源增删改查（多了一个 ReserveStore 依赖）
│   │   └── ReserveManager.java            提交、我的预约、取消、审核、条件查询
│   ├── entity/
│   │   ├── User.java / CampusResource.java
│   │   ├── ResourceStatus.java            AVAILABLE / OCCUPIED / DISABLED
│   │   ├── ReserveRecord.java             预约记录实体，8 个字段
│   │   └── ReserveStatus.java             PENDING / PASS / REJECT / CANCEL
│   ├── store/
│   │   ├── UserStore.java / ResourceStore.java
│   │   └── ReserveStore.java              预约集合查询、编号生成
│   ├── common/Result.java / Roles.java
│   ├── exception/                         11 个自定义异常
│   └── utils/
│       ├── FileUtil.java                  user.txt / resource.txt / reserve.txt
│       ├── TimeUtil.java                  两种时间格式与解析
│       └── Validator.java                 参数与角色校验
├── test/campus/test/
│   ├── Assert.java                        自建断言
│   ├── TestRunner.java                    用例登记与汇总
│   ├── UserManagerTest.java               8 条
│   ├── ResourceManagerTest.java           13 条
│   └── ReserveManagerTest.java            20 条
├── run.bat                                编译并运行主程序
└── test.bat                               编译并运行单元测试
```

源文件 27 个，测试文件 5 个。

## 数据文件格式

`reserve.txt` 一行一条记录，8 个字段：

```
recordId|resourceId|applicantUsername|applyTime|reserveStartTime|reserveEndTime|status|remark
```

示例：

```
B001|R001|student|2026-11-13 18:36:47|2026-11-20 09:00:00|2026-11-20 11:00:00|CANCEL|部门例会
```

- 三个时间统一用 `yyyy-MM-dd HH:mm:ss` 落盘，界面上按 `yyyy-MM-dd HH:mm` 输入与显示。
- `status` 存枚举的 `name()`（PENDING / PASS / REJECT / CANCEL），显示时用中文 label。
- 读取时状态或任一时间解析不出来，整行跳过，不猜默认值。
- 备注可以为空，但内部的 `|` 会被校验器挡住，避免破坏分隔格式。

## 菜单

```
游客  ：[1] 登录   [2] 注册   [0] 退出
管理员：[1] 新增资源 [2] 查询全部资源 [3] 按名称查询 [4] 修改资源 [5] 删除资源
        [6] 查询全部预约 [7] 审核预约 [8] 条件查询预约 [0] 退出
学生  ：[1] 查询可预约资源 [2] 按名称查询 [3] 提交预约 [4] 我的预约 [5] 取消预约 [0] 退出
```

## 核心设计

### 1. 预约状态用枚举，把「终态」写进代码

```java
public enum ReserveStatus {
    PENDING("待审核"), PASS("已通过"), REJECT("已驳回"), CANCEL("已取消");

    public boolean isFinal() {
        return this == REJECT || this == CANCEL;
    }
}
```

取消预约的判断从「不等于 PENDING 且不等于 PASS」变成一句 `isFinal()`。以后再加状态（比如「已过期」）只需要改这个枚举，`cancel` 不用动——这就是开闭原则落在代码上的样子。

### 2. 时间冲突只算「已通过」的预约

```java
boolean overlaps = record.getReserveStartTime().isBefore(endTime)
        && startTime.isBefore(record.getReserveEndTime());
```

条件写成 `start < otherEnd && otherStart < end`，首尾相接（9:00–11:00 与 11:00–13:00）不算冲突。如果写成闭区间比较，正常的两段相邻预约会被判成冲突，学生会觉得系统不讲理。

为什么只比 PASS：待审核和已驳回的记录还没占用资源，把它们也算进来，学生就会互相卡住——一个没被批的申请挡住另一个申请，管理员根本没得选。

### 3. 审核通过要同时改两张表

```java
resource.setStatus(ResourceStatus.OCCUPIED);
record.setStatus(ReserveStatus.PASS);
FileUtil.saveReserves(reserveStore.snapshot());
FileUtil.saveResources(resourceStore.snapshot());
```

资源状态和预约状态必须一起改，任何一边漏掉，重启后就会出现「记录已通过但资源还显示可预约」。这一轮先靠「两个 save 紧挨着写」保证一致性，事务性没有更强的手段——任务 19 会做配置驱动的多存储，那时这个话题会再出现一次。

### 4. 取消已通过的预约，资源要放回来

```java
boolean wasPass = record.getStatus() == ReserveStatus.PASS;
record.setStatus(ReserveStatus.CANCEL);
FileUtil.saveReserves(reserveStore.snapshot());
if (wasPass) {
    restoreResource(record.getResourceId());
}
```

只有原本是 PASS 的才恢复资源。如果取消一条待审核的申请也去恢复资源，会误把别的预约占用的资源改成可预约。

`restoreResource` 里还有一层 `ifPresent` + 状态判断：

```java
resourceStore.findById(resourceId).ifPresent(resource -> {
    if (resource.getStatus() == ResourceStatus.OCCUPIED) {
        resource.setStatus(ResourceStatus.AVAILABLE);
        ...
```

资源可能已经被管理员删掉（只有 PASS 才禁止删，但驳回后删掉是允许的），所以找不到就跳过，不能让一条取消操作把系统搞崩。

### 5. 删除校验从「留钩子」换成真实查询

V2.0 的 `hasBookingRecord()` 是个恒返回 `false` 的占位方法。这一轮把它删掉，直接问预约存储：

```java
if (reserveStore.hasPassReservation(resource.getId())) {
    throw new BusinessException("资源 " + trimmedId
            + " 存在已通过（PASS）的预约记录，禁止删除。请先处理该预约。");
}
```

这条规则和任务书一致，也解释了为什么删资源要连预约数据一起看：资源编号会出现在预约记录里，删掉资源后那些记录就成了悬空引用。

## 单元测试

老师要求写单元测试，但课程限定「不引第三方框架」，所以没有用 JUnit，自己写了三样东西：

- `Assert`：`isTrue` / `isFalse` / `equals` / `contains` / `notNull` / `fail`，失败抛 `AssertionError`。
- `TestRunner`：`run(名称, Runnable)` 把每条用例包起来，异常记一条 FAIL，最后打印通过数与失败数，有失败就以退出码 1 结束。
- 三个业务测试类：按「正常路径 + 边界 + 越权 + 错误输入」四类覆盖。

运行方式：双击 `test.bat`，或者命令行 `test.bat`。输出形如：

```
[PASS] 提交成功生成待审核记录
[PASS] 与已通过预约重叠被拒绝
[PASS] 首尾相接不算冲突
...
通过：41    失败：0    合计：41
ALL UNIT TESTS PASSED
```

41 条用例的分布：

| 测试类 | 条数 | 覆盖内容 |
|---|---|---|
| `UserManagerTest` | 8 | 注册成功、重复账号、密码过短、账号为空、登录成功、密码错误、账号不存在、按账号查询 |
| `ResourceManagerTest` | 13 | 学生越权新增/修改、编号顺延、空名称、学生只看可预约、学生查占用资源被拒、状态修改生效、有 PASS 禁止删、有 PENDING 可删、删除不存在、名称模糊查询 |
| `ReserveManagerTest` | 20 | 时间颠倒、结束早于当前、格式错误、资源不存在、资源不可预约、管理员代提交、提交成功、时段重叠、首尾相接、只查本人、越权取消、驳回后不能取消、取消 PASS 恢复资源、审核通过置占用、驳回不动资源、重复审核、学生审核、条件查询过滤、取消不存在记录 |

两个设计上的注意点：

**测试不碰正式数据。** `test.bat` 先切到 `testtmp/` 目录再运行，业务方法里那些 `FileUtil.appendUser` / `saveReserve` 写的是这个临时目录下的文件，跑完不会污染正式的 `user.txt` / `resource.txt` / `reserve.txt`。

**每条用例自己造数据。** 不让用例之间共享 store，避免「第 3 条失败导致后面 8 条连锁失败」。代价是每个 lambda 里都要 new 一次 store 和 manager，读起来略啰嗦，但定位问题快。

## 验收

24 组场景，按业务顺序走一遍完整闭环：

| 组 | 操作 | 期望 |
|---|---|---|
| A | 首次启动 | 3 个示例资源，0 条预约记录 |
| B | 学生提交 R001 09:00–11:00 | B001，待审核 |
| C | 再提 R001 10:00–12:00 | B002，待审核（此时没有 PASS，允许） |
| D | 管理员查全部预约 | 2 条 |
| E | 审核通过 B001 | R001 变为占用 |
| F | 驳回 B002 | 记录变已驳回，资源不动 |
| G | 学生查看我的预约 | 见「已通过」「已驳回」各一条 |
| H | 取消已驳回的 B002 | 提示不能取消 |
| I | 取消已通过的 B001 | 记录变已取消，R001 恢复可预约 |
| J | 管理员查资源列表 | R001 已恢复 |
| K | 学生提交 R002 14:00–16:00 | B003，待审核 |
| L | 审核通过 B003 | R002 变占用 |
| M | 管理员手动把 R002 改回可预约 | 修改成功 |
| N | 学生再提 R002 15:00–17:00 | 提示时间段冲突（关键用例） |
| O | 条件查询：状态已通过 | 1 条，B003 |
| P | 删除 R002 | 禁止删除（存在 PASS 预约） |
| Q | 取消 B003 | 资源恢复可预约 |
| R | 再删 R002 | 删除成功 |
| S | 重启 | 加载 3 条预约记录 |
| T | 起止时间颠倒 | 提示开始必须早于结束 |
| U | 结束时间填到过去 | 提示必须晚于当前时间 |
| V | 时间格式写成 `2026/11/20` | 提示格式不正确 |
| W | 提交 R999 | 提示未找到该资源 |
| X | 学生按名称查询 | 仍正常返回可预约资源 |

M 和 N 是一对：真实使用中，资源被置为占用后学生根本提交不了，冲突校验看起来「用不上」。但只要管理员手动把资源改回可预约（比如退租、维修结束），冲突校验就是最后一道防线。这组用例就是照这个路径设计的。

最终 `reserve.txt`：

```
B001|R001|student|2026-11-13 18:36:47|2026-11-20 09:00:00|2026-11-20 11:00:00|CANCEL|部门例会
B002|R001|student|2026-11-13 18:36:48|2026-11-20 10:00:00|2026-11-20 12:00:00|REJECT|重叠时段
B003|R002|student|2026-11-13 18:36:56|2026-11-25 14:00:00|2026-11-25 16:00:00|CANCEL|
```

取消的记录不会被删除，状态改成 CANCEL 留在文件里——预约是有审计价值的数据，删了就看不出「这个时段曾经被申请过」。

## 设计原则落点

| 原则 | 落点 |
|---|---|
| 信息专家 | `ReserveStore` 持有记录集合，所以冲突查询、条件查询、编号生成都在它这里 |
| 单一职责 | `ReserveManager` 只管预约规则，不读文件、不打印 |
| 开闭原则 | 新增异常子类（`TimeConflictException` 等）不改任何入口的 try/catch |
| 防御式编程 | 入口先 `requireStudent` / `requireAdmin`，时间先判格式再判先后 |
| 高内聚 | 状态流转集中在 `ReserveStatus.isFinal()` 和 `audit` / `cancel` 两个方法 |
| 差的设计（有意保留） | `ResourceManager` 直接依赖 `ReserveStore`，跨了两个业务域，任务 10 拆 |

## 已知局限

- `ReserveManager` 里有 5 个方法重复写 `try / catch (BusinessException e) { return Result.fail(...) }`，一共 5 处。任务 10 分层后会用统一的结果装配方式收掉。
- 一次操作要落两张表（审核通过、取消已通过），中途 IO 失败会出现「内存改了、文件没改成」。当前没有事务与回滚，只在方法内保证两次写的顺序。
- 冲突校验只比 PASS 记录，所以两条待审核的重叠申请可以并存。管理员审批时如果两条都点通过，会出现两段互相重叠的 PASS。这一轮没加「审批时二次校验」，留到状态机那一轮一起处理。
- 条件查询里资源编号要求完整匹配，申请人支持模糊匹配，两者口径不一致，界面上没有提示。
- `run.bat` 曾经有个坑：`javac` 用系统默认编码读 `@sources.txt`，目录名带中文时会解成乱码报「找不到文件」。现在参数文件里只写 `src\campus\...` 这样的相对路径，绝对路径前缀在批处理里被剥掉，与中文目录无关了。

## 怎么跑

```
run.bat     # 编译 + 运行主程序
test.bat    # 编译 + 跑 41 条单元测试
```
