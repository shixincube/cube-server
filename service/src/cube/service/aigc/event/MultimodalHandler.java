/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.event;

import cell.util.log.Logger;
import cube.aigc.Usage;
import cube.service.aigc.AIGCService;
import cube.service.aigc.unit.MultimodalUnitMeta;
import cube.service.aigc.unit.UnitMeta;
import org.json.JSONObject;

public class MultimodalHandler implements EventListener {

    private final AIGCService service;

    public MultimodalHandler(AIGCService service) {
        this.service = service;
    }

    @Override
    public void onEvent(EventCenter center, Event event) {
        if (Events.OmniVLSegment.equalsIgnoreCase(event.name)) {
            // 分段内容
            JSONObject payload = event.payload;
            /* JSON 结构
            {
                "sequence": 5,
                "performance": {
                    "elapsed": "5581",
                    "inputTokens": 2526,
                    "outputTokens": 9
                },
                "streamId": "4a605e54-e7d0-4b00-af4d-7c6764e1335a",
                "response": "无明显变化",
                "timestamp": 1789131476537,
                "timeRange": {
                    "endOffset": 53.594,
                    "startOffset": 45.594,
                    "endTimestamp": 1789131470677,
                    "startTimestamp": 1789131462677
                }
            }
            */

            String id = payload.getString("streamId");
            UnitMeta unitMeta = center.getUnitMeta(id);
            if (null != unitMeta) {
                MultimodalUnitMeta multimodalUnitMeta = (MultimodalUnitMeta) unitMeta;
                Usage usage = new Usage(payload.getJSONObject("performance"));
                Logger.d(MultimodalUnitMeta.class, "Update token usage: " +
                        usage.inputTokens + "/" + usage.outputTokens);
                this.service.getStorage().updateUsage(multimodalUnitMeta.getChannel().getAuthToken().getContactId(),
                        multimodalUnitMeta.unit.getCapability().getName(),
                        usage.outputTokens,
                        usage.inputTokens);
            }
        }
    }
}
