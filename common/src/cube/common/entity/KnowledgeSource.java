/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import cube.util.FileUtils;
import org.json.JSONObject;

/**
 * 知识源。
 */
public class KnowledgeSource implements JSONable {

    private KnowledgeDoc document;

    private KnowledgeArticle article;

    private String text;

    public KnowledgeSource(KnowledgeDoc document) {
        this.document = document;
    }

    public KnowledgeSource(KnowledgeArticle article) {
        this.article = article;
    }

    public KnowledgeSource(String text) {
        this.text = text;
    }

    public KnowledgeSource(JSONObject json) {
        if (json.has("document")) {
            this.document = new KnowledgeDoc(json.getJSONObject("document"));
        }
        if (json.has("article")) {
            this.article = new KnowledgeArticle(json.getJSONObject("article"));
        }
        if (json.has("text")) {
            this.text = json.getString("text");
        }
    }

    public KnowledgeDoc getDocument() {
        return this.document;
    }

    public KnowledgeArticle getArticle() {
        return this.article;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        if (null != this.document) {
            return "文件《" + FileUtils.extractFileName(this.document.getFileLabel().getFileName()) + "》";
        }
        else if (null != this.article) {
            return "文章《" + this.article.title + "》";
        }
        else if (null != this.text) {
            return "文本《" + this.text.substring(0, Math.min(20, this.text.length())).trim().replaceAll("\n", "") + "...》";
        }
        else {
            return "";
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.document) {
            json.put("document", this.document.toCompactJSON());
        }
        if (null != this.article) {
            json.put("article", this.article.toCompactJSON());
        }
        if (null != this.text) {
            json.put("text", this.text);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
