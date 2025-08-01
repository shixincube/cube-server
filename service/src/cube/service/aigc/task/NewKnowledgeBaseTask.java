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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.KnowledgeBaseInfo;
import cube.common.entity.KnowledgeScope;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.util.TextUtils;
import org.json.JSONObject;

/**
 * 创建知识库任务。
 */
public class NewKnowledgeBaseTask extends ServiceTask {

    public NewKnowledgeBaseTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String baseName = null;
        String displayName = null;
        String category = null;
        KnowledgeScope scope = KnowledgeScope.Private;
        try {
            baseName = packet.data.getString("name");
            displayName = packet.data.getString("displayName");

            if (packet.data.has("category")) {
                category = packet.data.getString("category");
                if (TextUtils.isBlank(category)) {
                    category = null;
                }
            }

            if (packet.data.has("scope")) {
                scope = KnowledgeScope.parse(packet.data.getString("scope"));
            }

            if (null == category) {
                category = displayName;
            }

            if (TextUtils.isBlank(baseName) || TextUtils.isBlank(displayName)) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
                markResponseTime();
                return;
            }
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        KnowledgeBaseInfo info = service.getKnowledgeFramework()
                .newKnowledgeBase(tokenCode, baseName, displayName, category, scope);
        if (null == info) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, info.toJSON()));
        markResponseTime();
    }
}
