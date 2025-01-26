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
import cube.aigc.ConfigInfo;
import cube.aigc.ContactPreference;
import cube.aigc.ModelConfig;
import cube.aigc.Notification;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取配置任务。
 */
public class GetConfigTask extends ServiceTask {

    public GetConfigTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (!packet.data.has("token")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String tokenCode = packet.data.getString("token");

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken token = service.getToken(tokenCode);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        List<Notification> notifications = service.getNotifications();
        if (null == notifications) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        // 个人偏好配置
        ContactPreference preference = service.getPreference(token.getContactId());

        List<ModelConfig> models = null;
        if (null == preference || preference.getModels().length() == 0) {
            models = service.getModelConfigs();
        }
        else {
            models = service.getModelConfigs(preference.getModels());
        }

        if (null == models) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        ConfigInfo configInfo = new ConfigInfo(models, notifications, preference);

        // 用量
        configInfo.usages = service.queryContactUsages(token.getContactId());

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, configInfo.toJSON()));
        markResponseTime();
    }
}
