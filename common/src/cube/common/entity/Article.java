/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Article implements JSONable {

    private String title;

    private String content;

    private List<Paragraph> paragraphs;

    public Article(String title) {
        this.title = title;
        this.paragraphs = new ArrayList<>();
    }

    public String getTitle() {
        return this.title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    public void addParagraph(JSONObject json) {
        Paragraph paragraph = new Paragraph(json);
        this.paragraphs.add(paragraph);
    }

    public void addParagraph(String title, String content) {
        this.paragraphs.add(new Paragraph(title, content));
    }

    public List<Paragraph> getParagraphs() {
        return this.paragraphs;
    }

    public Paragraph getParagraph(int index) {
        if (this.paragraphs.size() <= index) {
            return null;
        }
        return this.paragraphs.get(index);
    }

    public String spliceParagraphContent() {
        StringBuilder buf = new StringBuilder();
        for (Paragraph paragraph : this.paragraphs) {
            buf.append(paragraph.content);
            buf.append("\n");
        }
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        if (null != this.content && this.content.length() > 0) {
            json.put("content", this.content);
        }
        if (!this.paragraphs.isEmpty()) {
            JSONArray array = new JSONArray();
            for (Paragraph paragraph : this.paragraphs) {
                array.put(paragraph.toJSON());
            }
            json.put("paragraphs", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public class Paragraph implements JSONable {

        public final String title;

        public final String content;

        public Paragraph(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public Paragraph(JSONObject json) {
            this.title = json.getString("title");
            this.content = json.getString("content");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("title", this.title);
            json.put("content", this.content);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
