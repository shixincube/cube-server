/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
}
