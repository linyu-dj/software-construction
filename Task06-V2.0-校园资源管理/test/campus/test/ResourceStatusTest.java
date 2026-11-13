package campus.test;

import campus.entity.ResourceStatus;

/**
 * 资源状态枚举测试（V2.0）。
 *
 * 状态是封闭集合，落盘存枚举名、界面显示中文标签，两者必须能对上。
 * 解析失败返回 null 而不是抛异常，是给读取方的「跳过脏行」留出处理空间，
 * 这一点也纳入测试，避免以后有人把返回 null 改成抛异常而悄悄改变加载策略。
 */
public class ResourceStatusTest {

    public static void runAll(TestRunner runner) {
        runner.run("三种状态的显示标签正确", () -> {
            Assert.equals("可预约", ResourceStatus.AVAILABLE.getLabel(), "AVAILABLE 显示为可预约");
            Assert.equals("占用", ResourceStatus.OCCUPIED.getLabel(), "OCCUPIED 显示为占用");
            Assert.equals("停用", ResourceStatus.DISABLED.getLabel(), "DISABLED 显示为停用");
        });

        runner.run("toString 输出中文标签", () -> {
            Assert.equals("占用", ResourceStatus.OCCUPIED.toString(), "表格打印应显示中文");
        });

        runner.run("按枚举名解析成功", () -> {
            Assert.equals(ResourceStatus.AVAILABLE, ResourceStatus.parse("AVAILABLE"), "应解析出可预约");
            Assert.equals(ResourceStatus.DISABLED, ResourceStatus.parse("DISABLED"), "应解析出停用");
        });

        runner.run("解析时忽略首尾空格", () -> {
            Assert.equals(ResourceStatus.OCCUPIED, ResourceStatus.parse(" OCCUPIED "), "空格不应影响解析");
        });

        runner.run("非法值返回 null", () -> {
            Assert.isTrue(ResourceStatus.parse("可预约") == null, "中文标签不是枚举名，应返回 null");
            Assert.isTrue(ResourceStatus.parse("UNKNOWN") == null, "未知状态应返回 null");
            Assert.isTrue(ResourceStatus.parse(null) == null, "null 应返回 null");
        });

        runner.run("枚举共三种取值", () -> {
            Assert.equals(3, ResourceStatus.values().length, "状态集合应保持三态");
        });
    }
}
