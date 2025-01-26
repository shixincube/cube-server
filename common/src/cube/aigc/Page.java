/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 页面。
 */
public class Page implements JSONable {

    public String url;

    public String title;

    public List<String> textList;

    public List<String> imageList;

    public Page(JSONObject json) {
        if (json.has("url")) {
            this.url = json.getString("url");
        }
        else if (json.has("currentUrl")) {
            this.url = json.getString("currentUrl");
        }

        if (json.has("title")) {
            this.title = json.getString("title");
        }
        if (json.has("textList")) {
            this.textList = new ArrayList<>();
            JSONArray array = json.getJSONArray("textList");
            for (int i = 0; i < array.length(); ++i) {
                this.textList.add(array.getString(i).trim());
            }
        }
        if (json.has("imgList")) {
            this.imageList = new ArrayList<>();
            JSONArray array = json.getJSONArray("imgList");
            for (int i = 0; i < array.length(); ++i) {
                this.imageList.add(array.getString(i));
            }
        }
    }

    public void filterShortText() {
        if (null == this.textList) {
            return;
        }

        Iterator<String> iter = this.textList.iterator();
        while (iter.hasNext()) {
            String text = iter.next();
            if (text.length() <= 5) {
                iter.remove();
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.url) {
            json.put("url", this.url);
        }

        if (null != this.title) {
            json.put("title", this.title);
        }

        if (null != this.textList) {
            JSONArray array = new JSONArray();
            for (String text : this.textList) {
                array.put(text);
            }
            json.put("textList", array);
        }

        if (null != this.imageList) {
            JSONArray array = new JSONArray();
            for (String url : this.imageList) {
                array.put(url);
            }
            json.put("imgList", array);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        if (null != this.url) {
            json.put("url", this.url);
        }

        if (null != this.title) {
            json.put("title", this.title);
        }
        return json;
    }
}
