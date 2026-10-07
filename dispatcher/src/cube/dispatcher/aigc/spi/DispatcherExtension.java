/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.spi;

import org.eclipse.jetty.server.handler.ContextHandler;

import java.util.Collections;
import java.util.List;

/**
 * 业务模块的网关扩展点。
 *
 * <p><b>为何不让插件直接注册 {@code ContextHandler} 到 {@code HttpServer}</b>：
 * {@code HttpServer} 是网关内部设施，插件若持有它，就等于网关与业务模块
 * 双向耦合——插件改动会波及网关装配。职责拆为两半：</p>
 * <ul>
 *   <li><b>插件</b>只产出自己拥有的 {@code ContextHandler} 实例
 *       （{@link #getEndpointHandlers()}），路径由各 handler 自身声明；</li>
 *   <li><b>dispatcher 侧</b>据此注册并做路径去重，装配顺序与鉴权语义仍由网关掌控。</li>
 * </ul>
 *
 * <p><b>依赖方向</b>：本接口位于 dispatcher 模块，实现方（业务模块插件）在
 * <b>编译期依赖</b> {@code cube-dispatcher-*.jar}；本接口所在的一侧
 * <b>不引用任何插件类型</b>，仅按配置中的类名反射装载。</p>
 *
 * <p><b>实现类加载位置</b>：实现类只在 <b>dispatcher 进程</b>被反射装载。
 * 插件 jar 与宿主 service 同ClassLoader（{@code deploy/libs/}），但
 * service 侧不装载本接口的实现类，因此插件对 dispatcher 的编译期依赖
 * 不会在 service 侧触发类加载错误。</p>
 *
 * <p><b>向后兼容</b>：{@link #getEndpointHandlers()} 为 {@code default} 方法，
 * 早期只声明模块名与 REST 前缀的扩展实现<b>无需任何改动</b>即可继续装载
 * ——它只提供前缀声明（供冲突检测与兜底通道定位），不提供端点。</p>
 *
 * <p><b>路径兼容</b>：端点路径一律以各 handler 的 {@code super(path)} 为准，
 * 本接口不参与路径拼接，也不改动既有路径字符串。</p>
 */
public interface DispatcherExtension {

    /**
     * 获取扩展名，用于日志与冲突诊断。
     *
     * @return 返回扩展名。
     */
    String getName();

    /**
     * 获取本扩展声明的 REST 前缀（不含尾斜杠）。
     *
     * <p>供 dispatcher 侧做<b>前缀冲突检测</b>：两个扩展声明同一前缀时
     * 后者不得覆盖前者，须记录 ERROR 并拒绝。</p>
     *
     * <p>⚠️ 前缀声明<b>不</b>等于端点归属：一个前缀下可能混挂宿主端点
     * 与模块端点（如 {@code /app}），故前缀只用于诊断，不得用于推断
     * 某个端点该由谁注册。</p>
     *
     * @return 返回前缀列表，不可为 {@code null}。
     */
    List<String> getRestPrefixes();

    /**
     * 获取本扩展提供的 REST 端点处理器。
     *
     * <p>返回的每个实例都应已完成自身路径声明（构造器内 {@code super(path)}）。
     * dispatcher 侧按<b>迭代顺序</b>注册，并按 {@code getContextPath()} 做去重：
     * 路径重复时<b>先注册者保留</b>，后者记 ERROR 并跳过。</p>
     *
     * <p>默认返回空列表——不提供端点的扩展（只需前缀声明）无需改动。</p>
     *
     * @return 返回端点处理器列表，不可为 {@code null}。
     */
    default List<ContextHandler> getEndpointHandlers() {
        return Collections.emptyList();
    }
}