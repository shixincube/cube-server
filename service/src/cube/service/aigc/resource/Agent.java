/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.resource;

import cell.core.talk.TalkContext;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.auth.AuthConsts;
import cube.common.entity.*;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 代理访问。
 */
public final class Agent {

    private static Agent instance = null;

    private final String url;

    private String token;

    private String channelCode;

    private List<AIGCUnit> unitList;

    public final static Agent getInstance() {
        return Agent.instance;
    }

    public final static Agent createInstance(String url, String token) {
        Agent.instance = new Agent(url, token);
        return Agent.instance;
    }

    private Agent(String url, String token) {
        this.url = url.endsWith("/") ? url : url + "/";
        this.token = token;
        this.channelCode = Utils.randomString(16);

        this.unitList = new ArrayList<>();
        String[] models = new String[] {
                ModelConfig.BAIZE_UNIT,
                ModelConfig.BAIZE_X_UNIT,
                ModelConfig.BAIZE_NEXT_UNIT,
        };
        Contact contact = new Contact(100000, AuthConsts.DEFAULT_DOMAIN);
        for (String model : models) {
            List<String> subtasks = new ArrayList<>();
            subtasks.add(AICapability.NaturalLanguageProcessing.Conversational);
            AICapability capability = new AICapability(model,
                    AICapability.NaturalLanguageProcessingTask, subtasks, "");
            TalkContext context = new TalkContext(null, null);
            AIGCUnit unit = new AIGCUnit(contact, capability, context);
            this.unitList.add(unit);
        }
    }

    public void fillUnits(Map<String, AIGCUnit> map) {
        for (AIGCUnit unit : this.unitList) {
            map.put(unit.getQueryKey(), unit);
        }
    }

    public AIGCUnit selectUnit(String unitName) {
        for (AIGCUnit unit : this.unitList) {
            if (unit.getCapability().getName().equalsIgnoreCase(unitName)) {
                return unit;
            }
        }
        return null;
    }

    public String getUrl() {
        return this.url;
    }

    public GeneratingRecord generateText(String channelCode, String content, List<GeneratingRecord> records) {
        return this.generateText(channelCode, ModelConfig.BAIZE_NEXT_UNIT, content, new GeneratingOption(), records);
    }

    public GeneratingRecord generateText(String channelCode, String unitName, String content, GeneratingOption option,
                               List<GeneratingRecord> records) {
        String code = channelCode;
        if (null == code) {
            code = this.channelCode;
        }

        HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();

        try {
            JSONArray recordArray = new JSONArray();
            if (null != records) {
                for (GeneratingRecord record : records) {
                    recordArray.put(record.toJSON());
                }
            }

            String chatUrl = this.url + "aigc/chat/" + this.token + "/";

            Logger.d(this.getClass(), "#generateText - query:\n" + content);

            JSONObject data = new JSONObject();
            data.put("code", code);
            data.put("content", content);
            data.put("unit", unitName);
            data.put("option", (null == option) ? (new GeneratingOption()).toJSON() : option.toJSON());
            data.put("histories", 0);
            data.put("pattern", Consts.PATTERN_CHAT);
            data.put("recordable", false);
            data.put("records", recordArray);

            client.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(chatUrl).timeout(5, TimeUnit.MINUTES).content(provider).send();

            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject responseData = new JSONObject(response.getContentAsString());
                if (responseData.has("content")) {
                    Logger.d(this.getClass(), "#generateText - result:\n" + responseData.getString("content"));

                    String thought = responseData.has("thought") ? responseData.getString("thought") : "";
                    return new GeneratingRecord(Utils.generateSerialNumber(), unitName,
                            content, responseData.getString("content"), thought);
                }
                else {
                    Logger.e(this.getClass(), "#generateText - Response data has no content");
                }
            }
            else {
                Logger.e(this.getClass(), "#generateText - Response state: " + response.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HttpClientFactory.getInstance().returnHttpClient(client);
        }

        return null;
    }
}
