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
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 业务模块注册表。
 *
 * <p>持有唯一的 {@link ActionRouter} 实例，负责：读取模块清单 → 实例化 →
 * 校验 {@link ModuleDescriptor#spiVersion} → 建立动作绑定 →
 * 调用 {@link ActionModule#setup(AIGCHost)}。</p>
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
 * <p><b>线程约束</b>：<b>{@link #load()} 运行在单元安装阶段，此时宿主的服务级
 * 线程池尚未创建，因此本方法内严禁调用线程池或新建线程</b>，
 * 只允许做文件读取、反射实例化与内存注册。模块的 {@code setup()} 在本方法内
 * 被同步调用，故模块实现<b>不得</b>在 {@code setup()} 中提交异步任务或阻塞等待
 * 宿主就绪；确需异步应在首次动作派发时惰性启动。</p>
 *
 * <p><b>能力校验时机</b>：{@link #verifyCapabilities()} <b>不在</b>装载阶段执行。
 * 原因是装载时单元尚未注册（单元由后续的 Relay 上报产生），此刻校验必然全部失败。
 * 因此校验被设计为宿主就绪后的显式调用，见该方法注释。</p>
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
 * 模块启用开关配置键后缀。
 */
private final static String KEY_ENABLED_SUFFIX = ".enabled";

/**
 * 动作路由器。
 */
private final ActionRouter router;

/**
 * 已装载的模块实例。
 *
 * <p>使用并发集合：本列表在装载阶段写入、在派发与兄弟模块查找阶段读取，
 * 虽时序上装载早于读取，但 {@link #findModule(String)} 可能被运行期调用，
 * 不应依赖「写完即冻结」的隐含假设。</p>
 */
private final List<ActionModule> modules = new CopyOnWriteArrayList<>();

/**
 * 模块名 → 实例索引，供 {@link #findModule(String)} 与兄弟模块查找使用。
 */
private final ConcurrentHashMap<String, ActionModule> moduleMap = new ConcurrentHashMap<>();

/**
 * 宿主能力接口。
 */
private AIGCHost host;

/**
 * 是否存在非可选模块初始化失败。
 */
private volatile boolean blockingFailure = false;

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
 * @return 返回宿主能力接口；未设置时返回 <code>null</code>。
 */
public AIGCHost getHost() {
    return this.host;
}

/**
 * 设置宿主能力接口。
 *
 * <p>必须在 {@link #load()} 之前调用：模块的 {@code setup(host)} 在装载时
 * 同步执行，届时需要已可用的宿主能力。</p>
 *
 * @param host 宿主能力接口。
 */
public void setHost(AIGCHost host) {
    this.host = host;
}

/**
 * 查找已装载的模块实例。
 *
 * @param moduleName 模块名。
 * @return 返回模块实例；不存在时返回 <code>null</code>。
 */
public ActionModule findModule(String moduleName) {
    if (null == moduleName) {
        return null;
    }

    return this.moduleMap.get(moduleName);
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
 * <p>出厂配置未列出任何模块时，本方法只记录一行 INFO 日志后返回，
 * 因此运行期行为。</p>
 *
 * <p>配置查找顺序与宿主既有的配置加载保持一致：先
 * <code>config/aigc-modules.properties</code>，再退回工作目录下的
 * <code>aigc-modules.properties</code>；两者都不存在时按「零模块」处理，
 * 记 INFO 而非 ERROR（首次部署必然如此）。</p>
 *
 * <p>单个模块的处理顺序为：启用开关 → 实例化 → 绑定动作 → {@code setup(host)}。
 * <b>{@code setup} 必须排在绑定之后</b>，因为只有先完成模块自身初始化，
 * 建立的绑定才对派发线程可见且状态完整；反过来会让派发线程读到未初始化的实例。</p>
 *
 * @return 成功装载的模块数量。
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
    for (Map.Entry<Integer, String> entry : classNameMap.entrySet()) {
        int index = entry.getKey();
        String className = entry.getValue();

        // 启用开关：未显式配置时默认启用，避免运维漏写开关导致模块静默不加载
        if (!this.isEnabled(properties, index)) {
            Logger.i(ModuleRegistry.class, "#load - Module \"" + className + "\" is DISABLED by configuration");
            continue;
        }

        ActionModule module = this.instantiate(className);
        if (null == module) {
            continue;
        }

        // 先登记动作声明，再绑定：声明不随绑定失败而移除，
        // 使宿主在模块未就绪时能识别「该动作本属该模块」并回明确状态码
        this.declareActions(module);

        if (!this.bindActions(module)) {
            // 出现动作冲突时该模块视为装载失败，但不牵连其他模块
            Logger.e(ModuleRegistry.class, "#load - Module \"" + className
                    + "\" is NOT bound because of action conflicts");
            continue;
        }

        // 绑定成功后再初始化：模块在 setup 中读取自身状态，此时其动作已全部可见
        if (!this.setupModule(module)) {
            this.unbindActions(module);
            Logger.e(ModuleRegistry.class, "#load - Module \"" + className + "\" setup FAILED,"
                    + " its actions have been rolled back");
            continue;
        }

        this.modules.add(module);
        this.moduleMap.put(module.getName(), module);
        ++count;
    }

    this.router.setLoaded(true);

    Logger.i(ModuleRegistry.class, "#load - Loaded " + count + " AIGC action module(s), "
            + this.router.size() + " action(s) bound: " + this.router.actions());
    return count;
}

/**
 * 读取模块的启用开关。
 *
 * @param properties 配置属性。
 * @param index 模块序号。
 * @return 启用时返回 <code>true</code>；未配置或值非法时按「启用」处理。
 */
private boolean isEnabled(Properties properties, int index) {
    String value = properties.getProperty(KEY_PREFIX + index + KEY_ENABLED_SUFFIX);
    if (null == value) {
        return true;
    }

    return Boolean.parseBoolean(value.trim());
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
 * <p>本方法必须在 {@link ActionModule#setup(AIGCHost)} <b>之前</b>调用：
 * 模块初始化时其动作已全部可见，反之则会让派发线程读到未初始化的实例。</p>
 *
 * @param module 模块实例。
 * @return 全部绑定成功时返回 <code>true</code>。
 */
private boolean bindActions(ActionModule module) {    List<ActionBinding> actions = module.getActions();
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

/**
 * 登记模块声明的动作名。
 *
 * <p><b>与 {@link #bindActions(ActionModule)} 的区别</b>：绑定表在装载失败时会被回滚，
 * 而本方法登记的声明<b>不随之移除</b>。这样宿主在派发时能区分
 * 「动作属于未就绪的模块」与「动作不属于任何模块」，前者回明确状态码、
 * 后者走既有分支，避免模块未就绪时请求悬挂。</p>
 *
 * @param module 模块实例。
 */
private void declareActions(ActionModule module) {
    try {
        List<ActionBinding> actions = module.getActions();
        if (null == actions) {
            return;
        }

        List<String> names = new ArrayList<>(actions.size());
        for (ActionBinding binding : actions) {
            if (null != binding) {
                names.add(binding.action);
            }
        }

        this.router.declareAll(names);
    } catch (Throwable t) {
        // 声明阶段失败不阻断装载：该模块的绑定与 setup 仍照常进行，
        // 只是失去「明确状态码」这层兜底，退化为既有分支处理
        Logger.e(ModuleRegistry.class, "#declareActions - FAILED for module \""
                + module.getName() + "\"", (t instanceof Exception) ? (Exception) t : null);
    }
}

/**
 * 注销模块的全部动作绑定，用于初始化失败时回滚。
 *
 * @param module 模块实例。
 */
private void unbindActions(ActionModule module) {
    if (this.router.unbindAll(module)) {
        Logger.w(ModuleRegistry.class, "#unbindActions - Actions of module \"" + module.getName()
                + "\" have been unbound");
    }
}

/**
 * 初始化模块。
 *
 * <p><b>失败处理策略</b>：模块声明 <code>optional=false</code> 时，
 * 初始化失败仅记录 ERROR 并跳过该模块（主服务继续启动），但宿主会记为「未就绪」，
 * 由 {@link #hasBlockingFailure()} 暴露给运维；声明 <code>optional=true</code> 时
 * 同样只记录 ERROR。两者都不抛异常——插件不得阻断主服务，这是模块化的基本约束。</p>
 *
 * @param module 模块实例。
 * @return 初始化成功时返回 <code>true</code>。
 */
private boolean setupModule(ActionModule module) {
    ModuleDescriptor descriptor = module.getDescriptor();
    boolean optional = (null != descriptor) && descriptor.optional;

    try {
        module.setup(this.host);
        Logger.i(ModuleRegistry.class, "#setupModule - Module \"" + module.getName() + "\" is ready");
        return true;
    } catch (Throwable t) {
        // 捕获 Throwable 而非 Exception：模块的 setup 由第三方提供，
        // Error（如 NoClassDefFoundError）同样不应让宿主启动失败
        Logger.e(ModuleRegistry.class, "#setupModule - Module \"" + module.getName()
                + "\" setup FAILED, optional=" + optional, (t instanceof Exception) ? (Exception) t : null);

        if (!optional) {
            this.blockingFailure = true;
        }

        return false;
    }
}

/**
 * 校验模块声明的单元能力是否均可用。
 *
 * <p><b>调用时机</b>：必须在宿主就绪（单元已通过 Relay 上报注册）之后调用，
 * 因此<b>不能</b>在 {@link #load()} 内执行——装载阶段单元表必然为空，
 * 那样只会得到全量误报。</p>
 *
 * <p>校验失败不阻止模块工作：模块声明的能力可能只在特定部署形态下才有单元，
 * 宿主不预置业务能力名，无从判断「缺失」是配置错误还是部署预期。
 * 因此此处只记录明确的 WARN，把判断权交给运维。</p>
 *
 * @return 全部已装载模块的声明能力均可用时返回 <code>true</code>。
 */
public boolean verifyCapabilities() {
    boolean allAvailable = true;

    for (ActionModule module : this.modules) {
        ModuleDescriptor descriptor = module.getDescriptor();
        if (null == descriptor || descriptor.requiredCapabilities.isEmpty()) {
            continue;
        }

        for (String capability : descriptor.requiredCapabilities) {
            if (null == this.host || !this.host.hasUnit(capability)) {
                // 能力缺失意味着该模块的核心动作无法执行，属故障而非提示，
                // 故记 ERROR：模块已装载但功能不可用，宿主据此回ModuleNotLoaded
                Logger.e(ModuleRegistry.class, "#verifyCapabilities - Module \"" + module.getName()
                        + "\" requires capability \"" + capability + "\" but NO unit is available");
                allAvailable = false;
            }
        }
    }

    if (allAvailable) {
        Logger.i(ModuleRegistry.class, "#verifyCapabilities - All required capabilities of "
                + this.modules.size() + " module(s) are available");
    }

    return allAvailable;
}

/**
 * 是否存在「非可选模块初始化失败」。
 *
 * <p>供宿主在健康检查与日志中暴露：此时模块未装载，但主服务仍在运行，
 * 属降级而非故障。</p>
 *
 * @return 存在非可选模块初始化失败时返回 <code>true</code>。
 */
public boolean hasBlockingFailure() {
    return this.blockingFailure;
}

/**
 * 卸载全部模块并清空路由表。
 *
 * <p>由宿主在单元卸载时调用。单个模块的 {@code teardown()} 异常不影响其余模块，
 * 且不向上抛出——停机路径不应因插件异常而中断。</p>
 */
public void teardownAll() {
    for (ActionModule module : this.modules) {
        try {
            module.teardown();
            Logger.i(ModuleRegistry.class, "#teardownAll - Module \"" + module.getName() + "\" is stopped");
        } catch (Throwable t) {
            Logger.e(ModuleRegistry.class, "#teardownAll - Module \"" + module.getName()
                    + "\" teardown FAILED", (t instanceof Exception) ? (Exception) t : null);
        }

        this.router.unbindAll(module);
        this.moduleMap.remove(module.getName());
    }

    this.modules.clear();
    this.blockingFailure = false;

    Logger.i(ModuleRegistry.class, "#teardownAll - All AIGC action modules are unloaded");
}

/**
 * 驱动全部模块的心跳。
 *
 * <p>由宿主在自身的 tick 节奏中调用（与宿主心跳同频）， * 「门面逐个调业务场景 {@code onTick}」的硬编码写法。</p>
 *
 * <p>单个模块的心跳异常不影响其余模块，也不向上抛出——tick 是周期性维护，
 * 一个模块抛异常不应导致其余模块失去心跳。</p>
 *
 * @param now 当前时刻（毫秒）。
 */
public void tick(long now) {
    for (ActionModule module : this.modules) {
        try {
            module.onTick(now);
        } catch (Throwable t) {
            Logger.e(ModuleRegistry.class, "#tick - Module \"" + module.getName()
                    + "\" onTick FAILED", (t instanceof Exception) ? (Exception) t : null);
        }
    }
}
}
