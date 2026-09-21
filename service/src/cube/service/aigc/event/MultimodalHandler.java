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
        try {
            if (Events.StreamStarted.equalsIgnoreCase(event.name)) {
                // Nothing
            }
            else if (Events.StreamStopped.equalsIgnoreCase(event.name)) {
                JSONObject payload = event.payload;
                String id = payload.getString("streamId");
                // 移除
                EventCenter.getInstance().removeUnitMeta(id);
            }
            else if (Events.Segment.equalsIgnoreCase(event.name)) {
                // 分段内容
                JSONObject payload = event.payload;
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
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);
        }
    }
}
