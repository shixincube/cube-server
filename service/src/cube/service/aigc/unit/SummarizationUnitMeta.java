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
import cube.service.aigc.listener.SummarizationListener;
import org.json.JSONObject;

public class SummarizationUnitMeta extends UnitMeta {

    protected String text;

    protected SummarizationListener listener;

    public SummarizationUnitMeta(AIGCService service, AIGCUnit unit, String text, SummarizationListener listener) {
        super(service, unit);
        this.text = text;
        this.listener = listener;
    }

    @Override
    public void process() {
        JSONObject data = new JSONObject();
        data.put("text", this.text);

        Packet request = new Packet(AIGCAction.Summarization.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(AIGCService.class, "Summarization unit error");
            // 回调错误
            this.listener.onFailed(this.text, AIGCStateCode.UnitError);
            return;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);
        if (payload.has("summarization")) {
            this.listener.onCompleted(this.text, payload.getString("summarization"));
        }
        else {
            Logger.w(AIGCService.class, "Summarization unit return error");
            this.listener.onFailed(this.text, AIGCStateCode.NoData);
        }
    }
}
