/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.RetrieveReRankResult;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.RetrieveReRankListener;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RetrieveReRankUnitMeta extends UnitMeta {

    private List<String> queries;

    private List<FileLabel> fileLabels;

    private RetrieveReRankListener listener;

    public RetrieveReRankUnitMeta(AIGCService service, AIGCUnit unit, List<String> queries, RetrieveReRankListener listener) {
        super(service, unit);
        this.queries = queries;
        this.listener = listener;
    }

    public RetrieveReRankUnitMeta(AIGCService service, AIGCUnit unit, List<FileLabel> fileLabels, String query,
                                  RetrieveReRankListener listener) {
        super(service, unit);
        this.fileLabels = fileLabels;
        this.queries = new ArrayList<>();
        this.queries.add(query);
        this.listener = listener;
    }

    @Override
    public void process() {
        JSONObject data = new JSONObject();
        if (null != this.fileLabels) {
            data.put("query", this.queries.get(0));
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.fileLabels) {
                array.put(fileLabel.toJSON());
            }
            data.put("files", array);
        }
        else {
            data.put("queries", JSONUtils.toStringArray(this.queries));
        }

        Packet request = new Packet(AIGCAction.RetrieveReRank.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect());
        if (null == dialect) {
            Logger.w(AIGCService.class, "The retrieve re-rank unit error");
            // 回调错误
            this.listener.onFailed(this.queries, AIGCStateCode.UnitError);
            return;
        }

        List<RetrieveReRankResult> list = new ArrayList<>();

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(AIGCService.class, "The retrieve re-rank unit failed: " + Packet.extractCode(response));
            // 回调错误
            this.listener.onFailed(this.queries, AIGCStateCode.Failure);
            return;
        }
        JSONObject payload = Packet.extractDataPayload(response);
        JSONArray resultList = payload.getJSONArray("result");
        for (int i = 0; i < resultList.length(); ++i) {
            RetrieveReRankResult result = new RetrieveReRankResult(resultList.getJSONObject(i));
            list.add(result);
        }

        this.listener.onCompleted(list);
    }
}
