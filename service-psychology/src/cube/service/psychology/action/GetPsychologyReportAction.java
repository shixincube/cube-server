/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.PsychologyModule;
import org.json.JSONObject;

/**
 * 读取报告动作。
 *
 * <p>对应线协议动作 {@code getPsychologyReport}，逐字符等同于既有枚举
 * {@code AIGCAction.GetPsychologyReport} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → InvalidParameter → Ok / Failure}。</p>
 *
 * <p><b>三分支结构</b>（L88-176）：</p>
 * <ul>
 *   <li>{@code sn != 0}：查绘画报告，未命中则回退查量表报告；</li>
 *   <li>{@code sn == 0 && pageSize != 0}：列表分支，按 {@code type} 分绘画/量表；</li>
 *   <li>{@code sn == 0 && pageSize == 0}：{@code InvalidParameter}（回显请求体）。</li>
 * </ul>
 *
 * <p><b>⚠️ 已知缺陷，请勿「顺手修正」</b>：量表报告列表分支
 * 应答的 {@code size} 字段填的是<b>总条数</b>而非请求的 {@code pageSize}
 * （{@code GetPsychologyReportTask} L166）。这会让客户端按 {@code size}
 * 翻页时行为异常，但已上线多年、客户端可能已适配该行为；
 * 修正它属于独立的线协议变更。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link StopGeneratingReportAction}，令牌无效回 {@code IllegalOperation}。</p>
 */
public final class GetPsychologyReportAction implements AIGCActionTask {

    /**
     * 报告类型：绘画报告。
     */
    private final static String TYPE_PAINTING = "painting";

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

        // 刻意不判 data 是否为 null：packet.data 为 null 时会在下方
        // 解析块抛 NPE 并落入 catch 回 InvalidParameter（空对象）。
        // 若改用 ctx.getParams()，它返回空对象使全部参数取默认值，
        // 从而走到「列表分支」回 Ok——把失败变成了成功
        JSONObject data = ctx.getRequest().data;

        long sn = 0;
        boolean markdown = false;
        boolean sections = false;
        int pageIndex = 0;
        int pageSize = 10;
        boolean descending = true;
        int state = -1;
        String type = TYPE_PAINTING;

        try {
            sn = data.has("sn") ? data.getLong("sn") : 0;
            sections = data.has("sections") && data.getBoolean("sections");
            markdown = data.has("markdown") && data.getBoolean("markdown");
            pageIndex = data.has("page") ? data.getInt("page") : 0;
            pageSize = data.has("size") ? data.getInt("size") : 10;
            descending = !data.has("desc") || data.getBoolean("desc");
            state = data.has("state") ? data.getInt("state") : -1;
            type = data.has("type") ? data.getString("type") : TYPE_PAINTING;
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        PsychologyModule module = (PsychologyModule) ctx.getModule();

        if (0 != sn) {
            return this.querySingle(ctx, module, sn, markdown, sections);
        }

        if (0 != pageSize) {
            return this.queryList(ctx, module, authToken.getContactId(), pageIndex, pageSize,
                    descending, state, type);
        }

        // 此处回显原始请求体
        ctx.respond(AIGCStateCode.InvalidParameter, data);
        return AIGCStateCode.InvalidParameter;
    }

    /**
     * 按序列号读取单个报告。
     *
     * <p>先查绘画报告，未命中则回退查量表报告（L88-127）。
     * 两者都未命中时回 {@code Failure} 并回显请求体。</p>
     *
     * @param ctx 动作上下文。
     * @param module 心理学模块。
     * @param sn 报告序列号。
     * @param markdown 是否导出 Markdown 正文。
     * @param sections 是否导出分节 JSON。
     * @return 返回状态码。
     */
    private AIGCStateCode querySingle(ActionContext ctx, PsychologyModule module, long sn,
            boolean markdown, boolean sections) {
        String format = markdown ? "markdown" : (sections ? "sections" : "compact");

        JSONObject report = module.queryPaintingReport(ctx.getHost(), sn, format);
        if (null != report) {
            ctx.respond(AIGCStateCode.Ok, report);
            return AIGCStateCode.Ok;
        }

        // 导出过程可能抛异常（如 Markdown 模板缺失），在此回 Failure
        // 并回显请求体（L110-114）；查询侧已把异常收敛为 null，
        // 故这里无法区分「报告不存在」与「导出失败」，两者都回 Failure——
        // 与「导出失败」的应答一致，而「不存在」本就该回 Failure
        Logger.d(GetPsychologyReportAction.class, "#querySingle - No painting report: " + sn);

        JSONObject scaleReport = module.queryScaleReport(ctx.getHost(), sn);
        if (null != scaleReport) {
            ctx.respond(AIGCStateCode.Ok, scaleReport);
            return AIGCStateCode.Ok;
        }

        ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
        return AIGCStateCode.Failure;
    }

    /**
     * 分页读取报告列表。
     *
     * <p>L129-171 按 {@code type} 分绘画与量表两条路径。</p>
     *
     * @param ctx 动作上下文。
     * @param module 心理学模块。
     * @param contactId 联系人 ID。
     * @param pageIndex 页码，从 0 开始。
     * @param pageSize 每页条数。
     * @param descending 是否倒序。
     * @param state 报告状态；{@code -1} 表示不限。
     * @param type 报告类型。
     * @return 返回状态码。
     */
    private AIGCStateCode queryList(ActionContext ctx, PsychologyModule module, long contactId,
            int pageIndex, int pageSize, boolean descending, int state, String type) {
        JSONObject responseData;

        if (TYPE_PAINTING.equalsIgnoreCase(type)) {
            responseData = module.listPaintingReports(ctx.getHost(), contactId, pageIndex, pageSize, descending, state);
            responseData.put("size", pageSize);
        }
        else {
            responseData = module.listScaleReports(ctx.getHost(), contactId, descending, state);
            responseData.put("page", pageIndex);
            // ⚠️ L166 填的是 num（总条数）而非 pageSize。
            // 修正它属于独立的线协议变更，不可在此顺手做
            responseData.put("size", responseData.getInt("total"));
        }

        responseData.put("type", type);
        ctx.respond(AIGCStateCode.Ok, responseData);

        return AIGCStateCode.Ok;
    }
}
