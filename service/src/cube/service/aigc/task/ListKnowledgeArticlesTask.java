/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.KnowledgeArticle;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取知识库文档列表任务。
 */
public class ListKnowledgeArticlesTask extends ServiceTask {

    public ListKnowledgeArticlesTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        long start = 0;
        long end = 0;
        boolean activated = false;
        long articleId = 0;
        try {
            if (packet.data.has("start")) {
                start = packet.data.getLong("start");
            }
            if (packet.data.has("end")) {
                end = packet.data.getLong("end");
            }
            if (packet.data.has("activated")) {
                activated = packet.data.getBoolean("activated");
            }
            if (packet.data.has("articleId")) {
                articleId = packet.data.getLong("articleId");
            }
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONObject responsePayload = new JSONObject();

        if (0 != articleId) {
            KnowledgeArticle article = base.getKnowledgeArticle(articleId);
            if (null == article) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }

            JSONArray array = new JSONArray();
            array.put(article.toJSON());

            responsePayload.put("total", array.length());
            responsePayload.put("list", array);
        }
        else {
            List<KnowledgeArticle> articleList = base.getKnowledgeArticles(start, end, activated);
            if (null == articleList) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }

            int size = 20;
            if (articleList.size() > size) {
                int page = packet.data.has("page") ? packet.data.getInt("page") : 0;
                JSONArray array = new JSONArray();
                for (int i = page * size; i < articleList.size(); ++i) {
                    array.put(articleList.get(i).toCompactJSON());
                    if (array.length() >= size) {
                        break;
                    }
                }

                responsePayload.put("page", page);
                responsePayload.put("total", articleList.size());
                responsePayload.put("list", array);
            }
            else {
                JSONArray array = new JSONArray();
                for (KnowledgeArticle article : articleList) {
                    array.put(article.toCompactJSON());
                }

                responsePayload.put("total", articleList.size());
                responsePayload.put("list", array);
            }
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responsePayload));
        markResponseTime();
    }
}
