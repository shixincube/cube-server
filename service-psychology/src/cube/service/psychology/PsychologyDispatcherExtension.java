/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology;

import cube.dispatcher.aigc.spi.DispatcherExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 心理学业务的网关扩展声明。
 *
 * <p><b>本类只声明前缀</b>——模块的端点由
 * {@link cube.service.psychology.dispatcher.PsychologyEndpoints} 提供，
 * 它同样实现 {@link DispatcherExtension}，由 dispatcher 侧按配置项
 * {@code module.extensions} 里的类名反射装载。</p>
 *
 * <p>⚠️ 心理学有端点的前缀<b>不</b>都在 {@code /aigc/psychology} 下：
 * {@code /aigc/painting}、{@code /aigc/copilot}、{@code /aigc/chart}、
 * {@code /aigc/cot}、{@code /app/customer}、{@code /app/schedule}。
 * 这是历史命名，线协议不可改，故一并声明，否则 dispatcher 侧的冲突检测会漏掉它们。</p>
 *
 * <p>⚠️ <b>加载位置</b>：本类与 {@code PsychologyEndpoints} 都只在
 * dispatcher 进程被反射装载，宿主 service 侧不引用它们。</p>
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
                "/aigc/copilot",
                "/aigc/chart",
                "/aigc/cot",
                "/app/customer",
                "/app/schedule"));
    }
}