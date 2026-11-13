package campus.test;

import campus.common.Result;

/**
 * 返回结果封装测试（V2.1）。
 *
 * Result 是一个「值对象」：构造私有、只能由四个工厂方法产生，
 * 成功必然带提示、失败必然无数据。测试把这些约定固定下来，
 * 以后新增工厂方法时才不会破坏调用方对字段含义的假设。
 */
public class ResultTest {

    public static void runAll(TestRunner runner) {
        runner.run("非字符串数据走泛型重载，提示为默认文案", () -> {
            Result<Integer> result = Result.success(Integer.valueOf(42));
            Assert.isTrue(result.isSuccess(), "应标记为成功");
            Assert.equals("操作成功", result.getMsg(), "无自定义提示时使用默认文案");
            Assert.equals(Integer.valueOf(42), result.getData(), "应携带业务数据");
        });

        runner.run("字符串实参优先匹配提示重载，不产生歧义编译错误", () -> {
            // 这是 V2.1 遗留的重载歧义：success(T) 与 success(String) 在 T=String 时都能接受字符串。
            // 编译器按「更具体的重载优先」选中 success(String)，因此字符串实参被当作提示而不是数据。
            // 现有调用方没有依赖这条路径，测试用它固定当前行为，避免以后有人误以为字符串会进 data。
            Result<String> result = Result.success("payload");
            Assert.isTrue(result.isSuccess(), "应标记为成功");
            Assert.equals("payload", result.getMsg(), "字符串实参被解析为提示");
            Assert.isTrue(result.getData() == null, "字符串实参不会进入 data");
        });

        runner.run("成功但无数据时 data 为 null", () -> {
            Result<Void> result = Result.success("已保存");
            Assert.isTrue(result.isSuccess(), "应标记为成功");
            Assert.equals("已保存", result.getMsg(), "应使用自定义提示");
            Assert.isTrue(result.getData() == null, "无数据时 data 应为 null");
        });

        runner.run("成功同时指定提示与数据", () -> {
            Result<String> result = Result.success("查询完成", "value");
            Assert.isTrue(result.isSuccess(), "应标记为成功");
            Assert.equals("查询完成", result.getMsg(), "应使用自定义提示");
            Assert.equals("value", result.getData(), "应携带数据");
        });

        runner.run("失败时无数据且成功标记为 false", () -> {
            Result<String> result = Result.fail("账号已存在");
            Assert.isFalse(result.isSuccess(), "失败应标记为 false");
            Assert.equals("账号已存在", result.getMsg(), "失败提示应原样保留");
            Assert.isTrue(result.getData() == null, "失败时不应携带数据");
        });

        runner.run("toString 反映三个字段", () -> {
            String text = Result.fail("出错了").toString();
            Assert.contains("success=false", text, "应包含成功标记");
            Assert.contains("出错了", text, "应包含提示文案");
        });
    }
}
