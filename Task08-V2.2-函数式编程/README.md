# Task08 · V2.2 Stream + Optional

## 本任务目标

把资源集合的查询全部换成 Stream + Lambda，按编号查找改成 `Optional`，消掉手写 for 循环和「返回 null」的写法；新增名称模糊查询。业务规则、持久化、返回结构一律不动。

## 改动范围

| 文件 | 改动 |
|---|---|
| `store/ResourceStore.java` | 全部查询改 Stream，`findById` 返回 `Optional`，新增 `findByStatus` / `findByNameKeyword` / `search` |
| `biz/ResourceManager.java` | 查询改用 Stream 结果，`findById` / `update` / `delete` 用 `orElseThrow` |
| `SystemMain.java` | 管理员菜单加「按名称查询」，学生菜单加「按名称查询」 |
| 其他 | 未改动 |

## 菜单变化

```
管理员：[1]新增 [2]查询全部 [3]按名称查询 [4]修改 [5]删除 [0]退出
学生  ：[1]查询可预约 [2]按名称查询 [0]退出
```

编号有调整（修改/删除从 3/4 变成 4/5），因为中间插了查询项。

## Stream 改造点

### 1. 按编号查找 → Optional

```java
public Optional<CampusResource> findById(String id) {
    if (id == null) {
        return Optional.empty();
    }
    return resources.stream()
            .filter(resource -> resource.getId().equals(id))
            .findFirst();
}
```

旧写法是循环里 `return resource`，末尾 `return null`。问题不在循环，在返回值可能是 null，调用方漏判就是空指针。现在「可能找不到」写在类型里，编译器会提醒。

调用方：

```java
CampusResource resource = store.findById(id.trim())
        .orElseThrow(() -> new ResourceNotFoundException("未找到编号为 " + id + " 的资源。"));
```

### 2. 按状态筛选

```java
return resources.stream()
        .filter(resource -> resource.getStatus() == status)
        .sorted(Comparator.comparing(CampusResource::getId))
        .collect(Collectors.toList());
```

旧写法是 `new ArrayList` + 循环判断 + `add`，五行。现在三行，且「筛选」和「排序」各占一行，读起来是两件事。

### 3. 名称模糊查询

```java
String normalized = normalizeKeyword(keyword);
return resources.stream()
        .filter(resource -> resource.getName().toLowerCase(Locale.ROOT).contains(normalized))
        .sorted(Comparator.comparing(CampusResource::getId))
        .collect(Collectors.toList());
```

`normalizeKeyword` 把 null 当空串、去首尾空格、转小写。空关键词等于「全部」，这样界面上留空也能用。

用 `Locale.ROOT` 而不是无参 `toLowerCase()`：后者受默认区域设置影响，土耳其语环境下 `I` 会转成 `ı`，中文项目里虽然碰不到，但这是个确定性的写法。

### 4. 多条件组合查询

```java
return resources.stream()
        .filter(resource -> status == null || resource.getStatus() == status)
        .filter(resource -> normalized.isEmpty()
                || resource.getName().toLowerCase(Locale.ROOT).contains(normalized))
        .sorted(Comparator.comparing(CampusResource::getId))
        .collect(Collectors.toList());
```

两行 filter 就是两个条件，条件为空表示「不限」。比在循环里写三层 `if` 清楚。

### 5. 编号生成

```java
int max = resources.stream()
        .mapToInt(resource -> parseSequence(resource.getId()))
        .max()
        .orElse(0);
```

`max()` 返回 `OptionalInt`，空集合时 `orElse(0)` 给出默认值——这就是 Optional 的另一个用法：不是为了防空指针，而是为了让「空集合」有明确处理。

## 旧写法保留了吗

保留了，写成注释放在每个方法上方。任务书要求「可注释保留旧 for 循环版本，方便对比」。这样做的好处是讲代码时可以直接指着说「以前这么写，现在这么写」。

## 验收

15 组场景，其中 4 组是这一轮新增的查询场景：

| 用例 | 输入 | 结果 |
|---|---|---|
| 管理员搜「会议」 | `3` → `会议` | 命中 R001，共 1 条 |
| 学生搜「实验」 | `2` → `实验` | 0 条（R002 已占用，学生不可见） |
| 学生搜「投影」 | `2` → `投影` | 命中 R003，共 1 条 |
| 管理员搜「室」 | `3` → `室` | 命中 R001 / R002 / R004，共 3 条 |

其余 11 组是 V2.1 的回归：首启、增删改、权限过滤、重启加载、各类报错提示，输出与上一版一致。

## 设计原则落地

**函数式范式**：查询只描述「筛选什么、按什么排序」，不写怎么迭代。
**单一职责**：`ResourceStore` 只做集合查询，不碰业务判断与文件。
**防御式编程**：`Optional` 把「可能没有」显式化，业务层用 `orElseThrow` 一次性处理空值。

## 已知局限

- `search` 同时承担「按状态」和「按名称」两种筛选，参数可空表示不限，语义上依赖调用方约定。等任务 10 分层之后可以拆成两个明确的查询方法。
- 模糊查询只匹配名称，没有匹配类型或位置，任务书也没要求。
