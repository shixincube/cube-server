/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.spi;

import cell.util.log.Logger;
import cube.aigc.spi.ActionBinding;
import cube.aigc.spi.ActionModule;
import cube.aigc.spi.ActionRouter;
import cube.aigc.spi.AIGCSPI;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.ModuleDescriptor;
import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

/**
 * 业务模块注册表。
 *
 * <p>持有唯一的 {@link ActionRouter} 实例，负责：读取模块清单 → 实例化 →
 * 校验 {@link ModuleDescriptor#spiVersion} → 建立动作绑定 → 后续调用
 * {@link ActionModule#setup(AIGCHost)}。</p>
 *
 * <p><b>模块发现方式</b>：显式列出模块全限定类名
 * （<code>aigc-modules.properties</code>）。选择理由：本工程为 JDK 8 + ant +
 * 手写启动脚本，无依赖解析机制；按类路径顺序自动发现会引入「同名类被遮蔽」的
 * 调试成本，且模块启停需要运维开关，配置文件天然承载该职责。</p>
 *
 * <p><b>装载时序</b>：本类由 {@code AIGCCellet#install()} 调用。
 * {@code install()} 早于内核启动，也早于任何一次动作派发，
 * 因此装载完成时路由表已权威。这也是<b>不能</b>把装载放进
 * {@code AIGCService#start()} 的原因：那样会留下「派发已可用但路由表仍空」的窗口。</p>
 *
 * <p><b>线程约束</b>：{@link #load()} 运行在单元安装阶段，此时宿主的服务级
 * 线程池尚未创建，因此本方法内<b>严禁</b>调用线程池或新建线程，
 * 只允许做文件读取、反射实例化与内存注册。</p>
 */
public final class ModuleRegistry {

    /**
     * 模块清单配置文件名（相对工作目录）。
     */
    private final static String CONFIG_FILE = "aigc-modules.properties";

    /**
     * 模块类配置键前缀。
     */
    private final static String KEY_PREFIX = "module.";

    /**
     * 模块类配置键后缀。
     */
    private final static String KEY_SUFFIX = ".class";

    /**
     * 动作路由器。
     */
    private final ActionRouter router;

    /**
     * 已装载的模块实例。
     */
    private final List<ActionModule> modules = new ArrayList<>();

    /**
     * 宿主能力接口。
     *
     * <p>本阶段尚无宿主实现，故装载期间为 <code>null</code>；
     * 后续阶段由实现类填充。</p>
     */
    private AIGCHost host;

    /**
     * 构造函数。
     *
     * @param router 动作路由器。
     */
    public ModuleRegistry(ActionRouter router) {
        this.router = router;
    }

    /**
     * 获取动作路由器。
     *
     * @return 返回动作路由器。
     */
    public ActionRouter getRouter() {
        return this.router;
    }

    /**
     * 获取宿主能力接口。
     *
     * @return 返回宿主能力接口；本阶段恒为 <code>null</code>。
     */
    public AIGCHost getHost() {
        return this.host;
    }

    /**
     * 获取已装载模块列表（只读视图）。
     *
     * @return 返回模块列表。
     */
    public List<ActionModule> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    /**
     * 从配置文件发现并绑定模块。
     *
     * <p>本阶段的产出是「空注册表 + 可扩展的发现骨架」：配置文件未列出任何模块时，
     * 本方法只记录一行 INFO 日志后返回，因此运行期行为与改造前完全一致。</p>
     *
     * <p>配置查找顺序与宿主既有的配置加载保持一致：先
     * <code>config/aigc-modules.properties</code>，再退回工作目录下的
     * <code>aigc-modules.properties</code>；两者都不存在时按「零模块」处理，
     * 记 INFO 而非 ERROR（首次部署必然如此）。</p>
     *
     * @return 成功绑定的模块数量。
     */
    public int load() {
        File file = new File("config/" + CONFIG_FILE);
        if (!file.exists()) {
            file = new File(CONFIG_FILE);
        }

        if (!file.exists()) {
            Logger.i(ModuleRegistry.class, "#load - Module config \"" + CONFIG_FILE
                    + "\" NOT found, no AIGC action module is loaded");
            this.router.setLoaded(true);
            return 0;
        }

        Properties properties;
        try {
            properties = ConfigUtils.readProperties(file.getAbsolutePath());
        } catch (IOException e) {
            Logger.e(ModuleRegistry.class, "#load - Read module config failed: " + file.getAbsolutePath(), e);
            this.router.setLoaded(true);
            return 0;
        }

        // 按序号升序装载，装载顺序即绑定顺序（先到先得，与冲突策略一致）
        TreeMap<Integer, String> classNameMap = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(KEY_PREFIX) || !key.endsWith(KEY_SUFFIX)) {
                continue;
            }

            String value = properties.getProperty(key);
            if (null == value) {
                continue;
            }

            value = value.trim();
            if (value.isEmpty()) {
                continue;
            }

            // module.<n>.class
            String index = key.substring(KEY_PREFIX.length(), key.length() - KEY_SUFFIX.length());
            try {
                classNameMap.put(Integer.parseInt(index), value);
            } catch (NumberFormatException e) {
                Logger.e(ModuleRegistry.class, "#load - Illegal module key: " + key);
            }
        }

        int count = 0;
        for (String className : classNameMap.values()) {
            ActionModule module = this.instantiate(className);
            if (null == module) {
                continue;
            }

            if (!this.bindActions(module)) {
                // 出现动作冲突时该模块视为装载失败，但不牵连其他模块
                Logger.e(ModuleRegistry.class, "#load - Module \"" + className
                        + "\" is NOT bound because of action conflicts");
                continue;
            }

            this.modules.add(module);
            ++count;
        }

        this.router.setLoaded(true);

        Logger.i(ModuleRegistry.class, "#load - Loaded " + count + " AIGC action module(s), "
                + this.router.size() + " action(s) bound: " + this.router.actions());
        return count;
    }

    /**
     * 实例化一个模块。
     *
     * @param className 模块实现类的全限定名。
     * @return 返回模块实例；失败时返回 <code>null</code>。
     */
    private ActionModule instantiate(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            if (!(instance instanceof ActionModule)) {
                Logger.e(ModuleRegistry.class, "#load - Class \"" + className
                        + "\" is NOT an implementation of ActionModule");
                return null;
            }

            ActionModule module = (ActionModule) instance;
            ModuleDescriptor descriptor = module.getDescriptor();

            if (null == descriptor) {
                Logger.e(ModuleRegistry.class, "#load - Module \"" + className + "\" returns NULL descriptor");
                return null;
            }

            if (!AIGCSPI.isCompatible(descriptor.spiVersion)) {
                Logger.e(ModuleRegistry.class, "#load - Module \"" + className + "\" requires SPI version "
                        + descriptor.spiVersion + ", host supports " + AIGCSPI.VERSION + ", rejected");
                return null;
            }

            Logger.i(ModuleRegistry.class, "#load - Module \"" + descriptor.name + "\" version "
                    + descriptor.version + " discovered, optional=" + descriptor.optional);
            return module;
        } catch (ClassNotFoundException e) {
            Logger.e(ModuleRegistry.class, "#load - Class not found: " + className, e);
        } catch (NoSuchMethodException e) {
            Logger.e(ModuleRegistry.class, "#load - No public no-arg constructor: " + className, e);
        } catch (Exception e) {
            Logger.e(ModuleRegistry.class, "#load - Instantiate failed: " + className, e);
        }

        return null;
    }

    /**
     * 建立模块的全部动作绑定。
     *
     * <p>本方法必须在 {@link ActionModule#setup(AIGCHost)} <b>之后</b>调用：
     * 只有先完成模块自身初始化，建立的绑定才对派发线程可见且状态完整。
     * 本阶段尚未调用 <code>setup</code>（宿主能力实现属下一阶段）。</p>
     *
     * @param module 模块实例。
     * @return 全部绑定成功时返回 <code>true</code>。
     */
    private boolean bindActions(ActionModule module) {
        List<ActionBinding> actions = module.getActions();
        if (null == actions || actions.isEmpty()) {
            Logger.w(ModuleRegistry.class, "#bindActions - Module \"" + module.getName()
                    + "\" provides NO action");
            return true;
        }

        boolean success = true;
        for (ActionBinding binding : actions) {
            if (!this.router.bind(binding, module)) {
                success = false;
            }
        }

        return success;
    }
}
