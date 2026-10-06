/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.spi.verify;

import cube.service.aigc.spi.ModuleRegistry;
import cube.service.psychology.PsychologyStorage;
import cube.service.psychology.scene.PsychologyScene;

import java.io.File;

/**
 * 场景存储可达性只读验证。
 *
 * <p>验证「模块装配后场景确实可访问存储」这一核心判据：场景未装配时，
 * {@code storage} 与 {@code host} 恒为 null，41 处存储访问全部会抛
 * 空引用异常并被动作执行层 catch 成无信息的 {@code Failure}。</p>
 *
 * <p><b>严格只读</b>：只调用读取方法，不写入、不改配置。</p>
 */
public class SceneStorageVerify {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println(" 场景存储可达性验证（工作目录必须为 service/）");
        System.out.println("======================================================================");

        File configDir = new File("config");
        assertTrue("G0 工作目录下存在 config/ 目录", configDir.isDirectory());

        // 真实装载：走ModuleRegistry.load() → PsychologyModule.setup()
        Object host = newNoopHost();
        ModuleRegistry registry = newRegistry(host);
        int count = registry.load();
        assertTrue("G1 装载到 1 个模块", 1 == count);
        assertTrue("G1 无阻塞失败", !registry.hasBlockingFailure());

        // ── 核心判据 ──
        PsychologyScene scene = PsychologyScene.getInstance();

        assertTrue("G2 场景 isReady（host 与 storage 均已注入）", scene.isReady());

        // 直接触碰存储：这一行在修复前必然抛 NullPointerException
        int count2 = scene.numPaintingReports(0L, 0);
        assertTrue("G3 场景可访问存储（numPaintingReports 未抛空引用）", count2 >= 0);

        PsychologyStorage storage = scene.getStorage();
        assertTrue("G4 getStorage() 返回非 null", null != storage);

        // 存储与模块持有的是同一个实例
        cube.service.psychology.PsychologyModule module =
                (cube.service.psychology.PsychologyModule) registry.findModule("psychology");
        assertTrue("G5 场景与模块持有同一存储实例",
                storage == module.getStorageForTest());

        // 队列上限取自配置（psychology.local.json 的 preference 段）
        assertTrue("G6 队列上限 = 20（配置值，非代码默认 30）",
                20 == scene.getMaxQueueLength());

        // ── 卸载后应回到未装配 ──
        registry.teardownAll();
        assertTrue("G7 卸载后场景回到未装配", !scene.isReady());
        assertTrue("G7 卸载后场景不再持有存储", null == scene.getStorage());

        System.out.println("======================================================================");
        System.out.println(" 通过 " + passed + " / 失败 " + failed);
        System.exit(0 == failed ? 0 : 1);
    }

    private static ModuleRegistry newRegistry(Object host) throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new cube.aigc.spi.ActionRouter());
        registry.setHost((cube.aigc.spi.AIGCHost) host);
        return registry;
    }

    private static Object newNoopHost() throws Exception {
        Class<?> clazz = Class.forName(
                "cube.service.aigc.spi.test.ModuleRegistryLoadTest$NoopHost");
        java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + message);
        }
        else {
            failed++;
            System.out.println("  [FAIL] " + message);
        }
    }
}
