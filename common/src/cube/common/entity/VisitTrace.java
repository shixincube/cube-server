/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.log.Logger;
import cube.common.JSONable;
import cube.vision.Size;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 访问文件分享页记录。
 */
public class VisitTrace implements JSONable {

    public final static String PLATFORM_BROWSER = "Browser";

    public final static String PLATFORM_APPLET_WECHAT = "AppletWeChat";

    /**
     * 追踪码。
     */
    public String code;

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
     * @see TraceEvent
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

    /**
     * 触发该记录的联系人的 ID 。
     */
    public long contactId = 0;

    /**
     * 触发该记录的联系人的域。
     */
    public String contactDomain = null;

    /**
     * 发起分享的联系人 ID 。
     */
    public long sharerId = 0;

    /**
     * 上一级分享人的 ID 。
     */
    public long parentId = 0;

    /**
     * 下一级节点。
     */
    private List<VisitTrace> sublevelNodes;

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

        if (json.has("contactId")) {
            this.contactId = json.getLong("contactId");
        }
        if (json.has("contactDomain")) {
            this.contactDomain = json.getString("contactDomain");
        }
    }

    public String getCode() {
        return this.code;
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
        if (0 != this.sharerId) {
            return this.sharerId;
        }

        if (null != this.eventParam && this.eventParam.has("sharer")) {
            String idString = this.eventParam.getString("sharer");
            try {
                this.sharerId = Trace.parseString(idString);
                return this.sharerId;
            } catch (Exception e) {
                Logger.w(this.getClass(), "#getSharerId", e);
            }
        }

        try {
            URI uri = new URI(this.url);
            String query = uri.getQuery();
            if (null == query) {
                return 0;
            }

            String[] array = query.split("&");
            for (String param : array) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if (pair[0].trim().equalsIgnoreCase("s")) {
                        this.sharerId = Trace.parseString(pair[1].trim());
                        break;
                    }
                }
            }
        } catch (URISyntaxException e) {
            Logger.w(this.getClass(), "#getSharerId", e);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#getSharerId", e);
        }

        return this.sharerId;
    }

    public long getParentId() {
        if (0 != this.parentId) {
            return this.parentId;
        }

        if (null != this.eventParam && this.eventParam.has("parent")) {
            String idString = this.eventParam.getString("parent");
            try {
                this.parentId = Trace.parseString(idString);
                return this.parentId;
            } catch (Exception e) {
                Logger.w(this.getClass(), "#getParentId", e);
            }
        }

        try {
            URI uri = new URI(this.url);
            String query = uri.getQuery();
            if (null == query) {
                return 0;
            }

            String[] array = query.split("&");
            for (String param : array) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if (pair[0].trim().equalsIgnoreCase("p")) {
                        String str = pair[1].trim();
                        if (str.indexOf("%") > 0) {
                            str = str.substring(0, str.indexOf("%"));
                        }
                        this.parentId = Trace.parseString(str);
                        break;
                    }
                }
            }
        } catch (URISyntaxException e) {
            Logger.w(this.getClass(), "#getParentId", e);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#getParentId", e);
        }

        return this.parentId;
    }

    public void addSublevel(List<VisitTrace> traces) {
        synchronized (this) {
            if (null == this.sublevelNodes) {
                this.sublevelNodes = new ArrayList<>();
            }

            this.sublevelNodes.addAll(traces);
        }
    }

    public List<VisitTrace> getSublevelNodes() {
        return this.sublevelNodes;
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

        json.put("contactId", this.contactId);

        if (null != this.contactDomain) {
            json.put("contactDomain", this.contactDomain);
        }

        json.put("sharerId", this.getSharerId());
        json.put("parentId", this.getParentId());

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public JSONObject toMiniJSON() {
        JSONObject json = new JSONObject();
        json.put("platform", this.platform);
        json.put("time", this.time);
        json.put("address", this.address);
        json.put("contactId", this.contactId);
        return json;
    }
}
