/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.listener.PaintingReportListener;
import cube.aigc.psychology.listener.ScaleReportListener;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 生成心理学报告动作。
 *
 * <p>对应线协议动作 {@code generatePsychologyReport}，逐字符等同于既有枚举
 * {@code AIGCAction.GeneratePsychologyReport} 的 {@code name} 字段。</p>
 *
 * <p><b>三分支结构</b>（迁移前 {@code GeneratePsychologyReportTask} L60-183）：</p>
 * <ul>
 *   <li>{@code fileCode + theme + attribute}三者齐全 → 绘画报告；</li>
 *   <li>否则若有 {@code scaleSn} → 量表报告；</li>
 *   <li>否则 → {@code InvalidParameter}。</li>
 * </ul>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code NoToken → InvalidParameter → Ok / Failure}。</p>
 *
 * <p><b>⚠️ 令牌无效回 {@code NoToken} 而非 {@code InconsistentToken}</b>：迁移前
 * L52-57 是「令牌解析为 null → 回 NoToken」，并非校验有效性。因此
 * {@code requiresToken} 取 {@code false}，交由本处理器自判——若交由骨架前置校验，
 * 应答码会变成 {@code InconsistentToken}，属线协议可见变更。</p>
 *
 * <p><b>⚠️ 读参数一律用 {@code ctx.getRequest().data}</b>：迁移前用的是
 * {@code packet.data}，即<b>原样透传的请求体</b>。若改用 {@code ctx.getParams()}
 * （无 data 时返回空对象），三分支的 {@code has} 判定会全部变false，
 * 从而把「参数缺失」从 {@code InvalidParameter} 变成走到最后 else——
 * 看似等价实则改变分支归属，须避免。</p>
 */
public final class GeneratePsychologyReportAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String token = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == token) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        AuthToken authToken = ctx.getHost().resolveToken(token);
        if (null == authToken) {
            // 迁移前 L52-57：令牌解析为 null 也回 NoToken
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        JSONObject data = ctx.getRequest().data;

        if (data.has("fileCode") && data.has("theme") && data.has("attribute")) {
            return this.generatePainting(ctx, data, token, authToken);
        }
        else if (data.has("scaleSn")) {
            return this.generateScale(ctx, data, token, authToken);
        }
        else {
            // 迁移前 L180：回空对象而非请求体
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }

    /**
     * 绘画报告分支。
     *
     * @param ctx动作上下文。
     * @param data 请求体。
     * @param token 令牌码，用于日志。
     * @param authToken 已解析的访问令牌。
     * @return 返回状态码。
     */
    private AIGCStateCode generatePainting(ActionContext ctx, JSONObject data, String token,
            AuthToken authToken) {
        Attribute attribute;
        String fileCode;
        Theme theme;
        int maxIndicators;
        boolean adjust;
        String remark;

        try {
            attribute = new Attribute(data.getJSONObject("attribute"));
            fileCode = data.getString("fileCode");
            theme = Theme.parse(data.getString("theme"));
            //⚠️ 迁移前的默认值：indicators 缺省30、adjust 缺省 true
            //（尽管 L64 声明的局部变量初值是 10/true，随后立即被覆盖）
            maxIndicators = data.has("indicators") ? data.getInt("indicators") : 30;
            adjust = data.has("adjust") ? data.getBoolean("adjust") : true;
            remark = data.has("remark") ? data.getString("remark") : null;
        } catch (Exception e) {
            // 迁移前 L77-82：解析异常回空对象
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        if (null == theme) {
            // 迁移前 L84-90
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        PaintingReport report = new GeneratePsychologyReportAction.ReportBuilder(ctx, token, authToken)
                .build(fileCode, attribute, theme, maxIndicators, adjust, remark);

        if (null != report) {
            ctx.respond(AIGCStateCode.Ok, report.toJSON());
        }
        else {
            // 迁移前 L131：失败回显原始请求体
            ctx.respond(AIGCStateCode.Failure, data);
        }

        return AIGCStateCode.Ok;
    }

    /**
     * 量表报告分支。
     *
     * @param ctx 动作上下文。
     * @param data 请求体。
     * @param token 令牌码，用于日志。
     * @param authToken 已解析的访问令牌。
     * @return 返回状态码。
     */
    private AIGCStateCode generateScale(ActionContext ctx, JSONObject data, String token,
            AuthToken authToken) {
        long scaleSn;
        Language language;

        try {
            scaleSn = data.getLong("scaleSn");
            language = data.has("language")
                    ? Language.parse(data.getString("language")) : Language.Chinese;
        } catch (Exception e) {
            // 迁移前 L145-150
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        Scale scale = ctx.getHost().getScale(scaleSn);
        if (null == scale) {
            // 与迁移前一致：AIGCService#generateScaleReport 在取不到量表时返回 null，
            // Task 侧落入 Failure + 回显请求体
            ctx.respond(AIGCStateCode.Failure, data);
            return AIGCStateCode.Failure;
        }

        AIGCChannel channel = ctx.getHost().getChannelByToken(token);
        if (null == channel) {
            // 迁移前 createChannel(authToken, "Baize", random(16), language)
            channel = ctx.getHost().acquireChannel(authToken, "Baize", null, language);
        }

        ScaleReport report = ctx.getHost().generateScaleReport(channel, scale, language,
                new LoggingScaleListener(token));

        if (null != report) {
            ctx.respond(AIGCStateCode.Ok, report.toJSON());
        }
        else {
            ctx.respond(AIGCStateCode.Failure, data);
        }

        return AIGCStateCode.Ok;
    }

    /**
     * 绘画报告的取文件 + 组频道 + 提交编排。
     *
     * <p>迁移前这一段在 {@code AIGCService#generatePaintingReport} 内：解析令牌 →
     * 取文件标签 → 取或建频道 → 提交场景。本类拆成独立小类只为让
     * {@link #generatePainting} 保持可读，逻辑与迁移前逐句一致。</p>
     */
    private static final class ReportBuilder {

        private final ActionContext ctx;
        private final String token;
        private final AuthToken authToken;

        private ReportBuilder(ActionContext ctx, String token, AuthToken authToken) {
            this.ctx = ctx;
            this.token = token;
            this.authToken = authToken;
        }

        private PaintingReport build(String fileCode, Attribute attribute, Theme theme, int maxIndicators,
                boolean adjust, String remark) {
            if (!this.ctx.getHost().isReady()) {
                // 迁移前 AIGCService L2325：服务未启动
                Logger.w(GeneratePsychologyReportAction.class,
                        "#generatePaintingReport - The service has NOT started");
                return null;
            }

            // 迁移前 L2337-2355：取文件标签。
            // host.getFile(domain, fileCode) 与迁移前的
            //「fileStorage.notify(new GetFile(...)) + new FileLabel(json)」完全等价
            //（已核对 AIGCService#getFile 实现体一致），故此处直接调用
            FileLabel fileLabel = this.ctx.getHost().getFile(this.authToken.getDomain(), fileCode);
            if (null == fileLabel) {
                Logger.e(GeneratePsychologyReportAction.class,
                        "#generatePaintingReport - Get file failed: " + fileCode);
                return null;
            }

            AIGCChannel channel = this.ctx.getHost().getChannelByToken(this.token);
            if (null == channel) {
                // 迁移前 createChannel(authToken, "Baize", random(16), attribute.language)
                // channelCode 传 null 由宿主生成随机码，与迁移前等价
                channel = this.ctx.getHost().acquireChannel(this.authToken, "Baize", null, attribute.language);
            }

            return this.ctx.getHost().generatePaintingReport(channel, attribute, fileLabel, theme,
                    maxIndicators, adjust, 0, remark, new LoggingPaintingListener(this.token));
        }
    }

    /**
     * 绘画报告事件监听器。
     *
     * <p>迁移前是 {@code GeneratePsychologyReportTask} 内的匿名类，六个方法
     * <b>全部只打 debug 日志、无业务逻辑</b>（生成是异步的，此处仅记录事件）。
     * 此处逐方法原样复刻。</p>
     */
    private static final class LoggingPaintingListener implements PaintingReportListener {

        private final String token;

        private LoggingPaintingListener(String token) {
            this.token = token;
        }

        @Override
        public void onPaintingPredicting(PaintingReport report, FileLabel file) {
            Logger.d(GeneratePsychologyReportAction.class, "#onPaintingPredicting - " + this.token);
        }

        @Override
        public void onPaintingPredictCompleted(PaintingReport report, FileLabel file, Painting painting) {
            Logger.d(GeneratePsychologyReportAction.class, "#onPaintingPredictCompleted - " + this.token);
        }

        @Override
        public void onPaintingPredictFailed(PaintingReport report) {
            Logger.d(GeneratePsychologyReportAction.class, "#onPaintingPredictFailed - " + this.token);
        }

        @Override
        public void onReportEvaluating(PaintingReport report) {
            Logger.d(GeneratePsychologyReportAction.class, "#onReportEvaluating - " + this.token);
        }

        @Override
        public void onReportEvaluateCompleted(PaintingReport report, AIGCUnit unit) {
            Logger.d(GeneratePsychologyReportAction.class, "#onReportEvaluateCompleted - " + this.token);
        }

        @Override
        public void onReportEvaluateFailed(PaintingReport report) {
            Logger.d(GeneratePsychologyReportAction.class, "#onReportEvaluateFailed - " + this.token);
        }
    }

    /**
     * 量表报告事件监听器。
     *
     * <p>迁移前是匿名类，三个方法同样只打 debug 日志。此处逐方法原样复刻。</p>
     */
    private static final class LoggingScaleListener implements ScaleReportListener {

        private final String token;

        private LoggingScaleListener(String token) {
            this.token = token;
        }

        @Override
        public void onReportEvaluating(ScaleReport report) {
            Logger.d(GeneratePsychologyReportAction.class, "#onReportEvaluating - " + this.token);
        }

        @Override
        public void onReportEvaluateCompleted(ScaleReport report) {
            Logger.d(GeneratePsychologyReportAction.class, "#onReportEvaluateCompleted - " + this.token);
        }

        @Override
        public void onReportEvaluateFailed(ScaleReport report) {
            Logger.d(GeneratePsychologyReportAction.class, "#onReportEvaluateFailed - " + this.token);
        }
    }
}
