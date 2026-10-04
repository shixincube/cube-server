/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cube.aigc.spi.DispatcherExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 心理学业务的网关扩展声明。
 *
 * <p><b>本类只声明前缀，不提供 {@code ContextHandler}</b>——插件的编译类路径
 * 刻意不含 {@code cube-dispatcher-*.jar}，而 Jetty handler 必须引用
 * {@code Manager}、Jetty 与 Servlet API。强行在插件内编写会让插件依赖
 * dispatcher，破坏「插件只依赖 cube-common」的依赖方向。</p>
 *
 * <p>因此分工是：插件声明自己拥有哪些前缀，dispatcher 侧据此做冲突检测，
 * handler 仍由 dispatcher 注册（见 {@code Manager#setupHandler}）。</p>
 *
 * <p>⚠️ 心理学有 3 个端点的前缀<b>不在</b> {@code /aigc/psychology} 下：
 * {@code /aigc/painting/label}、{@code /aigc/stream/strategy}、
 * {@code /aigc/stream/caption}。这是历史命名，线协议不可改，故一并声明，
 * 否则 dispatcher 侧的冲突检测会漏掉它们。</p>
 */
public final class PsychologyDispatcherExtension implements DispatcherExtension {

    @Override
    public String getName() {
        return PsychologyModule.NAME;
    }

    @Override
    public List<String> getRestPrefixes() {
        return Collections.unmodifiableList(Arrays.asList(
                PsychologyModule.REST_PREFIX,
                "/aigc/painting",
                "/aigc/stream"));
    }
}
