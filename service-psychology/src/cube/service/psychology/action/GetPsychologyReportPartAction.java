/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cube.service.psychology.scene.ReportRenderer;
import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.ReportSection;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.entity.AIGCUnit;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 读取报告分段内容动作。
 *
 * <p>对应线协议动作 {@code getPsychologyReportPart}，逐字符等同于既有枚举
 * {@code AIGCAction.GetPsychologyReportPart} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → Failure / Ok → InvalidParameter}。</p>
 *
 * <p><b>⚠️ 两处不可「顺手统一」的差异</b>：</p>
 * <ol>
 *   <li>报告不存在时回 {@code Failure} + <b>空对象</b>（L95），
 *       而同批的 {@link GetPsychologyReportAction} 回 {@code Failure} +
 *       <b>回显请求体</b>；</li>
 *   <li>本动作没有独立的参数校验分支——整个「参数解析 + 字段组装」
 *       被一个 try 包住（L74-166），任何异常（含 {@code sn} 缺失）都统一
 *       落 {@code InvalidParameter}。因此<b>不要</b>把 {@code sn} 读取单独
 *       拆出来做前置校验，否则它会从 {@code InvalidParameter} 变成别的码。</li>
 * </ol>
 *
 * <p><b>处理中报告的语义</b>：当报告状态为
 * {@code Processing} 或 {@code Inferencing} 时，只填 {@code thought}
 * 一个字段（原始特征描述，<b>不</b>调模型），其余字段全部跳过，
 * 但仍然回 {@code Ok}（L144-159）。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link ModifyReportRemarkAction}。</p>
 */
public final class GetPsychologyReportPartAction implements AIGCActionTask {

    /**
     * 主观题特征描述的推理提示词。
     *
     * <p><b>⚠️ 逐字复刻的硬编码文本</b>（L171-172）。
     * 任何改动都会改变模型输出，进而改变 {@code thought} 字段的内容，
     * 属线协议可见变更。</p>
     */
    private final static String THOUGHT_PROMPT = "# 任务目标\n\n你作为经验丰富的、能熟练应用房树人绘画投射测验的心理咨询师，"
            + "对给定的绘画内容心理描述进行通俗化表述。\n\n# 绘画内容心理描述\n\n"
            + "```\n%s\n```\n\n# 注意事项\n\n"
            + "1. 不要额外添加任何和绘画内容无关的描述，尽可能多描述画面。\n"
            + "2. 可以使用专业词汇，但要避免使用极端话术。\n"
            + "3. 可以结合多个绘画内容，丰富描述，但是不能随意修改对应的心理特征。\n"
            + "4. 不要加前言，注意分段。";

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
        if (null == authToken) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }

        // 整个「参数解析 + 字段组装」共用一个 try，与L74-166 的结构一致：
        // 任何异常都统一落 InvalidParameter，故不可把某一步单独提前 return
        try {
            JSONObject data = ctx.getRequest().data;

            long sn = data.getLong("sn");
            boolean content = data.has("content") && data.getBoolean("content");
            boolean section = data.has("section") && data.getBoolean("section");
            boolean thought = data.has("thought") && data.getBoolean("thought");
            // L80-82：未要求正文时才接受摘要开关
            boolean summary = !content && data.has("summary") && data.getBoolean("summary");
            boolean rating = data.has("rating") && data.getBoolean("rating");

            boolean link = data.has("link") && data.getBoolean("link");
            Endpoint endpoint = link ? new Endpoint(data.getJSONObject("endpoint")) : null;

            AIGCHost host = ctx.getHost();
            // ⚠️ 取报告「实体」而非导出的 JSON：本动作需要读取状态、章节列表与
            // 时间戳等结构化字段，并把它们交给宿主做进一步加工
            PaintingReport report = host.getPaintingReport(sn);

            if (null == report) {
                // L93 先记一条 WARN 再应答
                Logger.w(this.getClass(), "#handle - Can NOT find report sn: " + sn);
                ctx.respondEmpty(AIGCStateCode.Failure);
                return AIGCStateCode.Failure;
            }

            JSONObject responseData = new JSONObject();
            responseData.put("sn", sn);
            responseData.put("state", report.getState().code);
            responseData.put("timestamp", report.timestamp);

            if (AIGCStateCode.Ok.code == report.getState().code) {
                this.fillCompleted(ctx, host, report, responseData, content, section, thought, summary, rating,
                        endpoint, tokenCode);
            }
            else if ((AIGCStateCode.Processing.code == report.getState().code
                    || AIGCStateCode.Inferencing.code == report.getState().code) && thought) {
                // 处理中：只回原始特征描述，不调模型，仍回 Ok
                PaintingFeatureSet featureSet = host.getPaintingFeatureSet(sn);
                if (null != featureSet) {
                    responseData.put("thought", ReportRenderer.makePaintingFeature(featureSet));
                }
                else {
                    Logger.w(this.getClass(), "#handle - Can NOT find feature set for report: " + sn);
                }
            }

            ctx.respond(AIGCStateCode.Ok, responseData);
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            Logger.w(this.getClass(), "#handle", e);
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }

    /**
     * 填充已完成报告的各可选字段。
     *
     * <p>对应 L105-141。</p>
     *
     * @param ctx 动作上下文。
     * @param host 宿主能力。
     * @param report 报告。
     * @param responseData 应答数据，就地填充。
     * @param content 是否输出正文。
     * @param section 是否输出分节。
     * @param thought 是否输出推理描述。
     * @param summary 是否输出摘要。
     * @param rating 是否输出评级。
     * @param endpoint 访问端点。
     * @param tokenCode 令牌码。
     */
    private void fillCompleted(ActionContext ctx, AIGCHost host, PaintingReport report,
            JSONObject responseData, boolean content, boolean section, boolean thought, boolean summary,
            boolean rating, Endpoint endpoint, String tokenCode) {
        if (content) {
            responseData.put("content", ReportRenderer.makeContent(report, true, 5, true));
        }

        if (section) {
            List<ReportSection> list = report.getReportSections();
            if (null != list) {
                JSONArray array = new JSONArray();
                for (ReportSection rs : list) {
                    array.put(rs.toPermissionJSON());
                }

                responseData.put("sections", array);
            }
        }

        if (thought) {
            PaintingFeatureSet featureSet = host.getPaintingFeatureSet(report.sn);
            if (null != featureSet) {
                responseData.put("thought", this.inferFeatureThought(ctx, featureSet));
            }
        }

        if (summary) {
            responseData.put("summary", ReportRenderer.makeContent(report, true, 0, false));
        }

        if (rating) {
            responseData.put("rating", ReportRenderer.makeRatingInformation(report));
        }

        if (null != endpoint) {
            responseData.put("link", ReportRenderer.makePageLink(endpoint, tokenCode, report, true, true));
        }
    }

    /**
     * 把绘画特征集转为通俗化描述。
     *
     * <p>对应 L169-181：先取原始特征描述作为载荷与降级值，
     * 再让模型改写；模型不可用时返回原始载荷。</p>
     *
     * @param ctx 动作上下文。
     * @param featureSet 绘画特征集。
     * @return 返回通俗化描述；模型不可用时返回原始特征描述。
     */
    private String inferFeatureThought(ActionContext ctx, PaintingFeatureSet featureSet) {
        String payload = ReportRenderer.makePaintingFeature(featureSet);

        AIGCUnit unit = ctx.getHost().selectUnit(ModelConfig.BAIZE_2_UNIT);
        GeneratingRecord record = (null == unit) ? null
                : ctx.getHost().syncGenerateText(unit, String.format(THOUGHT_PROMPT, payload),
                        new GeneratingOption(), null, null);

        if (null == record) {
            // 降级：输出原始内容，与L175-178 一致
            Logger.w(this.getClass(), "#inferFeatureThought - Infer failed, output raw content");
            return payload;
        }

        return record.answer;
    }
}
