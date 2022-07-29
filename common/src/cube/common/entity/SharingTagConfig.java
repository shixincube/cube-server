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

import cube.common.Domain;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件分享操作配置。
 */
public class SharingTagConfig implements JSONable {

    private Contact contact;

    private Device device;

    private FileLabel fileLabel;

    private long duration;

    private String password;

    /**
     * 是否支持预览。
     */
    private boolean preview;

    /**
     * 是否允许下载。
     */
    private boolean download;

    private List<AbstractContact> includeList;

    private List<AbstractContact> excludeList;

    public SharingTagConfig(Contact contact, Device device, FileLabel fileLabel, long duration, String password,
                            boolean preview, boolean download) {
        this.contact = contact;
        this.device = device;
        this.fileLabel = fileLabel;
        this.duration = duration;
        this.password = password;
        this.preview = preview;
        this.download = !preview || download;
    }

    public SharingTagConfig(JSONObject json) {
        if (json.has("contact")) {
            this.contact = new Contact(json.getJSONObject("contact"));
        }

        if (json.has("device")) {
            this.device = new Device(json.getJSONObject("device"));
        }

        this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        this.duration = json.getLong("duration");

        if (json.has("password")) {
            this.password = json.getString("password");
        }

        this.preview = json.getBoolean("preview");
        this.download = json.getBoolean("download");

        if (json.has("includeList")) {
            this.includeList = new ArrayList<>();
            JSONArray array = json.getJSONArray("includeList");
            for (int i = 0; i < array.length(); ++i) {
                AbstractContact current = ContactHelper.create(array.getJSONObject(i));
                this.includeList.add(current);
            }
        }

        if (json.has("excludeList")) {
            this.excludeList = new ArrayList<>();
            JSONArray array = json.getJSONArray("excludeList");
            for (int i = 0; i < array.length(); ++i) {
                AbstractContact current = ContactHelper.create(array.getJSONObject(i));
                this.excludeList.add(current);
            }
        }
    }

    public Domain getDomain() {
        return this.contact.getDomain();
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public long getDuration() {
        return this.duration;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean isPreview() {
        return this.preview;
    }

    public boolean isDownloadAllowed() {
        return this.download;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.contact) {
            json.put("contact", this.contact.toJSON());
        }

        if (null != this.device) {
            json.put("device", this.device.toJSON());
        }

        json.put("fileLabel", this.fileLabel.toJSON());
        json.put("duration", this.duration);

        if (null != this.password) {
            json.put("password", this.password);
        }

        json.put("preview", this.preview);
        json.put("download", this.download);

        if (null != this.includeList) {
            JSONArray array = new JSONArray();
            for (AbstractContact c : this.includeList) {
                array.put(c.toJSON());
            }
            json.put("includeList", array);
        }

        if (null != this.excludeList) {
            JSONArray array = new JSONArray();
            for (AbstractContact c : this.excludeList) {
                array.put(c.toJSON());
            }
            json.put("excludeList", array);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("contact")) {
            json.remove("contact");
            json.put("contact", this.contact.toBasicJSON());
        }

        if (json.has("fileLabel")) {
            json.remove("fileLabel");
            json.put("fileLabel", this.fileLabel.toCompactJSON());
        }

        if (json.has("device")) {
            json.remove("device");
        }
        return json;
    }
}
