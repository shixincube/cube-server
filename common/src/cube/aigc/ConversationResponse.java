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
import cube.common.entity.ComplexContext;
import cube.common.entity.FileLabel;
import cube.common.entity.KnowledgeQAResult;
import cube.common.entity.KnowledgeSource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConversationResponse implements JSONable {

    private long sn;

    private String id = "";

    private String conversationId = "";

    private String parentMessageId = "";

    private String role = "";

    private String text = "";

    private List<FileLabel> fileLabels;

    private boolean end = true;

    // 无用的属性，仅用于兼容旧版本
    private Detail detail = new Detail();

    public ComplexContext context;

    public List<KnowledgeSource> knowledgeSources;

    public ConversationResponse(long sn, String id, String conversationId, String text) {
        this.sn = sn;
        this.id = id;
        this.conversationId = conversationId;
        this.text = text;
    }

    public ConversationResponse(long sn, String id, String conversationId, String text, String parentMessageId) {
        this.sn = sn;
        this.id = id;
        this.conversationId = conversationId;
        this.text = text;
        this.parentMessageId = parentMessageId;
    }

    public ConversationResponse(long sn, String id, String conversationId, String text,
                                String parentMessageId, boolean end) {
        this(sn, id, conversationId, text, parentMessageId);
        this.end = end;
    }

    public ConversationResponse(long sn, String id, String conversationId, String parentMessageId,
                                List<FileLabel> fileLabels) {
        this.sn = sn;
        this.id = id;
        this.conversationId = conversationId;
        this.parentMessageId = parentMessageId;
        this.fileLabels = fileLabels;
    }

    public ConversationResponse(JSONObject json) {
        this.sn = json.getLong("sn");
        this.id = json.getString("id");
        this.conversationId = json.getString("conversationId");
        this.parentMessageId = json.getString("parentMessageId");
        this.end = json.getBoolean("end");
        this.role = json.getString("role");
        this.text = json.getString("text");
        this.detail = new Detail(json.getJSONObject("detail"));
        if (json.has("context")) {
            this.context = new ComplexContext(json.getJSONObject("context"));
        }
        if (json.has("fileLabels")) {
            this.fileLabels = new ArrayList<>();
            JSONArray array = json.getJSONArray("fileLabels");
            for (int i = 0; i < array.length(); ++i) {
                FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                this.fileLabels.add(fileLabel);
            }
        }
        if (json.has("knowledgeSources")) {
            this.knowledgeSources = new ArrayList<>();
            JSONArray array = json.getJSONArray("knowledgeSources");
            for (int i = 0; i < array.length(); ++i) {
                KnowledgeSource source = new KnowledgeSource(array.getJSONObject(i));
                this.knowledgeSources.add(source);
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("id", this.id);
        json.put("conversationId", this.conversationId);
        json.put("parentMessageId", this.parentMessageId);
        json.put("end", this.end);
        json.put("role", this.role);
        json.put("text", this.text);
        json.put("detail", this.detail.toJSON());
        if (null != this.context) {
            json.put("context", this.context.toJSON());
        }
        if (null != this.fileLabels) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.fileLabels) {
                array.put(fileLabel.toCompactJSON());
            }
            json.put("fileLabels", array);
        }
        if (null != this.knowledgeSources && !this.knowledgeSources.isEmpty()) {
            JSONArray array = new JSONArray();
            for (KnowledgeSource source : this.knowledgeSources) {
                array.put(source.toJSON());
            }
            json.put("knowledgeSources", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public class Detail implements JSONable {

        public String id;

        public long created;

        public String model;

        public String object;

        public Usage usage;

        public List<Choice> choices;

        public Detail() {
            this.id = "";
            this.created = 0;
            this.model = "";
            this.object = "";
            this.usage = new Usage("Baize", 0, 0, 0);
            this.choices = new ArrayList<>();
            this.choices.add(new Choice());
        }

        public Detail(String id, long created, String model, String object, Usage usage) {
            this.id = id;
            this.created = created;
            this.model = model;
            this.object = object;
            this.usage = usage;
            this.choices = new ArrayList<>();
            this.choices.add(new Choice());
        }

        public Detail(JSONObject json) {
            this.id = json.getString("id");
            this.created = json.getLong("created");
            this.model = json.getString("model");
            this.object = json.getString("object");
            this.usage = new Usage(json.getJSONObject("usage"));
            this.choices = new ArrayList<>();
            JSONArray array = json.getJSONArray("choices");
            for (int i = 0; i < array.length(); ++i) {
                Choice choice = new Choice(array.getJSONObject(i));
                this.choices.add(choice);
            }
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("id", this.id);
            json.put("created", this.created);
            json.put("model", this.model);
            json.put("object", this.object);
            json.put("usage", this.usage.toJSON());
            JSONArray array = new JSONArray();
            for (Choice choice : this.choices) {
                array.put(choice.toJSON());
            }
            json.put("choices", array);

            json.put("status", "Success");
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }


    public class Choice implements JSONable {

        public final int index;

        public final String finishReason;

        public final String text;

        public Choice() {
            this.index = 0;
            this.text = "";
            this.finishReason = "end";
        }

        public Choice(int index, String text, String finishReason) {
            this.index = index;
            this.text = text;
            this.finishReason = finishReason;
        }

        public Choice(JSONObject json) {
            this.index = json.getInt("index");
            this.text = json.getString("text");
            this.finishReason = json.getString("finish_reason");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("index", this.index);
            json.put("text", this.text);
            json.put("finish_reason", this.finishReason);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
