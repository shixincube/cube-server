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
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.SearchResult;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.Explorer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取搜索结果。
 */
public class GetSearchResultsTask extends ServiceTask {

    public GetSearchResultsTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        AuthToken authToken = extractAuthToken(dialect);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        List<SearchResult> searchResultList = Explorer.getInstance().querySearchResults(authToken);
        JSONArray array = new JSONArray();
        for (SearchResult sr : searchResultList) {
            array.put(sr.toJSON());
        }
        JSONObject responsePayload = new JSONObject();
        responsePayload.put("list", array);
        responsePayload.put("total", searchResultList.size());

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responsePayload));
        markResponseTime();
    }
}
