/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 模型的应用配置。
 */
public class ModelConfig implements JSONable {

    public static int EXTRA_LONG_CONTEXT_LIMIT = 5000;

    public static int BAIZE_CONTEXT_LIMIT = 1000;

    public static int BAIZE_NEXT_CONTEXT_LIMIT = 3000;


    public final static String[] TEXT_TO_IMAGE_UNIT = new String[] { "DallE" };

    public final static String[] EXTRA_LONG_PROMPT_UNIT = new String[]{ "GPT", "Gemini", "Wenxin" };

    public final static String BAIZE_UNIT = "Chat";

    public final static String BAIZE_NEXT_UNIT = "BaizeNext";

    public final static String PSYCHOLOGY_UNIT = "Psychology";

    private final static String[][] UNIT_MAP_MODEL = new String[][] {
            new String[] { "Chat", "Baize" },
            new String[] { "GPT", "GPT" },
            new String[] { "DallE", "DallE" },
            new String[] { "Gemini", "Gemini" },
            new String[] { "Wenxin", "Wenxin" },
            new String[] { "BaizeNext", "BaizeNext" }
    };

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
     * 获取单元名对应的模型。
     *
     * @param unit
     * @return
     */
    public static String getModelByUnit(String unit) {
        for (String[] map : UNIT_MAP_MODEL) {
            if (map[0].equalsIgnoreCase(unit)) {
                return map[1];
            }
        }
        return unit;
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
     * 判断指定的单元是否是支持超长提示词的单元。
     *
     * @param unitName
     * @return
     */
    public static boolean isExtraLongPromptUnit(String unitName) {
        for (String name : EXTRA_LONG_PROMPT_UNIT) {
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
        if (isExtraLongPromptUnit(unitName)) {
            return EXTRA_LONG_CONTEXT_LIMIT;
        }
        else {
            if (unitName.equalsIgnoreCase("BaizeNext")) {
                return BAIZE_NEXT_CONTEXT_LIMIT;
            }
            else {
                return BAIZE_CONTEXT_LIMIT;
            }
        }
    }
}
