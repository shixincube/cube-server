/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
 * 百度搜索器。
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

        JSONObject data = null;
        try {
            JSONArray list = Packet.extractDataPayload(response).getJSONArray("list");
            data = list.getJSONObject(0);
            if (!data.has("organicResults")) {
                Logger.w(this.getClass(), "#search - Baidu search result format error: " + url);
                return false;
            }

            this.baiduResult = new BaiduSearchResult(data);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#search - Baidu search result exception", e);
            return false;
        }

        return true;
    }

    @Override
    public void fillSearchResult(SearchResult searchResult) {
        searchResult.engine = "baidu";
        searchResult.created = this.startTime;

        if (null != this.baiduResult) {
            for (OrganicResult orgRes : this.baiduResult.organicResults) {
                searchResult.addOrganicResult(orgRes.position,
                        orgRes.title, orgRes.link, orgRes.snippet);
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#search - result num: " + searchResult.organicResults.size());
        }
    }

    @Override
    protected String makeURL(List<String> words) {
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
        String oqWord = words.get(words.size() - 1);
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

            if (json.has("organicResults")) {
                JSONArray organicResultArray = json.getJSONArray("organicResults");
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
