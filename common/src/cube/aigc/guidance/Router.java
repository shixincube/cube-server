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
            RouteExport routeExport = new RouteExport(data.getString("answer"), data.getString("jump"));
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

        Iterator<Map.Entry<String, RouteExport>> iter = this.route.exportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, RouteExport> e = iter.next();
            json.put(e.getKey(), e.getValue().toJSON());
        }
        return json;
    }

    public class Route {

        public final Map<String, RouteExport> exportMap;

        public Route(Map<String, RouteExport> exportMap) {
            this.exportMap = exportMap;
        }
    }

    public class RouteExport {

        public String answer;

        public String jump;

        public RouteExport(String answer, String jump) {
            this.answer = answer;
            this.jump = jump;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("answer", this.answer);
            json.put("jump", this.jump);
            return json;
        }
    }
}
