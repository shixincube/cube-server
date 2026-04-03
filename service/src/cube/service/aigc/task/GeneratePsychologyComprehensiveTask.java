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
import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.Comprehensive;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Language;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.ComprehensiveReportListener;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成心理融合评测任务。
 */
public class GeneratePsychologyComprehensiveTask extends ServiceTask {

    public GeneratePsychologyComprehensiveTask(Cellet cellet, TalkContext talkContext, Primitive primitive,
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
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            if (packet.data.has("theme") && packet.data.has("comprehensives")) {
                Theme theme = Theme.parse(packet.data.getString("theme"));

                List<Comprehensive> comprehensives = new ArrayList<>();
                JSONArray array = packet.data.getJSONArray("comprehensives");
                for (int i = 0; i < array.length(); ++i) {
                    Comprehensive comprehensive = new Comprehensive(array.getJSONObject(i));
                    comprehensives.add(comprehensive);
                }

                AIGCChannel channel = service.getChannelByToken(token);
                if (null == channel) {
                    channel = service.createChannel(authToken, "Baize", Utils.randomString(16),
                            Language.Chinese);
                }

                ComprehensiveReport report = PsychologyScene.getInstance().generateComprehensive(channel, theme,
                        comprehensives, new ComprehensiveReportListener() {
                            @Override
                            public void onPredicting(ComprehensiveReport report, Comprehensive comprehensive) {
                                Logger.d(GeneratePsychologyComprehensiveTask.class, "#onPredicting - " + report.sn);
                            }

                            @Override
                            public void onEvaluating(ComprehensiveReport report) {
                                Logger.d(GeneratePsychologyComprehensiveTask.class, "#onEvaluating - " + report.sn);
                            }

                            @Override
                            public void onEvaluateCompleted(ComprehensiveReport report) {
                                Logger.d(GeneratePsychologyComprehensiveTask.class, "#onEvaluateCompleted - " + report.sn);
                            }

                            @Override
                            public void onEvaluateFailed(ComprehensiveReport report) {
                                Logger.d(GeneratePsychologyComprehensiveTask.class, "#onEvaluateFailed - " + report.sn);
                            }
                        });

                if (null != report) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, report.toJSON()));
                }
                else {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                }
                markResponseTime();
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
                markResponseTime();
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
        }
    }
}
