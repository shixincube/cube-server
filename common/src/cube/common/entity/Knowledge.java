/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.Consts;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Knowledge implements JSONable {

    public String query;

    public List<Metadata> metadataList;

    public Knowledge(String query) {
        this.query = query;
        this.metadataList = new ArrayList<>();
    }

    public Knowledge(JSONObject json) {
        this.query = json.getString("query");
        this.metadataList = new ArrayList<>();
        if (json.has("metadata")) {
            JSONArray array = json.getJSONArray("metadata");
            for (int i = 0; i < array.length(); ++i) {
                Metadata metadata = new Metadata(array.getJSONObject(i));
                this.metadataList.add(metadata);
            }
        }
    }

    public boolean isEmpty() {
        return this.metadataList.isEmpty();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("query", this.query);

        JSONArray array = new JSONArray();
        for (Metadata metadata : this.metadataList) {
            array.put(metadata.toJSON());
        }
        json.put("metadata", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public String generatePrompt() {
        StringBuilder buf = new StringBuilder();
        for (Metadata metadata : this.metadataList) {
            buf.append(metadata.content);
            buf.append("\n\n");
        }
        String prompt = Consts.formatQuestion(buf.toString(), this.query);
        return prompt;
    }

    /*public void mergeAnswer(PromptMetadata other) {
        if (null == other.answer) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        String[] lines = this.prompt.split("\n");
        // 首行
        buf.append(lines[0]).append("\n");
        // 合并答案
        buf.append(other.answer);
        // 逐条恢复
        for (int i = 1; i < lines.length; ++i) {
            buf.append(lines[i]).append("\n");
        }
        if (buf.length() > 2) {
            buf.delete(buf.length() - 1, buf.length());
        }

        // 新提示词
        this.prompt = buf.toString();

        // 添加源
        this.metadataList.addAll(other.metadataList);
    }*/

    /**
     * 将相同的源进行合并。
     *
     * @return
     */
    public List<KnowledgeSource> mergeSources() {
        List<KnowledgeSource> list = new ArrayList<>();

        List<Metadata> mdList = new ArrayList<>();
        for (Metadata metadata : this.metadataList) {
            if (mdList.contains(metadata)) {
                continue;
            }
            mdList.add(metadata);
        }

        for (Metadata metadata : mdList) {
            KnowledgeSource source = metadata.getKnowledgeSource();
            if (null != source) {
                list.add(source);
            }
        }

        return list;
    }

    public class Metadata {

        public final static String DOCUMENT_PREFIX = "document:";

        public final static String ARTICLE_PREFIX = "article:";

        public final static String SEGMENT_PREFIX = "segment:";

        private String content;

        private String source;

        private KnowledgeSource knowledgeSource;

        private double score;

        public Metadata(JSONObject json) {
            this.content = json.getString("content");
            this.source = json.getString("source");
            String s = json.getString("score");
            this.score = Double.parseDouble(s);
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("content", this.content);
            json.put("source", this.source);
            json.put("score", Double.toString(this.score));
            return json;
        }

        public String getContent() {
            return this.content;
        }

        public String getSourceKey() {
            if (this.source.startsWith(DOCUMENT_PREFIX)) {
                return this.source.substring(DOCUMENT_PREFIX.length());
            }
            else if (this.source.startsWith(ARTICLE_PREFIX)) {
                return this.source.substring(ARTICLE_PREFIX.length());
            }
            else {
                return this.source.substring(SEGMENT_PREFIX.length());
            }
        }

        public boolean isDocument() {
            return this.source.startsWith(DOCUMENT_PREFIX);
        }

        public boolean isArticle() {
            return this.source.startsWith(ARTICLE_PREFIX);
        }

        public void setKnowledgeSource(KnowledgeSource knowledgeSource) {
            this.knowledgeSource = knowledgeSource;
        }

        public KnowledgeSource getKnowledgeSource() {
            return this.knowledgeSource;
        }

        /*public KnowledgeSource matchSource() {
            // 判断是文档还是文章
            if (this.source.startsWith(DOCUMENT_PREFIX)) {
                String fileCode = this.source.substring(DOCUMENT_PREFIX.length());
                KnowledgeDocument doc = getKnowledgeDocByFileCode(fileCode);
                if (null == doc) {
                    return null;
                }

                return new KnowledgeSource(doc);
            }
            else if (this.source.startsWith(ARTICLE_PREFIX)) {
                String id = this.source.substring(ARTICLE_PREFIX.length());
                try {
                    long articleId = Long.parseLong(id);
                    KnowledgeArticle article = storage.readKnowledgeArticle(articleId);
                    if (null == article) {
                        return null;
                    }

                    return new KnowledgeSource(article);
                } catch (Exception e) {
                    // Nothing
                }
            }
            else if (this.source.startsWith(SEGMENT_PREFIX)) {
                String segment = this.source.substring(SEGMENT_PREFIX.length());
                return new KnowledgeSource(segment);
            }

            return null;
        }*/

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Metadata) {
                if (this.source.equals(((Metadata) obj).source)) {
                    return true;
                }

                // File Code 或 ID 重复
                String key = this.getSourceKey();
                String otherKey = ((Metadata) obj).getSourceKey();
                if (null != key && null != otherKey) {
                    return key.equals(otherKey);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.source.hashCode();
        }
    }
}
