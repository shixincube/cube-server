/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.dispatcher;

import cube.dispatcher.aigc.spi.DispatcherExtension;
import cube.service.psychology.dispatcher.handler.*;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 心理学模块的网关端点提供者。
 *
 * <p>向 dispatcher 注入本模块拥有的 REST 端点。dispatcher 侧
 * {@code DispatcherExtensions} 按配置类名反射装载本类，注册其中的处理器；
 * 本模块<b>不</b>直接接触 {@code HttpServer}，也不决定注册顺序之外的任何装配细节。</p>
 *
 * <p><b>编译期依赖方向</b>：本类 import 了 {@code cube.dispatcher.*} 的
 * handler 类型，故插件构建的 classpath 必须包含 {@code cube-dispatcher-*.jar}。
 * 这是<b>有意反向</b>的依赖——被依赖方（dispatcher）仍不引用本包任何类型，
 * 方向是单向的。</p>
 *
 * <p>⚠️ <b>加载位置约束</b>：本类<b>只在 dispatcher 进程被反射装载</b>。
 * 插件 jar 与宿主 service 同一 ClassLoader（{@code deploy/libs/}），
 * 但 service 侧的模块装载只读 {@code aigc-modules.properties} 里的
 * {@code module.*.class}，不会触及本类；Java 惰性链接下，
 * service 进程因此不会因缺少 dispatcher 的类型而失败。
 * <b>禁止</b>在 {@code cube.service.psychology} 的任何宿主侧类中引用本类。</p>
 *
 * <p><b>路径兼容</b>：下列端点的路径<b>全部</b>由各 handler 自身的
 * {@code super(path)} 决定，本类不拼接、不改写任何路径字符串，
 * 且注册顺序与改造前 {@code Manager#setupHandler} 中的顺序逐条一致。</p>
 *
 * <p><b>语音相关端点</b>（语音识别、说话人分离、语音情绪识别、
 * 语音分析、语音流申请与停止）属宿主基础能力实现，其 handler 仍留在
 * {@code cube.dispatcher.aigc.handler} 根包，不在本类注册。</p>
 */
public final class PsychologyEndpoints implements DispatcherExtension {

    /**
     * 模块名，与 {@code PsychologyModule#getName()} 一致。
     */
    private final static String NAME = "psychology";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getRestPrefixes() {
        // ⚠️ 本模块的端点并不全部落在 /aigc/psychology 下（还有 /aigc/painting、
        // /aigc/copilot、/aigc/chart、/aigc/cot、/app/customer、/app/schedule 等），
        // 故此处声明的是本模块<b>涉及</b>的顶级前缀，供冲突诊断；
        // 端点归属以下方 getEndpointHandlers() 的实际列表为准。
        return Collections.unmodifiableList(Arrays.asList(
                "/aigc/psychology",
                "/aigc/painting",
                "/aigc/copilot",
                "/aigc/chart",
                "/aigc/cot",
                "/app/customer",
                "/app/schedule"));
    }

    @Override
    public List<ContextHandler> getEndpointHandlers() {
        // ⚠️ 顺序即注册顺序，与改造前 Manager#setupHandler 中的语句顺序逐条一致。
        //    宿主自身的端点已在 dispatcher 侧先行注册，且重复路径会被去重跳过。
        return Arrays.asList(
                // ── 报告族 ──
                new PsychologyReports(),
                new PsychologyReportPage(),
                new PsychologyReportParts(),
                new PsychologyModifyReportRemark(),
                new ResetReportAttention(),
                new PsychologyPaintingReportState(),
                new PsychologyStopping(),

                // ── 量表族 ──
                new PsychologyScales(),
                new PsychologyScaleOperation(),

                // ── 对话与综合评测 ──
                new PsychologyConversation(),
                new PsychologyComprehensives(),

                // ── 绘画与标签 ──
                new PsychologyPaintings(),
                new CheckPsychology(),
                new PaintingLabels(),
                new Chart(),
                new ChainOfThought(),

                // ── 模板文章 ──
                new PsychologyTemplateArticle(),

                // ── 咨询策略（路径在 /aigc/stream 下，历史命名，不可改） ──
                new QueryCounselingStrategy(),
                new QueryCounselingCaption(),

                // ── 语音流（宿主语音基础能力之上的咨询业务编排：
                //    分离与情绪识别由宿主完成，这里只做语音分析与停止） ──
                new SpeechAnalysis(),
                new StopStream(),

                // ── 陪练（路径在 /aigc/copilot 下） ──
                new ApplyCopilot(),
                new DisposeCopilot(),
                new SubmitCopilotSheet(),

                // ── 客户与日程 CRUD（路径在 /app 下） ──
                new Customers(),
                new NewCustomer(),
                new DeleteCustomer(),
                new Schedules(),
                new NewSchedule(),
                new DeleteSchedule());
    }
}