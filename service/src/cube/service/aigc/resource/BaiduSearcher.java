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

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AICapability;
import cube.common.entity.AIGCUnit;
import cube.common.entity.SearchResult;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BaiduSearcher extends ResourceSearcher {

    private final String urlFormat = "https://www.baidu.com/s?wd=%s&oq=%s&f=8&tn=baidu&ie=UTF-8";

    private BaiduSearchResult baiduResult;

    private long startTime;

    public BaiduSearcher(AIGCService service) {
        super(service);
    }

    @Override
    public boolean search(List<String> words) {
        this.startTime = System.currentTimeMillis();

        AIGCUnit unit = this.service.selectUnitBySubtask(AICapability.DataProcessing.ExtractURLContent);
        if (null == unit) {
            Logger.w(this.getClass(), "#search - No unit: " + AICapability.DataProcessing.ExtractURLContent);
            return false;
        }

        String url = this.makeURL(words);
        if (null == url) {
            Logger.w(this.getClass(), "#search - No words");
            return false;
        }

        JSONObject payload = new JSONObject();
        payload.put("url", url);
        payload.put("parser", "baidu");
        Packet request = new Packet(AIGCAction.ExtractURLContent.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(), "#search - Unit is error");
            return false;
        }

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#search - Unit return state: " + Packet.extractCode(response));
            return false;
        }

        JSONArray list = Packet.extractDataPayload(response).getJSONArray("list");
        JSONObject data = list.getJSONObject(0);
        if (!data.has("organic_results")) {
            Logger.w(this.getClass(), "#search - Baidu search result format error: " + url);
            return false;
        }

        this.baiduResult = new BaiduSearchResult(data);
        return true;
    }

    public void fillSearchResult(SearchResult searchResult) {
        searchResult.engine = "baidu";
        searchResult.created = this.startTime;

        if (null != this.baiduResult) {
            for (OrganicResult orgRes : this.baiduResult.organicResults) {
                searchResult.addOrganicResult(orgRes.position,
                        orgRes.title, orgRes.link, orgRes.snippet);
            }
        }
    }

    private String makeURL(List<String> words) {
        if (words.isEmpty()) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        for (String word : words) {
            buf.append(word);
            buf.append(" ");
        }
        buf.deleteCharAt(buf.length() - 1);

        String word = buf.toString();
        String oqWord = words.get(0);
        try {
            word = URLEncoder.encode(word, "UTF-8");
            word = word.replaceAll("\\+", "%20");

            oqWord = URLEncoder.encode(oqWord, "UTF-8");
            oqWord = oqWord.replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return String.format(this.urlFormat, word, oqWord);
    }

    private class BaiduSearchResult {

        public String url;

        public List<OrganicResult> organicResults;

        public BaiduSearchResult(JSONObject json) {
            this.url = json.getString("url");
            this.organicResults = new ArrayList<>();

            if (json.has("organic_results")) {
                JSONArray organicResultArray = json.getJSONArray("organic_results");
                for (int i = 0; i < organicResultArray.length(); ++i) {
                    OrganicResult or = new OrganicResult(organicResultArray.getJSONObject(i));
                    this.organicResults.add(or);
                }
            }
        }
    }

    private class OrganicResult {

        public int position;

        public String title;

        public String link;

        public String snippet;

        public OrganicResult(JSONObject json) {
            this.position = json.getInt("position");
            this.title = json.getString("title");
            this.link = json.getString("link");
            this.snippet = json.getString("snippet");
        }
    }
}
