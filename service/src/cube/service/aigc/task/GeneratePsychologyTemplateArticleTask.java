/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.ReportArticle;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Language;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PaintingTemplateArticleListener;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONObject;

public class GeneratePsychologyTemplateArticleTask extends ServiceTask {

    public GeneratePsychologyTemplateArticleTask(Cellet cellet, TalkContext talkContext, Primitive primitive,
                                               ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        final AuthToken authToken = service.getToken(token);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            Attribute attribute = new Attribute(packet.data.getJSONObject("attribute"));
            Theme theme = Theme.parse(packet.data.getString("theme"));
            String templateName = packet.data.getString("templateName");

            String fileCode = packet.data.has("fileCode") ? packet.data.getString("fileCode") : null;
            String fileUrl = packet.data.has("fileUrl") ? packet.data.getString("fileUrl") : null;

            FileLabel fileLabel = null;
            if (null != fileCode) {
                fileLabel = service.getFile(authToken.getDomain(), fileCode);
            }
            else if (null != fileUrl) {
                fileLabel = service.downloadFile(authToken, fileUrl);
                if (null == fileLabel) {
                    Logger.w(this.getClass(), "#run - Download file failed: " + fileUrl);
                }
            }

            if (null == fileLabel) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.NotFound.code, new JSONObject()));
                markResponseTime();
                return;
            }

            AIGCChannel channel = service.getChannelByToken(token);
            if (null == channel) {
                channel = service.createChannel(authToken, "Baize", Utils.randomString(16),
                        Language.Chinese);
            }

            long sn = PsychologyScene.getInstance().generatePaintingTemplateArticle(channel, attribute, fileLabel, theme,
                    templateName, new PaintingTemplateArticleListener() {
                        @Override
                        public void onPaintingPredicted(PaintingReport report) {
                            // Nothing
                        }

                        @Override
                        public void onCompleted(PaintingReport report, ReportArticle article) {
                            // Nothing
                        }

                        @Override
                        public void onFailed(AIGCChannel channel, FileLabel fileLabel) {
                            // Nothing
                        }
                    });

            if (0 != sn) {
                packet.data.put("sn", sn);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, packet.data));
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
            }
            markResponseTime();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
        }
    }
}
