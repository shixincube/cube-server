/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import org.json.JSONArray;
import org.json.JSONObject;

public class ConversationRequest implements JSONable {

    public final String prompt;

    public final Options options;

    public final boolean usingContext;

    public final boolean usingNetwork;

    public final float temperature;

    public final float topP;

    public final int searchTopK;

    public final int searchFetchK;

    public ConversationRequest(JSONObject json) {
        this.prompt = json.getString("prompt");
        this.options = new Options(json.getJSONObject("options"));
        this.usingContext = !json.has("usingContext") || json.getBoolean("usingContext");
        this.usingNetwork = json.has("usingNetwork") && json.getBoolean("usingNetwork");
        this.temperature = json.has("temperature") ? json.getFloat("temperature") : 0.7f;
        this.topP = json.has("top_p") ? json.getFloat("top_p") : 0.8f;
        this.searchTopK = json.has("searchTopK") ? json.getInt("searchTopK") : 10;
        this.searchFetchK = json.has("searchFetchK") ? json.getInt("searchFetchK") : 50;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("prompt", this.prompt);
        json.put("options", this.options.toJSON());
        json.put("usingContext", this.usingContext);
        json.put("usingNetwork", this.usingNetwork);
        json.put("temperature", this.temperature);
        json.put("top_p", this.topP);
        json.put("searchTopK", this.searchTopK);
        json.put("searchFetchK", this.searchFetchK);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    /**
     * 选项。
     */
    public class Options implements JSONable {

        public String conversationId;

        public String parentMessageId;

        public String workPattern;

        public JSONArray attachments;

        public JSONArray categories;

        public Options(JSONObject json) {
            if (json.has("conversationId")) {
                this.conversationId = json.getString("conversationId");
            }
            if (json.has("parentMessageId")) {
                this.parentMessageId = json.getString("parentMessageId");
            }
            if (json.has("workPattern")) {
                this.workPattern = json.getString("workPattern");
            }
            if (json.has("attachments")) {
                this.attachments = json.getJSONArray("attachments");
            }
            if (json.has("categories")) {
                this.categories = json.getJSONArray("categories");
            }
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            if (null != this.conversationId) {
                json.put("conversationId", this.conversationId);
            }
            if (null != this.parentMessageId) {
                json.put("parentMessageId", this.parentMessageId);
            }
            if (null != this.workPattern) {
                json.put("workPattern", this.workPattern);
            }
            if (null != this.attachments) {
                json.put("attachments", this.attachments);
            }
            if (null != this.categories) {
                json.put("categories", this.categories);
            }
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
