/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import java.util.List;

/**
 * 业务模块的网关扩展点。
 *
 * <p><b>为何不让插件直接提供 {@code ContextHandler}</b>：业务模块（如
 * {@code service-psychology}）的编译类路径<b>刻意不含</b>
 * {@code cube-dispatcher-*.jar}——它必须与宿主 service 同ClassLoader 且
 * 零 dispatcher 依赖。而 Jetty handler 必须 import
 * {@code cube.dispatcher.aigc.Manager}、Jetty 与 Servlet API，这些在插件侧不可见。
 * 若强行在插件内编写，插件构建就得引入 dispatcher jar，依赖方向随即倒转。</p>
 *
 * <p>故职责拆为两半：</p>
 * <ul>
 *   <li><b>插件</b>只声明自己拥有哪些 REST 前缀与动作（{@link #getRestPrefixes()}）；</li>
 *   <li><b>dispatcher 侧</b>据此注册 handler，路径与鉴权语义仍由 dispatcher 掌控。</li>
 * </ul>
 *
 * <p>实现类若存在，须打包进 {@code deploy/libs/}（与宿主同一 ClassLoader）。</p>
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
     * @return 返回前缀列表，不可为 {@code null}。
     */
    List<String> getRestPrefixes();
}
