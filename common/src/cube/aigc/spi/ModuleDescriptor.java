/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 业务模块描述符。
 *
 * <p>宿主据此校验依赖、决定是否加载、决定加载失败时是否阻断主服务。</p>
 *
 * <p><b>关于 {@link #requiredCapabilities}</b>：其值等于单元注册时上报的
 * <code>AICapability.getName()</code>（如 <code>"Psychology"</code>），
 * 宿主不预置任何业务能力名，只在启动期做「每个声明的能力至少有一个有效单元」
 * 的存在性校验；运行期由模块自传键调用 {@link AIGCHost#selectUnit(String)}。</p>
 */
public final class ModuleDescriptor {

    /**
     * 模块名，全局唯一；同时作为存储命名与资源目录的前缀。
     */
    public final String name;

    /**
     * 模块版本，仅用于日志与诊断。
     */
    public final String version;

    /**
     * 模块编译期使用的 SPI 版本，必须等于 {@link AIGCSPI#VERSION}。
     */
    public final int spiVersion;

    /**
     * 动作命名空间，仅供冲突检测与兜底通道匹配，不参与线协议。
     */
    public final List<String> actionNamespaces;

    /**
     * 该模块的 REST 前缀（不含尾斜杠）。
     */
    public final List<String> restPrefixes;

    /**
     * 需要的模型单元能力名，取值等于单元上报的 <code>AICapability.getName()</code>。
     */
    public final List<String> requiredCapabilities;

    /**
     * 依赖的兄弟模块名（对应 {@link AIGCHost#getSiblingModule}）。
     */
    public final List<String> requires;

    /**
     * 装载超时（毫秒）。实现禁止阻塞超过该时长。
     */
    public final long bootTimeoutMs;

    /**
     * 是否为可选模块。
     *
     * <p><code>true</code> 表示加载失败只记录 ERROR 并把模块标记为未就绪，
     * 主服务继续启动；<code>false</code> 表示加载失败将使宿主保持「未就绪」。
     * <b>该值必须由模块显式声明，不允许宿主侧隐式默认。</b></p>
     */
    public final boolean optional;

    /**
     * 构造函数。
     *
     * @param name 模块名。
     * @param version 模块版本。
     * @param spiVersion SPI 版本。
     * @param actionNamespaces 动作命名空间列表，可为 <code>null</code>。
     * @param restPrefixes REST 前缀列表，可为 <code>null</code>。
     * @param requiredCapabilities 所需单元能力名列表，可为 <code>null</code>。
     * @param requires 兄弟模块依赖列表，可为 <code>null</code>。
     * @param bootTimeoutMs 装载超时（毫秒）。
     * @param optional 是否可选模块。
     */
    public ModuleDescriptor(String name, String version, int spiVersion,
            List<String> actionNamespaces, List<String> restPrefixes,
            List<String> requiredCapabilities, List<String> requires,
            long bootTimeoutMs, boolean optional) {
        this.name = name;
        this.version = version;
        this.spiVersion = spiVersion;
        this.actionNamespaces = copy(actionNamespaces);
        this.restPrefixes = copy(restPrefixes);
        this.requiredCapabilities = copy(requiredCapabilities);
        this.requires = copy(requires);
        this.bootTimeoutMs = bootTimeoutMs;
        this.optional = optional;
    }

    /**
     * 复制列表为不可修改视图。
     *
     * @param list 原始列表，可为 <code>null</code>。
     * @return 返回不可修改的列表。
     */
    private static List<String> copy(List<String> list) {
        if (null == list || list.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<>(list));
    }
}
