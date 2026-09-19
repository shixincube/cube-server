/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 模型的应用配置。
 */
public class ModelConfig implements JSONable {

    public static int EXTRA_LONG_CONTEXT_LIMIT = 10 * 1024 * 1024;

    public static int BAIZE_CONTEXT_LIMIT = 1024 * 1024;

    public static int BAIZE_X_CONTEXT_LIMIT = 1024 * 1024;

    public static int BAIZE_NEXT_CONTEXT_LIMIT = 1024 * 1024;

    public final static String[] TEXT_TO_IMAGE_UNIT = new String[] { "DallE" };

    public final static String BAIZE_UNIT = "Baize2";

    public final static String BAIZE_X_UNIT = "Baize2";

    public final static String BAIZE_NEXT_UNIT = "Baize2";

    public final static String PSYCHOLOGY_UNIT = "Psychology";

    public final static String FACIAL_EXPRESSION_UNIT = "FacialExpression";

    private final String model;

    private final String name;

    private final String desc;

    private final String apiURL;

    private final JSONObject parameter;

    public ModelConfig(String model, String name, String desc, String apiURL, JSONObject parameter) {
        this.model = model;
        this.name = name;
        this.desc = desc;
        this.apiURL = apiURL + (apiURL.endsWith("/") ? "" : "/");
        this.parameter = parameter;
    }

    public ModelConfig(String model, JSONObject json) {
        this.model = model;
        this.name = json.getString("name");
        this.desc = json.getString("desc");
        this.apiURL = json.getString("apiURL");
        this.parameter = json.getJSONObject("parameter");
    }

    public ModelConfig(JSONObject json) {
        this.model = json.has("model") ? json.getString("model") :
                json.getJSONObject("parameter").getString("unit");
        this.name = json.getString("name");
        this.desc = json.getString("desc");
        this.apiURL = json.getString("apiURL");
        this.parameter = json.getJSONObject("parameter");
    }

    public String getModel() {
        return this.model;
    }

    public String getName() {
        return this.name;
    }

    public String getDesc() {
        return this.desc;
    }

    public String getApiURL() {
        return this.apiURL;
    }

    public String getUnitName() {
        return this.parameter.getString("unit");
    }

    public String getChannelURL() {
        int index = this.apiURL.indexOf("/aigc/");
        return this.apiURL.substring(0, index) + "/aigc/channel/";
    }

    public JSONObject getParameter() {
        return this.parameter;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("model", this.model);
        json.put("name", this.name);
        json.put("desc", this.desc);
        json.put("apiURL", this.apiURL);
        json.put("parameter", this.parameter);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 判断指定的单元是否是 TextToImage 单元。
     *
     * @param unitName
     * @return
     */
    public static boolean isTextToImageUnit(String unitName) {
        for (String name : TEXT_TO_IMAGE_UNIT) {
            if (name.equalsIgnoreCase(unitName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取提示词长度限制。
     *
     * @param unitName
     * @return
     */
    public static int getPromptLengthLimit(String unitName) {
        if (unitName.equalsIgnoreCase(BAIZE_UNIT)) {
            return BAIZE_CONTEXT_LIMIT;
        }
        else if (unitName.equalsIgnoreCase(BAIZE_NEXT_UNIT)) {
            return BAIZE_NEXT_CONTEXT_LIMIT;
        }
        else if (unitName.equalsIgnoreCase(BAIZE_X_UNIT)) {
            return BAIZE_X_CONTEXT_LIMIT;
        }

        return EXTRA_LONG_CONTEXT_LIMIT;
    }
}
