/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 知识库文章。
 */
public class KnowledgeArticle extends Entity {

    public long contactId;

    public String category;

    public String title;

    public String content;

    /**
     * 字数。
     */
    public int numWords;

    public String summarization;

    public String author;

    public int year;

    public int month;

    public int date;

    public String baseName;

    public KnowledgeScope scope = KnowledgeScope.Private;

    public int numKeywords;

    public boolean activated;

    public int numSegments;

    public KnowledgeArticle(String domain, long contactId, String baseName,
                            String category, String title, String content, String author,
                            int year, int month, int date, long timestamp, KnowledgeScope scope) {
        super(0L, domain, timestamp);
        this.contactId = contactId;
        this.baseName = baseName;
        this.category = category;
        this.title = title;
        this.content = content;
        this.author = author;
        this.year = year;
        this.month = month;
        this.date = date;
        this.scope = scope;
        this.numWords = content.length();
    }

    public KnowledgeArticle(long id, String domain, long contactId, String baseName,
                            String category, String title, String content,
                            String summarization, String author, int year, int month, int date, long timestamp,
                            KnowledgeScope scope, boolean activated, int numSegments) {
        super(id, domain, timestamp);
        this.contactId = contactId;
        this.baseName = baseName;
        this.category = category;
        this.title = title;
        this.content = content;
        this.summarization = summarization;
        this.author = author;
        this.year = year;
        this.month = month;
        this.date = date;
        this.scope = scope;
        this.activated = activated;
        this.numSegments = numSegments;
        this.numWords = content.length();
    }

    public KnowledgeArticle(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.baseName = json.getString("base");
        this.category = json.getString("category");
        this.title = json.getString("title");
        if (json.has("content")) {
            this.content = json.getString("content");
        }
        if (json.has("summarization")) {
            this.summarization = json.getString("summarization");
        }
        this.author = json.has("author") ? json.getString("author") : "Anonymity";
        this.year = json.getInt("year");
        this.month = json.getInt("month");
        this.date = json.getInt("date");

        this.scope = json.has("scope") ? KnowledgeScope.parse(json.getString("scope")) :
                KnowledgeScope.Private;
        this.activated = json.has("activated") && json.getBoolean("activated");
        this.numSegments = json.has("numSegments") ? json.getInt("numSegments") : 0;
        if (json.has("numWords")) {
            this.numWords = json.getInt("numWords");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KnowledgeArticle) {
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
        json.put("contactId", this.contactId);
        json.put("base", this.baseName);
        json.put("category", this.category);
        json.put("title", this.title);
        if (null != this.content) {
            json.put("content", this.content);
        }
        else {
            json.put("content", "");
        }
        if (null != this.summarization) {
            json.put("summarization", this.summarization);
        }
        else {
            json.put("summarization", "");
        }
        json.put("author", this.author);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        json.put("scope", this.scope.name);
        json.put("activated", this.activated);
        json.put("numSegments", this.numSegments);
        json.put("numWords", this.numWords);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("contactId", this.contactId);
        json.put("base", this.baseName);
        json.put("category", this.category);
        json.put("title", this.title);
        json.put("author", this.author);
        json.put("year", this.year);
        json.put("month", this.month);
        json.put("date", this.date);
        json.put("scope", this.scope.name);
        json.put("activated", this.activated);
        json.put("numSegments", this.numSegments);
        json.put("numWords", this.numWords);
        return json;
    }
}
