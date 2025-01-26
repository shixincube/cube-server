/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.opinion;

import cube.aigc.Sentiment;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 文章。
 */
public class Article implements JSONable {

    public long id;

    public String title;

    public String content;

    public String author;

    public int year;

    public int month;

    public int date;

    public Sentiment sentiment;

    public Article(String title, String content, String author, int year, int month, int date) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.year = year;
        this.month = month;
        this.date = date;
    }

    public Article(JSONObject json) {
        this.title = json.getString("title");
        this.content = json.getString("content");
        this.author = json.has("author") ? json.getString("author") : "Anonymity";
        this.year = json.getInt("year");
        this.month = json.getInt("month");
        this.date = json.getInt("date");

        if (json.has("id")) {
            this.id = json.getLong("id");
        }
        if (json.has("sentiment")) {
            this.sentiment = Sentiment.parse(json.getString("sentiment"));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("title", this.title);
        json.put("content", this.content);
        json.put("author", this.author);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        if (null != this.sentiment) {
            json.put("sentiment", this.sentiment.code);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("title", this.title);
        json.put("author", this.author);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        return json;
    }
}
