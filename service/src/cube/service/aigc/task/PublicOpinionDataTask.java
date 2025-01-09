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
import cube.aigc.Sentiment;
import cube.aigc.opinion.Article;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.module.Module;
import cube.service.aigc.module.ModuleManager;
import cube.service.aigc.module.PublicOpinion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 舆情模块数据任务。
 */
public class PublicOpinionDataTask extends ServiceTask {

    public final static String ACTION_ADD_ARTICLE = "addArticle";

    public final static String ACTION_REMOVE_ARTICLE = "removeArticle";

    public final static String ACTION_GET_ARTICLES = "getArticles";

    public PublicOpinionDataTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        AuthToken authToken = extractAuthToken(dialect);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String action = null;
        String category = null;
        String sentiment = null;
        Article article = null;
        String title = null;
        try {
            action = packet.data.getString("action");

            if (ACTION_ADD_ARTICLE.equalsIgnoreCase(action)) {
                category = packet.data.getString("category");
                sentiment = packet.data.has("sentiment") ?
                        packet.data.getString("sentiment") : Sentiment.Undefined.code;
                JSONObject articleJson = packet.data.getJSONObject("article");
                article = new Article(articleJson);
            }
            else if (ACTION_REMOVE_ARTICLE.equalsIgnoreCase(action)) {
                category = packet.data.getString("category");
                title = packet.data.getString("title");
            }
            else if (ACTION_GET_ARTICLES.equalsIgnoreCase(action)) {
                category = packet.data.getString("category");
            }
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        Module module = ModuleManager.getInstance().getModule("PublicOpinion");
        if (null == module) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NotFound.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONObject responseData = new JSONObject();

        PublicOpinion publicOpinion = (PublicOpinion) module;
        if (ACTION_ADD_ARTICLE.equalsIgnoreCase(action)) {
            long id = publicOpinion.addArticle(category, sentiment, article);
            responseData.put("id", id);
        }
        else if (ACTION_REMOVE_ARTICLE.equalsIgnoreCase(action)) {
            int total = publicOpinion.removeArticle(category, title);
            responseData.put("total", total);
        }
        else if (ACTION_GET_ARTICLES.equalsIgnoreCase(action)) {
            List<Article> list = publicOpinion.getArticleList(category);
            JSONArray array = new JSONArray();
            for (Article art : list) {
                array.put(art.toCompactJSON());
            }

            responseData.put("total", array.length());
            responseData.put("list", array);
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
