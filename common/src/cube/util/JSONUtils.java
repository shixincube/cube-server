/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cube.benchmark.ResponseTime;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * JSON 辅助函数。
 */
public final class JSONUtils {

    private JSONUtils() {
    }

    /**
     * Map 结构转为 JSON Object 结构。
     *
     * @param map
     * @return
     */
    public static JSONObject toJSONObjectAsLong(Map<String, Long> map) {
        JSONObject json = new JSONObject();
        Iterator<Map.Entry<String, Long>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Long> e = iter.next();
            json.put(e.getKey(), e.getValue().longValue());
        }
        return json;
    }

    /**
     * 将 JSON Object 结构转为 Map 结构。
     *
     * @param json
     * @return
     */
    public static Map<String, Long> toLongMap(JSONObject json) {
        Map<String, Long> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, json.getLong(key));
        }
        return map;
    }

    /**
     * Map 结构转为 JSON Object 结构。
     *
     * @param map
     * @return
     */
    public static JSONObject toJSONObject(Map<String, ? extends JSONable> map) {
        JSONObject json = new JSONObject();
        Iterator<? extends Map.Entry<String, ? extends JSONable>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ? extends JSONable> e = iter.next();
            json.put(e.getKey(), e.getValue().toJSON());
        }
        return json;
    }

    /**
     * Map 结构转为 JSON Object 结构。
     *
     * @param map
     * @return
     */
    public static JSONObject toJSONObjectAsList(Map<String, List<ResponseTime>> map) {
        JSONObject json = new JSONObject();
        Iterator<Map.Entry<String, List<ResponseTime>>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<ResponseTime>> e = iter.next();

            JSONArray array = new JSONArray();
            for (JSONable listValue : e.getValue()) {
                array.put(listValue.toJSON());
            }

            json.put(e.getKey(), array);
        }
        return json;
    }

    /**
     * 将指定的 JSON 对象的数据克隆到新 JSON 对象里。
     *
     * @param src 指定克隆源。
     * @return 返回克隆的新对象。
     */
    public static JSONObject clone(JSONObject src) {
        JSONObject dest = new JSONObject();
        Iterator<String> iter = src.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            Object value = src.get(key);

            if (value instanceof JSONObject) {
                dest.put(key, clone((JSONObject) value));
            }
            else if (value instanceof JSONArray) {
                dest.put(key, clone((JSONArray) value));
            }
            else {
                dest.put(key, value);
            }
        }
        return dest;
    }

    /**
     * 将指定的 JSON 数组对象的数据克隆到新 JSON 数组对象里。
     *
     * @param src
     * @return
     */
    public static JSONArray clone(JSONArray src) {
        JSONArray dest = new JSONArray();
        for (int i = 0; i < src.length(); ++i) {
            Object value = src.get(i);
            if (value instanceof JSONObject) {
                dest.put(clone((JSONObject) value));
            }
            else if (value instanceof JSONArray) {
                dest.put(clone((JSONArray) value));
            }
            else {
                dest.put(value);
            }
        }
        return dest;
    }

    /**
     * 将列表转为存储字符串类型的 JSON 数组。
     *
     * @param list
     * @return
     */
    public static JSONArray toStringArray(List<String> list) {
        JSONArray array = new JSONArray();
        for (String str : list) {
            array.put(str);
        }
        return array;
    }

    /**
     * 将列表转为存储字符串类型的 JSON 数组。
     *
     * @param list
     * @return
     */
    public static JSONArray toStringArray(String[] list) {
        JSONArray array = new JSONArray();
        for (String str : list) {
            array.put(str);
        }
        return array;
    }

    /**
     * 将 JSON 数组转为存储字符串的列表。
     *
     * @param array
     * @return
     */
    public static List<String> toStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            list.add(array.getString(i));
        }
        return list;
    }

    /**
     * 将 JSON 数组转为存储字符串数组。
     *
     * @param array
     * @return
     */
    public static String[] toStringArray(JSONArray array) {
        String[] result = new String[array.length()];
        for (int i = 0; i < array.length(); ++i) {
            result[i] = array.getString(i);
        }
        return result;
    }

    /**
     * 将 JSON 文本里的反斜杠加倍，用于写入 MySQL 的字符串字段。
     *
     * <p>注意：本方法仅做字符替换，实现已被 {@code cube.service.util.JSONStorageUtils#encode(String)}
     * 取代，新代码请直接使用 {@code JSONStorageUtils}。本方法保留仅为兼容旧调用。</p>
     *
     * @param text
     * @return
     */
    public static String serializeEscape(String text) {
        return text.replaceAll("\\\\", "\\\\\\\\");
    }

    /**
     * 将真实换行符替换为 {@code \n} 两个字符。
     *
     * <p>注意：本方法名为 serialize 却曾被用在读取路径上，语义混乱。
     * 读取侧请直接使用 {@code cube.service.util.JSONStorageUtils#decodeObject(String)} /
     * {@code decodeArray(String)}，它们已经内置了历史脏数据的修复逻辑。
     * 本方法保留仅为兼容旧调用。</p>
     *
     * @param text
     * @return
     */
    public static String serializeLineFeed(String text) {
        return text.replaceAll("\\n", "\\\\n");
    }

    /**
     * 反转义数据库读出的文本。
     *
     * @param text
     * @return
     * @deprecated 该实现存在缺陷，请勿继续使用：它把 {@code \"} 还原为 {@code "}、
     * 把 {@code \\} 还原为 {@code \}，会把<b>合法</b>的 JSON 文本破坏成非法 JSON；
     * 且使用 {@code String.replaceAll()} 做纯字符串替换（替换串里 {@code \} 与 {@code $}
     * 有特殊含义）属于误用。
     * 读取路径请改用 {@code cube.service.util.JSONStorageUtils#decodeObject(String)} /
     * {@code decodeArray(String)}：先按标准 JSON 解析，失败时才对历史脏数据做修复，
     * 无法修复时返回 {@code null} 而不是抛出/破坏数据。
     */
    @Deprecated
    public static String deserializeEscape(String text) {
        String result = text.replaceAll("\\\\\"", "\\\"");
        result = result.replaceAll("\\\\n", "\\n");
        result = result.replaceAll("\\\\\\\\", "\\\\");
        return result;
    }

//    public static String deserializeEscape(String text) {
//        String result = text.replaceAll("\\\\\"", "\\\"");
//        return result.replaceAll("\\n", "\\\\n");
//    }

    public static void main(String[] args) {
        JSONObject json = new JSONObject();

        String content = "X\"JW\"，结束。\n下一行\\前面是斜线";

        json.put("reference", "Abnormal");
        json.put("content", content);
        System.out.println("Raw : " + json.toString());

        String dbString = serializeEscape(json.toString());
        System.out.println("Seri: " + dbString);

        String rawString = deserializeEscape(dbString);
        System.out.println("Desi: " + rawString);

        json = new JSONObject(rawString);
        System.out.println(json.getString("content"));
    }
}
