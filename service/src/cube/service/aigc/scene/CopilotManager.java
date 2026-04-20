/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.copilot.CopilotSetting;
import cube.aigc.psychology.copilot.CopilotSheet;
import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CopilotManager {

    private AIGCService service;

    private Map<Long, Copilot> copilotMap;

    private final static CopilotManager instance = new CopilotManager();

    private CopilotManager() {
        this.copilotMap = new ConcurrentHashMap<>();
    }

    public static CopilotManager getInstance() {
        return CopilotManager.instance;
    }

    public void start(AIGCService service) {
        this.service = service;
    }

    public void stop() {

    }

    public CopilotSetting applyCopilot(AuthToken authToken, CopilotSetting copilotSetting) {
        // 移除历史
        this.copilotMap.remove(authToken.getContactId());

        String copilotQuickStrategy = Resource.getInstance().getStrategyContent("copilot_quick_strategy");
        if (null == copilotQuickStrategy) {
            Logger.w(this.getClass(), "#applyCopilot - No copilot quick strategy: " + authToken.getContactId());
            return null;
        }

        CopilotSetting setting = new CopilotSetting(copilotSetting.personalityTrait,
                copilotSetting.attachmentType, copilotSetting.culturalBackground,
                copilotSetting.chiefComplaintType, copilotSetting.painLevel,
                copilotSetting.defenseMechanism, copilotSetting.empathy, copilotSetting.speechStyle,
                copilotSetting.hiddenAgendaModel, copilotSetting.multipleRolesModel, copilotSetting.ethicalTrapModel);

        copilotQuickStrategy = copilotQuickStrategy.replace("{{setting}}", setting.toMarkdown());

        Copilot copilot = new Copilot(authToken, setting);
        this.copilotMap.put(authToken.getContactId(), copilot);

        GeneratingRecord record = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, copilotQuickStrategy,
                null, null, null);
        if (null != record) {
            setting.setStrategyTemplate(filter(record.answer));
        }

        return setting;
    }

    public Copilot disposeCopilot(AuthToken authToken, long sn) {
        Copilot copilot = this.copilotMap.remove(authToken.getContactId());
        if (null == copilot) {
            Logger.w(this.getClass(), "#disposeCopilot - Can NOT find copilot: " + authToken.getContactId());
            return null;
        }

        // TODO
        return copilot;
    }

    public void submitContent(AuthToken authToken, CopilotSheet sheet) {

    }

    private String filter(String text) {
        StringBuilder buf = new StringBuilder();
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.length() == 0) {
                buf.append("\n");
                continue;
            }
            int start = line.indexOf("（");
            if (start > 0) {
                if (start < 6) {
                    int end = line.indexOf("）");
                    line = line.substring(0, start) + line.substring(end + 1).trim();
                    start = line.indexOf("（");
                    if (start > 0) {
                        line = line.substring(0, start).trim();
                    }
                }
                else {
                    line = line.substring(0, start).trim();
                }
            }
            else {
                line = line.trim();
            }

            // 提取来访者的话术
            if (line.startsWith("来访者")) {
                buf.append(line);
                buf.append("\n");
            }
        }

        return buf.toString();
    }

    public class Copilot implements JSONable {

        public final long sn;

        public final long timestamp;

        public AuthToken authToken;

        public CopilotSetting copilotSetting;

        public List<CopilotSheet> records;

        public long duration;

        public Copilot(AuthToken authToken, CopilotSetting copilotSetting) {
            this.sn = copilotSetting.getSn();
            this.timestamp = System.currentTimeMillis();
            this.authToken = authToken;
            this.copilotSetting = copilotSetting;
            this.records = new ArrayList<>();
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = this.toCompactJSON();
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            JSONObject json = new JSONObject();
            json.put("sn", this.sn);
            json.put("timestamp", this.timestamp);
            json.put("duration", this.duration);
            json.put("setting", this.copilotSetting.toCompactJSON());
            return json;
        }
    }
}
