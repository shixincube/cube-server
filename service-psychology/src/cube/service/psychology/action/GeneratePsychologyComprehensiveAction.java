/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.Comprehensive;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.ComprehensiveReportListener;
import cube.service.psychology.scene.PsychologyScene;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成心理融合评测动作。
 *
 * <p>对应线协议动作 {@code generatePsychologyComprehensive}，逐字符等同于既有枚举
 * {@code AIGCAction.GeneratePsychologyComprehensive} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → InvalidParameter / Failure / Ok / IllegalOperation}。
 * 其中「无 token 参数」与「令牌解析不出」<b>都回 NoToken</b>；
 * {@code theme} 或 {@code comprehensives} 缺失时回 {@code InvalidParameter}。</p>
 */
public final class GeneratePsychologyComprehensiveAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        final AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
        if (null == authToken) {
            // ⚠️ 与「无 token 参数」同回 NoToken（宿主原行为）
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        try {
            if (!ctx.getRequest().data.has("theme")
                    || !ctx.getRequest().data.has("comprehensives")) {
                ctx.respondEmpty(AIGCStateCode.InvalidParameter);
                return AIGCStateCode.InvalidParameter;
            }

            Theme theme = Theme.parse(ctx.getRequest().data.getString("theme"));

            List<Comprehensive> comprehensives = new ArrayList<>();
            JSONArray array = ctx.getRequest().data.getJSONArray("comprehensives");
            for (int i = 0; i < array.length(); ++i) {
                comprehensives.add(new Comprehensive(array.getJSONObject(i)));
            }

            AIGCChannel channel = ctx.getHost().getChannelByToken(tokenCode);
            if (null == channel) {
                channel = ctx.getHost().createChannel(authToken, "Baize",
                        Utils.randomString(16), Language.Chinese);
            }

            ComprehensiveReport report = PsychologyScene.getInstance().generateComprehensive(channel, theme,
                    comprehensives, new ComprehensiveReportListener() {
                        @Override
                        public void onPredicting(ComprehensiveReport report, Comprehensive comprehensive) {
                            Logger.d(GeneratePsychologyComprehensiveAction.class,
                                    "#onPredicting - " + report.sn);
                        }

                        @Override
                        public void onEvaluating(ComprehensiveReport report) {
                            Logger.d(GeneratePsychologyComprehensiveAction.class,
                                    "#onEvaluating - " + report.sn);
                        }

                        @Override
                        public void onEvaluateCompleted(ComprehensiveReport report) {
                            Logger.d(GeneratePsychologyComprehensiveAction.class,
                                    "#onEvaluateCompleted - " + report.sn);
                        }

                        @Override
                        public void onEvaluateFailed(ComprehensiveReport report) {
                            Logger.d(GeneratePsychologyComprehensiveAction.class,
                                    "#onEvaluateFailed - " + report.sn);
                        }
                    });

            if (null != report) {
                ctx.respond(AIGCStateCode.Ok, report.toJSON());
                return AIGCStateCode.Ok;
            }

            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#handle", e);
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }
    }
}
