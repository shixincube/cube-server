/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.test.spi;

import cube.aigc.spi.ActionBinding;
import cube.aigc.spi.ActionContext;
import cube.aigc.spi.ActionModule;
import cube.aigc.spi.ActionRouter;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.ModuleDescriptor;
import cube.aigc.spi.ModuleException;
import cube.common.state.AIGCStateCode;

import java.util.Collections;
import java.util.List;

/**
 * 动作路由器策略验证。
 *
 * <p>本工程无测试框架，故用main 方法直接运行，退出码 0 表示全部通过：</p>
 * <pre>
 * java -cp out/p0/common-classes:&lt;dependencies&gt;/* cube.test.spi.ActionRouterConflictTest
 * </pre>
 */
public final class ActionRouterConflictTest {

    /**
     * 失败计数。
     */
    private static int failures = 0;

    private ActionRouterConflictTest() {
    }

    /**
     * 占位模块：仅用于验证绑定归属。
     */
    private static final class StubModule implements ActionModule {

        private final String name;

        private StubModule(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor(this.name, "0.0.1", 1,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(),
                    1000L, true);
        }

        @Override
        public void setup(AIGCHost host) throws ModuleException {
        }

        @Override
        public void teardown() {
        }

        @Override
        public List<ActionBinding> getActions() {
            return Collections.emptyList();
        }
    }

    /**
     * 构造一个占位动作绑定。
     *
     * @param action 动作名。
     * @return 返回动作绑定。
     */
    private static ActionBinding bind(String action) {
        return new ActionBinding(action, new AIGCActionTask() {
            @Override
            public AIGCStateCode handle(ActionContext ctx) {
                return AIGCStateCode.Ok;
            }
        });
    }

    /**
     * 记录一项检查结果。
     *
     * @param condition 检查条件。
     * @param message 检查描述。
     */
    private static void check(boolean condition, String message) {
        if (condition) {
            System.out.println("[PASS] " + message);
        } else {
            ++failures;
            System.out.println("[FAIL] " + message);
        }
    }

    /**
     * 入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        ActionRouter router = new ActionRouter();
        ActionModule moduleA = new StubModule("moduleA");
        ActionModule moduleB = new StubModule("moduleB");

        // ① 冲突拒绝覆盖
        check(router.bind(bind("alphaAction"), moduleA), "first bind of \"alphaAction\" succeeds");
        check(!router.bind(bind("alphaAction"), moduleB), "second bind of \"alphaAction\" is REJECTED");
        check(router.lookup("alphaAction").getOwner() == moduleA,
                "the first binder keeps the ownership (no hijack)");
        check(router.size() == 1, "router size stays 1 after the conflicting bind");

        // ② 宿主保留名拒绝
        check(!router.bind(bind("checkToken"), moduleA), "reserved action \"checkToken\" is REJECTED");
        check(!router.bind(bind("event"), moduleA), "reserved action \"event\" is REJECTED");
        check(!router.bind(bind("setup"), moduleA), "reserved action \"setup\" is REJECTED");
        check(!router.bind(bind("teardown"), moduleA), "reserved action \"teardown\" is REJECTED");
        check(!router.bind(null, moduleA), "NULL binding is REJECTED");
        check(!router.bind(new ActionBinding("", bind("x").task), moduleA), "empty action name is REJECTED");

        // ③ 精确匹配（大小写敏感，与线协议一致）
        check(!router.isBound("AlphaAction"), "lookup is case-sensitive");
        check(router.lookup(null) == null, "lookup(NULL) returns NULL instead of throwing");

        // ④ unbindAll 精确性
        check(router.bind(bind("betaAction"), moduleA), "moduleA binds \"betaAction\"");
        check(router.bind(bind("gammaAction"), moduleB), "moduleB binds \"gammaAction\"");
        check(router.size() == 3, "router size is 3 before unbind");

        check(router.unbindAll(moduleA), "unbindAll(moduleA) reports success");
        check(router.size() == 1, "only moduleA's bindings are removed");
        check(router.isBound("gammaAction"), "moduleB's binding survives moduleA's unbindAll");
        check(!router.unbindAll(new StubModule("ghost")), "unbindAll on an unbound module reports failure");

        // ⑤ 注销后可重新绑定（验证双参remove 的 CAS 语义不留下死角）
        check(router.bind(bind("alphaAction"), moduleB), "\"alphaAction\" can be re-bound after unbindAll");

        System.out.println(failures == 0 ? "ALL PASSED" : (failures + " CHECK(S) FAILED"));
        System.exit(failures == 0 ? 0 : 1);
    }
}
