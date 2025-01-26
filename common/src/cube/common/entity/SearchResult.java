/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.auth.AuthToken;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果。
 */
public class SearchResult implements JSONable {

    public long sn;

    public String engine;

    public long created;

    public String originSentence;

    public List<String> keywords;

    public List<OrganicResult> organicResults;

    public AuthToken authToken;

    /**
     * 是否已被客户端拉取数据。
     */
    public boolean popup = false;

    public SearchResult(String originSentence) {
        this.sn = Utils.generateSerialNumber();
        this.engine = "unknown";
        this.created = System.currentTimeMillis();
        this.originSentence = originSentence;
    }

    public boolean hasResult() {
        return (null != this.organicResults && !this.organicResults.isEmpty());
    }

    public void addOrganicResult(int position, String title, String link, String snippet) {
        if (null == this.organicResults) {
            this.organicResults = new ArrayList<>();
        }

        OrganicResult result = new OrganicResult(position, title, link, snippet);
        this.organicResults.add(result);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("sn", this.sn);
        json.put("engine", this.engine);
        json.put("created", this.created);

        if (null != this.keywords) {
            JSONArray array = new JSONArray();
            for (String word : this.keywords) {
                array.put(word);
            }
            json.put("keywords", array);
        }

        if (null != this.organicResults) {
            JSONArray array = new JSONArray();
            for (OrganicResult orgRes : this.organicResults) {
                array.put(orgRes.toJSON());
            }
            json.put("organicResults", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public class OrganicResult {

        public int position;

        public String title;

        public String link;

        public String snippet;

        public OrganicResult(int position, String title, String link, String snippet) {
            this.position = position;
            this.title = title;
            this.link = link;
            this.snippet = snippet;
        }

        public OrganicResult(JSONObject json) {
            this.position = json.getInt("position");
            this.title = json.getString("title");
            this.link = json.getString("link");
            this.snippet = json.getString("snippet");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("position", this.position);
            json.put("title", this.title);
            json.put("link", this.link);
            json.put("snippet", this.snippet);
            return json;
        }
    }
}
