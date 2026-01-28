/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ConsultationTheme;
import cube.aigc.psychology.Attribute;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.CounselingStrategy;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.CounselingManager;
import org.json.JSONObject;

/**
 * 查询咨询提示性说明。
 */
public class QueryCounselingCaptionTask extends ServiceTask {

    public QueryCounselingCaptionTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token || !packet.data.has("theme") || !packet.data.has("attribute")
            || !packet.data.has("streamName")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            AIGCService service = ((AIGCCellet) this.cellet).getService();

            AuthToken authToken = service.getToken(token);
            ConsultationTheme theme = ConsultationTheme.parse(packet.data.getString("theme"));
            Attribute attribute = new Attribute(packet.data.getJSONObject("attribute"));
            String streamName = packet.data.getString("streamName");
            int index = packet.data.getInt("index");

            CounselingStrategy result = CounselingManager.getInstance().queryCounselingCaption(authToken,
                    theme, attribute, streamName, index);
            if (null == result) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.NoData.code, new JSONObject()));
                markResponseTime();
                return;
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, result.toJSON()));
            markResponseTime();
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
    }
}
