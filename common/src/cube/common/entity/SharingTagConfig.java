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

    /**
     * 分享标签的持续有效时长。
     */
    private long duration;

    /**
     * 分享标签密码。
     */
    private String password;

    /**
     * 是否支持预览。
     */
    private boolean preview;

    /**
     * 是否允许下载。
     */
    private boolean download;

    /**
     * 是否追踪下载。
     */
    private boolean traceDownload = true;

    private List<AbstractContact> includeList;

    private List<AbstractContact> excludeList;

    public SharingTagConfig(Contact contact, Device device, FileLabel fileLabel, long duration, String password,
                            boolean preview, boolean download, boolean traceDownload) {
        this.contact = contact;
        this.device = device;
        this.fileLabel = fileLabel;
        this.duration = duration;
        this.password = password;
        this.preview = preview;
        this.download = !preview || download;
        this.traceDownload = traceDownload;
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
        this.traceDownload = json.getBoolean("traceDownload");

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

    /**
     * 获取域信息。
     *
     * @return 返回域信息。
     */
    public Domain getDomain() {
        return this.contact.getDomain();
    }

    /**
     * 设置标签所属的联系人。
     *
     * @param contact 标签所属的联系人。
     */
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    /**
     * 获取标签所属的联系人。
     *
     * @return 返回标签所属的联系人。
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取创建分享标签的设备。
     *
     * @return 返回创建分享标签的设备。
     */
    public Device getDevice() {
        return this.device;
    }

    /**
     * 获取分享标签对应的文件。
     *
     * @return 返回分享标签对应的文件。
     */
    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    /**
     * 获取分享的有效时长。
     *
     * @return 获取分享的有效时长，单位：毫秒。
     */
    public long getDuration() {
        return this.duration;
    }

    /**
     * 获取查看分享的密码。
     *
     * @return 返回分享密码。
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * 该分享标签是否设置了密码。
     *
     * @return 如果设置了密码返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean hasPassword() {
        return (null != this.password);
    }

    /**
     * 是否为文件创建预览图。
     *
     * @return 如果创建了预览图返回 {@code true} 。
     */
    public boolean isPreview() {
        return this.preview;
    }

    /**
     * 设置是否为文件创建预览图。
     *
     * @param value 创建预览图设置 {@code true} 。
     */
    public void setPreview(boolean value) {
        this.preview = value;
    }

    /**
     * 是否允许下载文件。
     *
     * @return 如果允许下载文件返回 {@code true} 。
     */
    public boolean isDownloadAllowed() {
        return this.download;
    }

    /**
     * 是否强制追踪下载事件，如果返回 {@code true} 表示需要登录系统才能下载文件。
     *
     * @return 如果返回 {@code true} 表示需要登录系统才能下载文件。
     */
    public boolean isTraceDownload() {
        return this.traceDownload;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.contact) {
            json.put("contact", this.contact.toBasicJSON());
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
        json.put("traceDownload", this.traceDownload);

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
