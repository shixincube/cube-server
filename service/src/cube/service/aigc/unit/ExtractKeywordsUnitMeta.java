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
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ExtractKeywordsListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExtractKeywordsUnitMeta extends UnitMeta {

    protected String text;

    protected ExtractKeywordsListener listener;

    public ExtractKeywordsUnitMeta(AIGCService service, AIGCUnit unit, String text, ExtractKeywordsListener listener) {
        super(service, unit);
        this.text = text;
        this.listener = listener;
    }

    @Override
    public void process() {
        JSONObject data = new JSONObject();
        data.put("text", this.text);
        Packet request = new Packet(AIGCAction.ExtractKeywords.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(AIGCService.class, "Extract keywords unit error");
            // 回调错误
            this.listener.onFailed(this.text, AIGCStateCode.UnitError);
            return;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);
        if (payload.has("words")) {
            JSONArray array = payload.getJSONArray("words");
            List<String> words = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); ++i) {
                String word = array.getString(i).replaceAll("\n", "");
                if (word.length() == 0) {
                    continue;
                }
                words.add(word);
            }

            if (words.isEmpty()) {
                this.listener.onFailed(this.text, AIGCStateCode.NoData);
            }
            else {
                this.listener.onCompleted(this.text, words);
            }
        }
        else {
            this.listener.onFailed(this.text, AIGCStateCode.DataStructureError);
        }
    }
}
