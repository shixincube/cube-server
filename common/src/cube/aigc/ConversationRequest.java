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

import com.sun.istack.internal.Nullable;
import cube.common.JSONable;
import org.json.JSONObject;

public class ConversationRequest implements JSONable {

    public final String prompt;

    public final Options options;

    public final float temperature;

    public final float topP;

    public ConversationRequest(JSONObject json) {
        this.prompt = json.getString("prompt");
        this.options = new Options(json.getJSONObject("options"));
        this.temperature = json.has("temperature") ? json.getFloat("temperature") : 0.7f;
        this.topP = json.has("top_p") ? json.getFloat("top_p") : 0.8f;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("prompt", this.prompt);
        json.put("options", this.options.toJSON());
        json.put("temperature", this.temperature);
        json.put("top_p", this.topP);
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

        @Nullable
        public String conversationId;

        @Nullable
        public String parentMessageId;

        public Options(JSONObject json) {
            if (json.has("conversationId")) {
                this.conversationId = json.getString("conversationId");
            }
            if (json.has("parentMessageId")) {
                this.parentMessageId = json.getString("parentMessageId");
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
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
