/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.common.entity;

import cube.common.JSONable;
import cube.vision.Size;
import org.json.JSONObject;

/**
 * 访问文件分享页记录。
 */
public class VisitTrace implements JSONable {

    /**
     * 访问时间。
     */
    public long time;

    /**
     * 访问 IP 。
     */
    public String ip;

    /**
     * 域名。
     */
    public String domain;

    /**
     * URL 。
     */
    public String url;

    /**
     * 页面标题。
     */
    public String title;

    /**
     * 屏幕分辨率。
     */
    public Size screenSize;

    /**
     * 屏幕色深。
     */
    public int screenColorDepth;

    /**
     * 屏幕方向。
     */
    public String screenOrientation;

    /**
     * Referrer
     */
    public String referrer;

    /**
     * 浏览器信息。
     */
    public String userAgent;

    /**
     * 客户端语言。
     */
    public String language;

    /**
     * 事件。
     */
    public String event;

    /**
     * 事件标签。
     */
    public String eventTag;

    /**
     * 事件参数。
     */
    public JSONObject eventParam;

    public VisitTrace(long time, String ip, JSONObject clientTrace) {
        this.time = time;
        this.ip = ip;
        this.domain = clientTrace.getString("domain");
        this.url = clientTrace.getString("url");
        this.title = clientTrace.getString("title");

        JSONObject screen = clientTrace.getJSONObject("screen");
        this.screenSize = new Size(screen.getInt("width"), screen.getInt("height"));
        this.screenColorDepth = screen.getInt("colorDepth");
        this.screenOrientation = screen.getString("orientation");

        this.referrer = clientTrace.getString("referrer");
        this.language = clientTrace.getString("language");
        this.userAgent = clientTrace.getString("userAgent");
    }

    public VisitTrace(JSONObject json) {
        this.time = json.getLong("time");
        this.ip = json.getString("ip");
        this.domain = json.getString("domain");
        this.url = json.getString("url");
        this.title = json.getString("title");

        JSONObject screen = json.getJSONObject("screen");
        this.screenSize = new Size(screen.getInt("width"), screen.getInt("height"));
        this.screenColorDepth = screen.getInt("colorDepth");
        this.screenOrientation = screen.getString("orientation");

        this.referrer = json.getString("referrer");
        this.language = json.getString("language");
        this.userAgent = json.getString("userAgent");

        if (json.has("event")) {
            this.event = json.getString("event");
        }
        if (json.has("eventTag")) {
            this.eventTag = json.getString("eventTag");
        }
        if (json.has("eventParam")) {
            this.eventParam = json.getJSONObject("eventParam");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("time", this.time);
        json.put("ip", this.ip);
        json.put("domain", this.domain);
        json.put("url", this.url);
        json.put("title", this.title);

        JSONObject screen = new JSONObject();
        screen.put("width", this.screenSize.width);
        screen.put("height", this.screenSize.height);
        screen.put("colorDepth", this.screenColorDepth);
        screen.put("orientation", this.screenOrientation);
        json.put("screen", screen);

        json.put("referrer", this.referrer);
        json.put("language", this.language);
        json.put("userAgent", this.userAgent);

        if (null != this.event) {
            json.put("event", this.event);
        }
        if (null != this.eventTag) {
            json.put("eventTag", this.eventTag);
        }
        if (null != this.eventParam) {
            json.put("eventParam", this.eventParam);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
