/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WordCloud implements JSONable {

    private final static String[] sFilterWords = new String[] {
            ",", ".", "!", "?", "'", "\"", "**", "*", "-", "\n", "\r",
            "@", "#", "$", "%", "^", "&", "(", ")", "[", "]", "{", "}",
            "|", "\\", ":", ";", "<", ">", "=", "+", "`", "~", "/",
            "，", "。", "！", "？", "《", "》", "“", "”", "…", "：", "；",
            "http", "https", "sn", "page", "bigfive", "indicator", "html",
            "7017", "7010"
    };

    private long timestamp;

    private Map<String, AtomicInteger> words;

    public WordCloud() {
        this.timestamp = System.currentTimeMillis();
        this.words = new HashMap<>();
    }

    public void addWord(String word) {
        if (this.isFilterWord(word)) {
            return;
        }

        AtomicInteger value = this.words.get(word);
        if (null == value) {
            this.words.put(word, new AtomicInteger(1));
        }
        else {
            value.incrementAndGet();
        }
    }

    private boolean isFilterWord(String word) {
        if (word.length() > 6) {
            return true;
        }

        for (String w : sFilterWords) {
            if (word.toLowerCase().contains(w)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("timestamp", this.timestamp);
        JSONArray array = new JSONArray();
        for (Map.Entry<String, AtomicInteger> entry : this.words.entrySet()) {
            JSONObject data = new JSONObject();
            data.put("word", entry.getKey());
            data.put("value", entry.getValue().get());
            array.put(data);
        }
        json.put("list", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
