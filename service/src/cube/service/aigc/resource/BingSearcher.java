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
 * 必应搜索器。
 */
public class BingSearcher extends ResourceSearcher {

    private final String urlFormat = "https://www.bing.com/search?q=%s";

    private BingSearchResult bingResult;

    private long startTime;

    public BingSearcher(AIGCService service) {
        super(service);
    }

    @Override
    public boolean search(List<String> words) {
        Logger.d(this.getClass(), "#search - Words: " + words.toArray(new String[0]).toString());

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
        payload.put("parser", "bing");
        Packet request = new Packet(AIGCAction.ExtractURLContent.name, payload);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 90 * 1000);
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
                Logger.w(this.getClass(), "#search - Bing search result format error: " + url);
                return false;
            }

            this.bingResult = new BingSearchResult(data);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#search - Bing search result exception", e);
            return false;
        }

        return true;
    }

    @Override
    public void fillSearchResult(SearchResult searchResult) {
        searchResult.engine = "bing";
        searchResult.created = this.startTime;

        if (null != this.bingResult) {
            for (OrganicResult orgRes : this.bingResult.organicResults) {
                searchResult.addOrganicResult(orgRes.position,
                        orgRes.title, orgRes.link, orgRes.snippet);
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#search - result num: " + (searchResult.hasResult() ?
                    searchResult.organicResults.size() : 0));
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
        try {
            word = URLEncoder.encode(word, "UTF-8");
            word = word.replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return String.format(this.urlFormat, word);
    }

    private class BingSearchResult {

        public String url;

        public String recommend;

        public List<OrganicResult> organicResults;

        public BingSearchResult(JSONObject json) {
            this.url = json.getString("url");
            this.recommend = json.getString("recommend");
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
