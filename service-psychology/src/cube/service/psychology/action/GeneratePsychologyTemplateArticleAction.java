/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Theme;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.PaintingTemplateArticleListener;
import cube.service.psychology.scene.PsychologyScene;
import cube.service.psychology.scene.TemplateArticleBuilder;
import org.json.JSONObject;

/**
 * 生成模板文章动作。
 *
 * <p>对应线协议动作 {@code generatePsychologyTemplateArticle}，逐字符等同于既有枚举
 * {@code AIGCAction.GeneratePsychologyTemplateArticle} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → InvalidParameter → NotFound / Ok / Failure / IllegalOperation}。
 * 注意「无 token 参数」与「令牌解析不出」分别回 {@code NoToken} 与
 * {@code InvalidParameter}，二者不同，勿统一。</p>
 *
 * <p><b>成功应答回显整个请求体</b>并补 {@code sn} 与 {@code fileCode}，
 * 失败时也回显请求体（仅 {@code NotFound} 与 {@code IllegalOperation} 回空对象）。</p>
 */
public final class GeneratePsychologyTemplateArticleAction implements AIGCActionTask {

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
            // ⚠️ 此处回 InvalidParameter 而非 NoToken（宿主原行为），勿与
            //    GetPsychologyTemplateArticleAction 统一
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        try {
            Attribute attribute = new Attribute(ctx.getRequest().data.getJSONObject("attribute"));
            Theme theme = Theme.parse(ctx.getRequest().data.getString("theme"));
            String templateName = ctx.getRequest().data.getString("templateName");
            boolean structured = ctx.getRequest().data.has("structured")
                    && ctx.getRequest().data.getBoolean("structured");

            String fileCode = ctx.getRequest().data.has("fileCode")
                    ? ctx.getRequest().data.getString("fileCode") : null;
            String fileUrl = ctx.getRequest().data.has("fileUrl")
                    ? ctx.getRequest().data.getString("fileUrl") : null;

            FileLabel fileLabel = null;
            if (null != fileCode) {
                fileLabel = ctx.getHost().getFile(authToken.getDomain(), fileCode);
            }
            else if (null != fileUrl) {
                fileLabel = ctx.getHost().downloadFile(authToken, fileUrl);
                if (null == fileLabel) {
                    Logger.w(this.getClass(), "#handle - Download file failed: " + fileUrl);
                }
            }

            if (null == fileLabel) {
                ctx.respondEmpty(AIGCStateCode.NotFound);
                return AIGCStateCode.NotFound;
            }

            AIGCChannel channel = ctx.getHost().getChannelByToken(tokenCode);
            if (null == channel) {
                channel = ctx.getHost().createChannel(authToken, "Baize",
                        Utils.randomString(16), Language.Chinese);
            }

            TemplateArticleBuilder builder = PsychologyScene.getInstance().generatePaintingTemplateArticle(
                    channel, attribute, fileLabel, theme, templateName, structured,
                    new PaintingTemplateArticleListener() {
                        @Override
                        public void onPaintingPredicted(PaintingReport report) {
                            // Nothing
                        }

                        @Override
                        public void onCompleted(PaintingReport report,
                                cube.aigc.psychology.composition.ReportArticle article) {
                            // Nothing
                        }

                        @Override
                        public void onFailed(AIGCChannel channel, FileLabel fileLabel) {
                            // Nothing
                        }
                    });

            JSONObject data = ctx.getRequest().data;
            if (null != builder) {
                data.put("sn", builder.getArticle().sn);
                data.put("fileCode", builder.getFileCode());
                ctx.respond(AIGCStateCode.Ok, data);
                return AIGCStateCode.Ok;
            }

            ctx.respond(AIGCStateCode.Failure, data);
            return AIGCStateCode.Failure;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#handle", e);
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }
    }
}
