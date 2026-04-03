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

    private List<Paragraph> paragraphs;

    public Article(String title) {
        this.title = title;
        this.paragraphs = new ArrayList<>();
    }

    public void addParagraph(String title, String keyword, String content) {
        this.paragraphs.add(new Paragraph(title, keyword, content));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        JSONArray array = new JSONArray();
        for (Paragraph paragraph : this.paragraphs) {
            array.put(paragraph.toJSON());
        }
        json.put("paragraphs", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public class Paragraph implements JSONable {

        public String title;

        public String keyword;

        public String content;

        public Paragraph(String title, String keyword, String content) {
            this.title = title;
            this.keyword = keyword;
            this.content = content;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("title", this.title);
            json.put("keyword", this.keyword);
            json.put("content", this.content);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
