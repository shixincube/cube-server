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
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 访问文件分享页记录。
 */
public class VisitTrace implements JSONable {

    public final static String PLATFORM_BROWSER = "Browser";

    public final static String PLATFORM_APPLET_WECHAT = "AppletWeChat";

    /**
     * 平台。数据来源的平台描述。
     */
    public String platform;

    /**
     * 访问时间。
     */
    public long time;

    /**
     * 访问地址 。
     */
    public String address;

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
     * 用户代理描述。
     */
    public String userAgent;

    /**
     * 平台代理描述。
     */
    public JSONObject agent;

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

    public VisitTrace(String platform, long time, String address, JSONObject clientTrace) throws JSONException {
        this.platform = platform;
        this.time = time;
        this.address = clientTrace.has("address") ? clientTrace.getString("address") : address;

        this.domain = clientTrace.getString("domain");
        this.url = clientTrace.getString("url");
        this.title = clientTrace.getString("title");

        JSONObject screen = clientTrace.getJSONObject("screen");
        this.screenSize = new Size(screen.getInt("width"), screen.getInt("height"));
        this.screenColorDepth = screen.getInt("colorDepth");
        this.screenOrientation = screen.getString("orientation");

        this.language = clientTrace.getString("language");

        if (clientTrace.has("userAgent")) {
            this.userAgent = clientTrace.getString("userAgent");
        }

        if (clientTrace.has("agent")) {
            this.agent = clientTrace.getJSONObject("agent");
        }

        if (clientTrace.has("event")) {
            this.event = clientTrace.getString("event");
        }
        if (clientTrace.has("eventTag")) {
            this.eventTag = clientTrace.getString("eventTag");
        }
        if (clientTrace.has("eventParam")) {
            this.eventParam = clientTrace.getJSONObject("eventParam");
        }
    }

    public VisitTrace(String platform, long time, String address, String domain, String url, String title,
                      JSONObject screen, String language, String userAgent, JSONObject agent,
                      String event, String eventTag, JSONObject eventParam) {
        this.platform = platform;
        this.time = time;
        this.address = address;
        this.domain = domain;
        this.url = url;
        this.title = title;

        this.screenSize = new Size(screen.getInt("width"), screen.getInt("height"));
        this.screenColorDepth = screen.getInt("colorDepth");
        this.screenOrientation = screen.getString("orientation");

        this.language = language;

        this.userAgent = userAgent;
        this.agent = agent;

        this.event = event;
        this.eventTag = eventTag;
        this.eventParam = eventParam;
    }

    public VisitTrace(JSONObject json) {
        this.platform = json.getString("platform");
        this.time = json.getLong("time");
        this.address = json.getString("address");
        this.domain = json.getString("domain");
        this.url = json.getString("url");
        this.title = json.getString("title");

        JSONObject screen = json.getJSONObject("screen");
        this.screenSize = new Size(screen.getInt("width"), screen.getInt("height"));
        this.screenColorDepth = screen.getInt("colorDepth");
        this.screenOrientation = screen.getString("orientation");

        this.language = json.getString("language");

        if (json.has("userAgent")) {
            this.userAgent = json.getString("userAgent");
        }

        if (json.has("agent")) {
            this.agent = json.getJSONObject("agent");
        }

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

    public JSONObject getScreenJSON() {
        JSONObject json = new JSONObject();
        json.put("width", this.screenSize.width);
        json.put("height", this.screenSize.height);
        json.put("colorDepth", this.screenColorDepth);
        json.put("orientation", this.screenOrientation);
        return json;
    }

    public long getSharerId() {
        if (null != this.eventParam && this.eventParam.has("sharer")) {
            String idString = this.eventParam.getString("sharer");
            try {
                return Trace.parseString(idString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            URI uri = new URI(this.url);
            String query = uri.getQuery();
            String[] array = query.split("&");
            for (String param : array) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if (pair[0].trim().equalsIgnoreCase("s")) {
                        return Trace.parseString(pair[1].trim());
                    }
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public long getParentId() {
        if (null != this.eventParam && this.eventParam.has("parent")) {
            String idString = this.eventParam.getString("parent");
            try {
                return Trace.parseString(idString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            URI uri = new URI(this.url);
            String query = uri.getQuery();
            String[] array = query.split("&");
            for (String param : array) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if (pair[0].trim().equalsIgnoreCase("p")) {
                        return Trace.parseString(pair[1].trim());
                    }
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("platform", this.platform);
        json.put("time", this.time);
        json.put("address", this.address);
        json.put("url", this.url);
        json.put("domain", this.domain);
        json.put("title", this.title);

        JSONObject screen = new JSONObject();
        screen.put("width", this.screenSize.width);
        screen.put("height", this.screenSize.height);
        screen.put("colorDepth", this.screenColorDepth);
        screen.put("orientation", this.screenOrientation);
        json.put("screen", screen);

        json.put("language", this.language);

        if (null != this.userAgent) {
            json.put("userAgent", this.userAgent);
        }

        if (null != this.agent) {
            json.put("agent", this.agent);
        }

        if (null != this.event) {
            json.put("event", this.event);
        }
        if (null != this.eventTag) {
            json.put("eventTag", this.eventTag);
        }
        if (null != this.eventParam) {
            json.put("eventParam", this.eventParam);
        }

        json.put("sharerId", this.getSharerId());
        json.put("parentId", this.getParentId());

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
