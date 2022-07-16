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

    private String code;

    private SharingTagConfig config;

    private long expiryDate;

    private String httpURL;

    private String httpsURL;

    private List<FileLabel> previewList;

    private List<VisitTrace> visitTraceList;

    public SharingTag(SharingTagConfig config) {
        super(Utils.generateSerialNumber(), config.getDomain());
        this.config = config;

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
                      String password, boolean preview, boolean download) {
        super(id, domain, timestamp);
        this.code = code;
        this.expiryDate = expiryDate;
        this.config = new SharingTagConfig(contact, device, fileLabel, duration, password, preview, download);
    }

    public SharingTag(JSONObject json) {
        super(json);
        this.code = json.getString("code");
        this.config = new SharingTagConfig(json.getJSONObject("config"));
        this.expiryDate = json.getLong("expiryDate");

        if (json.has("httpURL")) {
            this.httpURL = json.getString("httpURL");
        }
        if (json.has("httpsURL")) {
            this.httpsURL = json.getString("httpsURL");
        }

        if (json.has("previewList")) {
            this.previewList = new ArrayList<>();
            JSONArray array = json.getJSONArray("previewList");
            for (int i = 0; i < array.length(); ++i) {
                this.previewList.add(new FileLabel(array.getJSONObject(i)));
            }
        }

        if (json.has("visitTraceList")) {
            this.visitTraceList = new ArrayList<>();
            JSONArray array = json.getJSONArray("visitTraceList");
            for (int i = 0; i < array.length(); ++i) {
                this.visitTraceList.add(new VisitTrace(array.getJSONObject(i)));
            }
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

    public void setHttpURL(String url) {
        this.httpURL = url;
    }

    public void setHttpsURL(String url) {
        this.httpsURL = url;
    }

    public void setURLs(Endpoint http, Endpoint https) {
        this.httpURL = "http://" + http.getHost() + ":" + http.getPort() +
                "/sharing/" + this.code;
        this.httpsURL = "https://" + https.getHost() + ":" + https.getPort() +
                "/sharing/" + this.code;
    }

    public void setPreviewList(List<FileLabel> list) {
        if (null == this.previewList) {
            this.previewList = new ArrayList<>();
        }

        this.previewList.clear();
        this.previewList.addAll(list);
    }

    public List<FileLabel> getPreviewList() {
        return this.previewList;
    }

    public List<VisitTrace> getVisitTraceList() {
        return this.visitTraceList;
    }

    public void addVisitTrace(VisitTrace visitTrace) {
        this.visitTraceList.add(visitTrace);
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

        if (null != this.previewList) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.previewList) {
                array.put(fileLabel.toJSON());
            }
            json.put("previewList", array);
        }

        if (null != this.visitTraceList) {
            JSONArray array = new JSONArray();
            for (VisitTrace visitTrace : this.visitTraceList) {
                array.put(visitTrace.toJSON());
            }
            json.put("visitTraceList", array);
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

        if (null != this.previewList) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.previewList) {
                array.put(fileLabel.toJSON());
            }
            json.put("previewList", array);
        }

        return json;
    }
}
