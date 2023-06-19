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

package cube.service.aigc.resource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果。
 */
public class SearchResult {

    public String engine;

    public long created;

    public List<String> keywords;

    public List<OrganicResult> organicResults;

    public SearchResult() {
        this.engine = "unknown";
        this.created = System.currentTimeMillis();
    }

    public void addOrganicResult(int position, String title, String link, String snippet) {
        if (null == this.organicResults) {
            this.organicResults = new ArrayList<>();
        }

        OrganicResult result = new OrganicResult(position, title, link, snippet);
        this.organicResults.add(result);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
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
