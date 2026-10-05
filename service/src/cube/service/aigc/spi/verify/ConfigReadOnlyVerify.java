/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.spi.verify;

import cube.aigc.spi.AIGCSPI;
import cube.aigc.spi.ActionRouter;
import cube.util.ConfigUtils;
import cube.service.aigc.spi.ModuleRegistry;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * 配置恢复只读验证。
 *
 * <p>复用真实的 {@link ModuleRegistry#load()} 与真实的
 * {@link ConfigUtils#readJsonFile(String)}，在工作目录 = service/ 下验证
 * service/config/aigc-modules.properties 与 service/config/psychology.local.json
 * 能被程序正常读取。</p>
 *
 * <p><b>本类严格只读</b>：不写入、不修改任何配置文件，绝不调用
 * {@code FileOutputStream}。这是它与 ModuleRegistryLoadTest /
 * PsychologyPluginTest 的根本区别——后两者会用测试桩内容覆写
 * config/ 下的真实配置，只能在临时目录里跑。</p>
 */
public class ConfigReadOnlyVerify {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println(" 配置恢复只读验证（工作目录必须为service/）");
        System.out.println("======================================================================");
        System.out.println("user.dir = " + new File(".").getAbsolutePath());
        System.out.println();

        // ---------- 前置：工作目录正确性 ----------
        File configDir = new File("config");
        assertTrue("V0 工作目录下存在 config/ 目录", configDir.isDirectory());

        // ---------- 1. aigc-modules.properties 物理存在 ----------
        File modulesFile = new File("config/aigc-modules.properties");
        assertTrue("V1 config/aigc-modules.properties 存在", modulesFile.isFile());
        assertTrue("V1 文件非空", modulesFile.length() > 0);

        // ---------- 2. 用程序真实使用的 ConfigUtils 解析 ----------
        Properties properties = ConfigUtils.readProperties(modulesFile.getAbsolutePath());
        assertTrue("V2 ConfigUtils 解析出属性表", null != properties);
        assertTrue("V2 解析出 module.1.class",
                null != properties.getProperty("module.1.class"));
        assertTrue("V2 module.1.class 值正确",
                "cube.service.psychology.PsychologyModule"
                        .equals(properties.getProperty("module.1.class").trim()));
        assertTrue("V2 module.1.enabled = true",
                "true".equalsIgnoreCase(
                        properties.getProperty("module.1.enabled", "").trim()));

        // ---------- 3. ModuleRegistry 真实装载（会走 setup 读 local 配置） ----------
        Object host = newNoopHost();
        ModuleRegistry registry = newRegistry(host);
        int count = registry.load();
        System.out.println();
        System.out.println("  ModuleRegistry.load() 返回已装载模块数 = " + count);
        System.out.println("  路由绑定动作数 = " + registry.getRouter().size());
        System.out.println();

        assertTrue("V3 装载到 1 个模块（未 Loaded 0 即配置缺失）", 1 == count);
        assertTrue("V3 模块按名可查 = psychology", null != registry.findModule("psychology"));
        assertTrue("V3 路由已标记装载完成", registry.getRouter().isLoaded());
        assertTrue("V3 动作已绑定（>=20）", registry.getRouter().size() >= 20);
        assertTrue("V3 无阻塞失败", !registry.hasBlockingFailure());

        // ---------- 4. psychology.local.json 独立读取校验 ----------
        // 与 PsychologyModule#readConfig 同一查找顺序：local 优先，template 兜底
        JSONObject local = ConfigUtils.readJsonFile("psychology.local.json");
        JSONObject template = ConfigUtils.readJsonFile("psychology.json.template");
        boolean fromLocal = (null != local);
        JSONObject config = fromLocal ? local : template;
        assertTrue("V4 读到心理学数据库配置（local 或 template）", null != config);
        assertTrue("V4 实际生效的是 psychology.local.json", fromLocal);

        JSONObject storage = config.getJSONObject("storage");
        assertTrue("V4 存在 storage 段", null != storage);
        assertTrue("V4 storage.type = MySQL",
                "MySQL".equals(storage.getString("type")));
        assertTrue("V4 storage.host 非空", null != storage.getString("host"));
        assertTrue("V4 storage.host 不是未定制的占位符",
                !storage.getString("host").startsWith("<your-"));
        assertTrue("V4 storage.port = 3306",
                3306 == storage.getInt("port"));
        assertTrue("V4 storage.schema 非空", null != storage.getString("schema"));
        assertTrue("V4 storage.user 非空", null != storage.getString("user"));
        assertTrue("V4 storage.password 非空（不打印明文）",
                null != storage.getString("password"));

        JSONObject preference = config.getJSONObject("preference");
        assertTrue("V5 存在 preference 段", null != preference);
        assertTrue("V5 preference.maxQueueLength = 20",
                20 == preference.getInt("maxQueueLength"));

        JSONObject unit = config.getJSONObject("unit");
        assertTrue("V5 存在 unit 段", null != unit);
        assertTrue("V5 unit.name 非空", null != unit.getString("name"));
        assertTrue("V5 unit.contextLength = 5000",
                5000 == unit.getInt("contextLength"));

        // ---------- 5. SPI 版本与模块声明 ----------
        assertTrue("V6 SPI 版本 >= 7", AIGCSPI.VERSION >= 7);
        System.out.println("  AIGCSPI.VERSION = " + AIGCSPI.VERSION);
        System.out.println();

        // ---------- 汇总 ----------
        System.out.println("======================================================================");
        System.out.println(" 通过 " + passed + " / 失败 " + failed);
        System.out.println("======================================================================");
        System.exit(0 == failed ? 0 : 1);
    }

    /**
     * 构造 ModuleRegistry 并注入 host，与 ModuleRegistryLoadTest#newRegistry 同构。
     */
    private static ModuleRegistry newRegistry(Object host) throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new ActionRouter());
        registry.setHost((cube.aigc.spi.AIGCHost) host);
        return registry;
    }

    /**
     * 复用测试包里的 NoopHost（已编译），避免重复实现 51 个 SPI 方法。
     */
    private static Object newNoopHost() throws Exception {
        Class<?> clazz = Class.forName(
                "cube.service.aigc.spi.test.ModuleRegistryLoadTest$NoopHost");
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + message);
        } else {
            failed++;
            System.out.println("  [FAIL] " + message);
        }
    }
}