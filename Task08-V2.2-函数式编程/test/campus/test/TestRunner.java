package campus.test;

/**
 * 测试运行器。
 *
 * 每个用例是一个 Runnable：跑通了记一条 PASS，抛 AssertionError 或任何异常记一条 FAIL，
 * 全部跑完后打印汇总，失败数大于 0 时以非 0 退出码结束进程——这样 test.bat 能判断成败。
 */
public class TestRunner {

    private int passed;
    private int failed;

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        System.out.println("================ 单元测试开始 ================");

        ResourceStoreTest.runAll(runner);
        ResourceManagerTest.runAll(runner);

        System.out.println("================ 单元测试结束 ================");
        System.out.println("通过：" + runner.passed + "    失败：" + runner.failed
                + "    合计：" + (runner.passed + runner.failed));
        if (runner.failed > 0) {
            System.exit(1);
        }
    }

    /** 执行一条用例 */
    public void run(String name, Runnable body) {
        try {
            body.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable error) {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
        }
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }
}
