/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Router {

    public final static String RULE_TRUE_FALSE = "true-or-false";

    private String rule;

    private Route route;

    public Router(JSONObject json) {
        this.rule = json.getString("rule");

        JSONObject map = json.getJSONObject("route");
        Map<String, RouteExport> exportMap = new HashMap<>();
        for (String key : map.keySet()) {
            JSONObject data = map.getJSONObject(key);

            Map<String, String> answerMap = new HashMap<>();
            if (data.has("answer")) {
                JSONObject answerJson = data.getJSONObject("answer");
                Iterator<String> keys = answerJson.keys();
                while (keys.hasNext()) {
                    String answerKey = keys.next();
                    answerMap.put(answerKey, answerJson.getString(answerKey));
                }
            }

            RouteExport routeExport = new RouteExport(data.getString("jump"), answerMap);
            exportMap.put(key, routeExport);
        }
        this.route = new Route(exportMap);
    }

    public String getRule() {
        return this.rule;
    }

    public Route getRoute() {
        return this.route;
    }

    public RouteExport getRouteExport(String code) {
        return this.route.exportMap.get(code);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("rule", this.rule);

        JSONObject routeJson = new JSONObject();
        Iterator<Map.Entry<String, RouteExport>> iter = this.route.exportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, RouteExport> e = iter.next();
            routeJson.put(e.getKey(), e.getValue().toJSON());
        }
        json.put("route", routeJson);
        return json;
    }

    public class Route {

        public final Map<String, RouteExport> exportMap;

        public Route(Map<String, RouteExport> exportMap) {
            this.exportMap = exportMap;
        }
    }

    public class RouteExport {

        public String jump;

        public Map<String, String> answerMap;

        public RouteExport(String jump, Map<String, String> answerMap) {
            this.jump = jump;
            this.answerMap = answerMap;
        }

        public boolean hasAnswer() {
            return !this.answerMap.isEmpty();
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("jump", this.jump);

            JSONObject answerMapData = new JSONObject();
            for (Map.Entry<String, String> data : this.answerMap.entrySet()) {
                answerMapData.put(data.getKey(), data.getValue());
            }
            json.put("answer", answerMapData);

            return json;
        }
    }
}
