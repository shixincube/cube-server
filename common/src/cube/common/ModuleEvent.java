/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 模块事件描述。
 */
public class ModuleEvent implements JSONable {

    /**
     * 模块名。
     */
    private String moduleName;

    /**
     * 事件名。
     */
    private String eventName;

    /**
     * 事件的数据负载。
     */
    private JSONObject data;

    /**
     * 事件上下文。
     */
    private JSONObject context;

    /**
     * 构造函数。
     *
     * @param moduleName 模块名。
     * @param eventName 事件名。
     * @param data 对应的数据。
     */
    public ModuleEvent(String moduleName, String eventName, JSONObject data) {
        this.moduleName = moduleName;
        this.eventName = eventName;
        this.data = data;
    }

    /**
     * 构造函数。
     *
     * @param json 模块事件的 JSON 格式。
     */
    public ModuleEvent(JSONObject json) {
        try {
            this.moduleName = json.getString("module");
            this.eventName = json.getString("event");
            this.data = json.getJSONObject("data");
            if (json.has("context")) {
                this.context = json.getJSONObject("context");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取模块名称。
     *
     * @return 返回模块名称。
     */
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * 获取事件名称。
     *
     * @return 返回事件名称。
     */
    public String getEventName() {
        return this.eventName;
    }

    /**
     * 获取事件的数据。
     *
     * @return 返回事件的数据。
     */
    public JSONObject getData() {
        return this.data;
    }

    /**
     * 设置上下文。
     *
     * @param context
     */
    public void setContext(JSONObject context) {
        this.context = context;
    }

    /**
     * 获取上下文。
     *
     * @return 返回上下文。
     */
    public JSONObject getContext() {
        return this.context;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("module", this.moduleName);
            json.put("event", this.eventName);
            json.put("data", this.data);
            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 提取 JSON 数据里的模块名称。
     *
     * @param json 指定 JSON 数据。
     * @return
     */
    public static String extractModuleName(JSONObject json) {
        String mod = null;
        try {
            mod = json.getString("module");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mod;
    }

    /**
     * 提取 JSON 数据里的事件名。
     *
     * @param json 指定 JSON 数据。
     * @return
     */
    public static String extractEventName(JSONObject json) {
        String event = null;
        try {
            event = json.getString("event");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return event;
    }
}
