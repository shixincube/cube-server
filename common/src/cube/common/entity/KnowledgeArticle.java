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

package cube.common.entity;

import org.json.JSONObject;

/**
 * 知识库文章。
 */
public class KnowledgeArticle extends Entity {

    public String category;

    public String title;

    public String content;

    public String author;

    public int year;

    public int month;

    public int date;

    public KnowledgeArticle(String category, String title, String content, String author,
                            int year, int month, int date, long timestamp) {
        super();
        this.category = category;
        this.title = title;
        this.content = content;
        this.author = author;
        this.year = year;
        this.month = month;
        this.date = date;
        this.timestamp = timestamp;
    }

    public KnowledgeArticle(long id, String category, String title, String content, String author,
                            int year, int month, int date, long timestamp) {
        super(id, "", timestamp);
        this.category = category;
        this.title = title;
        this.content = content;
        this.author = author;
        this.year = year;
        this.month = month;
        this.date = date;
    }

    public KnowledgeArticle(JSONObject json) {
        super(json);
        this.category = json.getString("category");
        this.title = json.getString("title");
        if (json.has("content")) {
            this.content = json.getString("content");
        }
        this.author = json.has("author") ? json.getString("author") : "Anonymity";
        this.year = json.getInt("year");
        this.month = json.getInt("month");
        this.date = json.getInt("date");
    }

    @Override
    public boolean equals(Object obj) {
        if (null != obj && obj instanceof KnowledgeArticle) {
            KnowledgeArticle other = (KnowledgeArticle) obj;
            return other.id.longValue() == this.id.longValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("category", this.category);
        json.put("title", this.title);
        if (null != this.content) {
            json.put("content", this.content);
        }
        json.put("author", this.author);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("category", this.category);
        json.put("title", this.title);
        json.put("author", this.author);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        return json;
    }
}