/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Router {

    public final static String RULE_IF_ELSE = "if-else";

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

    public class Route {

        final Map<String, RouteExport> exportMap;

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
    }
}
