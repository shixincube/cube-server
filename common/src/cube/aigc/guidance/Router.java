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

    public enum Rule {
        /**
         * 是或否规则。
         */
        TrueOrFalse("true-or-false"),

        /**
         * 单选规则。
         */
        SingleChoice("single-choice"),

        /**
         * 多选规则。
         */
        MultipleChoice("multiple-choice"),

        /**
         * 讨论和论述规则。
         */
        Discuss("discuss"),

        ;

        public final String code;

        Rule(String code) {
            this.code = code;
        }

        public static Rule parse(String nameOrCode) {
            for (Rule rule : Rule.values()) {
                if (rule.code.equalsIgnoreCase(nameOrCode)) {
                    return rule;
                }
            }
            return Discuss;
        }
    }

    /**
     * 路由出口为：是。
     */
    public final static String EXPORT_TRUE = "true";

    /**
     * 路由出口为：否。
     */
    public final static String EXPORT_FALSE = "false";

    /**
     * 路由出口为：执行评估规则。
     * 2026-1-2 为了兼容 MINI 评测保留，后续取消该出口。
     */
    public final static String EXPORT_EVALUATION = "evaluation";

    private Rule rule;

    private Map<String, RouteExport> exportMap;

    public Router(JSONObject json) {
        this.rule = Rule.parse(json.getString("rule"));

        this.exportMap = new HashMap<>();
        JSONObject routeJson = json.getJSONObject("route");

        if (routeJson.has(EXPORT_TRUE)) {
            JSONObject data = routeJson.getJSONObject(EXPORT_TRUE);
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
            this.exportMap.put(EXPORT_TRUE, routeExport);
        }

        if (routeJson.has(EXPORT_FALSE)) {
            JSONObject data = routeJson.getJSONObject(EXPORT_FALSE);
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
            this.exportMap.put(EXPORT_FALSE, routeExport);
        }

        if (routeJson.has(EXPORT_EVALUATION)) {
            JSONObject data = routeJson.getJSONObject(EXPORT_EVALUATION);
            RouteExport routeExport = new RouteExport(data.getString("script"));
            this.exportMap.put(EXPORT_EVALUATION, routeExport);
        }
    }

    public Rule getRule() {
        return this.rule;
    }

    public RouteExport getRouteExport(String code) {
        return this.exportMap.get(code);
    }

    public boolean hasEvaluationExport() {
        for (String key : this.exportMap.keySet()) {
            if (key.equals(EXPORT_EVALUATION)) {
                return true;
            }
        }
        return false;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("rule", this.rule.code);

        JSONObject routeJson = new JSONObject();
        Iterator<Map.Entry<String, RouteExport>> iter = this.exportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, RouteExport> e = iter.next();
            routeJson.put(e.getKey(), e.getValue().toJSON());
        }
        json.put("route", routeJson);
        return json;
    }

    public class RouteExport {

        public String jump;

        public Map<String, String> answerMap;

        public String script;

        public RouteExport(String jump, Map<String, String> answerMap) {
            this.jump = jump;
            this.answerMap = answerMap;
        }

        public RouteExport(String script) {
            this.script = script;
        }

        public boolean hasAnswer() {
            return (null != this.answerMap) && (!this.answerMap.isEmpty());
        }

        public boolean isJump() {
            return (null != this.jump);
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            if (null != this.jump) {
                json.put("jump", this.jump);
            }
            if (null != this.script) {
                json.put("script", this.script);
            }

            if (null != this.answerMap) {
                JSONObject answerMapData = new JSONObject();
                for (Map.Entry<String, String> data : this.answerMap.entrySet()) {
                    answerMapData.put(data.getKey(), data.getValue());
                }
                json.put("answer", answerMapData);
            }

            return json;
        }
    }
}
