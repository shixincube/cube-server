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

import cell.core.net.Endpoint;
import cell.util.Utils;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 分享标签。
 */
public class SharingTag extends Entity {

    /**
     * 一般状态。
     */
    public final static int STATE_NORMAL = 0;

    /**
     * 已取消状态。
     */
    public final static int STATE_CANCEL = 1;

    private String code;

    private Trace sharer;

    private Trace parent;

    private SharingTagConfig config;

    private long expiryDate;

    private String httpHostInfo;
    private String httpsHostInfo;

    private String httpURL;

    private String httpsURL;

    private List<FileLabel> previewList;

    private int state = STATE_NORMAL;

    public SharingTag(SharingTagConfig config) {
        super(Utils.generateSerialNumber(), config.getDomain());
        this.config = config;

        this.sharer = new Trace(config.getContact());
        this.parent = new Trace(config.getContact());

        StringBuilder buf = new StringBuilder();
        buf.append(config.getFileLabel().getFileCode());
        buf.append(config.getDomain().getName());
        buf.append(this.id.toString());
        buf.append(config.getContact().getId().toString());
        this.code = FileUtils.fastHash(buf.toString());

        if (config.getDuration() > 0) {
            this.expiryDate = this.timestamp + config.getDuration();
        }
        else {
            this.expiryDate = 0;
        }
    }

    public SharingTag(Long id, String domain, long timestamp, String code, long expiryDate,
                      Contact contact, Device device, FileLabel fileLabel, long duration,
                      String password, boolean preview, boolean download, int state) {
        super(id, domain, timestamp);
        this.code = code;
        this.expiryDate = expiryDate;
        this.config = new SharingTagConfig(contact, device, fileLabel, duration, password, preview, download);

        this.sharer = new Trace(this.config.getContact());
        this.parent = new Trace(this.config.getContact());

        this.state = state;
    }

    public SharingTag(JSONObject json) {
        super(json);
        this.code = json.getString("code");
        this.config = new SharingTagConfig(json.getJSONObject("config"));
        this.expiryDate = json.getLong("expiryDate");

        this.sharer = new Trace(this.config.getContact());
        this.parent = new Trace(this.config.getContact());

        if (json.has("httpURL")) {
            this.httpURL = json.getString("httpURL");
        }
        if (json.has("httpsURL")) {
            this.httpsURL = json.getString("httpsURL");
        }

        if (json.has("httpHostInfo")) {
            this.httpHostInfo = json.getString("httpHostInfo");
        }
        if (json.has("httpsHostInfo")) {
            this.httpsHostInfo = json.getString("httpsHostInfo");
        }

        if (json.has("previewList")) {
            this.previewList = new ArrayList<>();
            JSONArray array = json.getJSONArray("previewList");
            for (int i = 0; i < array.length(); ++i) {
                this.previewList.add(new FileLabel(array.getJSONObject(i)));
            }
        }

        if (json.has("sharer")) {
            this.sharer = new Trace(json.getJSONObject("sharer"));
        }

        if (json.has("parent")) {
            this.parent = new Trace(json.getJSONObject("parent"));
        }
    }

    public String getCode() {
        return this.code;
    }

    public long getExpiryDate() {
        return this.expiryDate;
    }

    public SharingTagConfig getConfig() {
        return this.config;
    }

    public void resetContact(Contact contact) {
        this.config.setContact(contact);
    }

    public void setURLs(Endpoint http, Endpoint https) {
        this.httpHostInfo = http.getHost() + ":" + http.getPort();
        this.httpsHostInfo = https.getHost() + ":" + https.getPort();

        String parameter = "?s=" + this.sharer.toString() + "&p=" + this.parent.toString();

        this.httpURL = "http://" + this.httpHostInfo +
                "/sharing/" + this.code + parameter;

        this.httpsURL = "https://" + this.httpsHostInfo +
                "/sharing/" + this.code + parameter;
    }

    public String getHttpURL() {
        return this.httpURL;
    }

    public String getHttpsURL() {
        return this.httpsURL;
    }

    public void setPreviewList(List<FileLabel> list) {
        if (null == list || list.isEmpty()) {
            return;
        }

        if (null == this.previewList) {
            this.previewList = new ArrayList<>();
        }

        this.previewList.clear();
        this.previewList.addAll(list);
    }

    public List<FileLabel> getPreviewList() {
        return this.previewList;
    }

    public int getState() {
        return this.state;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("code", this.code);
        json.put("expiryDate", this.expiryDate);
        json.put("config", this.config.toJSON());

        if (null != this.httpURL) {
            json.put("httpURL", this.httpURL);
        }
        if (null != this.httpsURL) {
            json.put("httpsURL", this.httpsURL);
        }

        if (null != this.httpHostInfo) {
            json.put("httpHostInfo", this.httpHostInfo);
        }
        if (null != this.httpsHostInfo) {
            json.put("httpsHostInfo", this.httpsHostInfo);
        }

        if (null != this.sharer) {
            json.put("sharer", this.sharer.toJSON());
        }
        if (null != this.parent) {
            json.put("parent", this.parent.toJSON());
        }

        if (null != this.previewList) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.previewList) {
                array.put(fileLabel.toJSON());
            }
            json.put("previewList", array);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("code", this.code);
        json.put("expiryDate", this.expiryDate);
        json.put("config", this.config.toCompactJSON());

        if (null != this.httpURL) {
            json.put("httpURL", this.httpURL);
        }
        if (null != this.httpsURL) {
            json.put("httpsURL", this.httpsURL);
        }

        if (null != this.httpHostInfo) {
            json.put("httpHostInfo", this.httpHostInfo);
        }
        if (null != this.httpsHostInfo) {
            json.put("httpsHostInfo", this.httpsHostInfo);
        }

        if (null != this.sharer) {
            json.put("sharer", this.sharer.toJSON());
        }
        if (null != this.parent) {
            json.put("parent", this.parent.toJSON());
        }

        if (null != this.previewList) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.previewList) {
                array.put(fileLabel.toCompactJSON());
            }
            json.put("previewList", array);
        }

        return json;
    }

    public static String[] makeURLs(SharingTag tag, String sharer) {
        String parameter = "?s=" + sharer + "&p=" + tag.sharer.toString();

        String httpURL = "http://" + tag.httpHostInfo + "/sharing/" + tag.code + parameter;
        String httpsURL = "https://" + tag.httpsHostInfo + "/sharing/" + tag.code + parameter;
        return new String[]{ httpURL, httpsURL };
    }
}
