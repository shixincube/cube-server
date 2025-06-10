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
import cube.common.entity.KnowledgeArticle;
import cube.common.entity.KnowledgeScope;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * 更新知识库文章任务。
 */
public class UpdateKnowledgeArticleTask extends ServiceTask {

    public UpdateKnowledgeArticleTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        String baseName = KnowledgeFramework.DefaultName;
        if (packet.data.has("base")) {
            baseName = packet.data.getString("base");
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        KnowledgeBase base = service.getKnowledgeBase(tokenCode, baseName);
        if (null == base) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        KnowledgeArticle article = null;
        try {
            Calendar calendar = Calendar.getInstance();
            int year = packet.data.has("year") ? packet.data.getInt("year") : calendar.get(Calendar.YEAR);
            int month = packet.data.has("month") ? packet.data.getInt("month") : calendar.get(Calendar.MONTH) + 1;
            int date = packet.data.has("date") ? packet.data.getInt("date") : calendar.get(Calendar.DATE);
            long timestamp = packet.data.has("timestamp") ? packet.data.getLong("timestamp") : System.currentTimeMillis();

            KnowledgeScope scope = packet.data.has("scope") ?
                    KnowledgeScope.parse(packet.data.getString("scope")) : KnowledgeScope.Private;

            KnowledgeArticle input = new KnowledgeArticle(packet.data.getLong("id"),
                    base.getAuthToken().getDomain(), base.getAuthToken().getContactId(), baseName,
                    packet.data.getString("category"), packet.data.getString("title"),
                    packet.data.getString("content"), packet.data.getString("summarization"),
                    packet.data.getString("author"), year, month, date, timestamp,
                    scope, packet.data.getBoolean("activated"), packet.data.getInt("numSegments"));
            // 更新知识库文章
            article = base.updateKnowledgeArticle(input);
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (null == article) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, article.toJSON()));
        markResponseTime();
    }
}
