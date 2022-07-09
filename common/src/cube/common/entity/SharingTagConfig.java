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

    private FileLabel fileLabel;

    private long expiryDate;

    private String password;

    private List<AbstractContact> includeList;

    private List<AbstractContact> excludeList;

    public SharingTagConfig(Contact contact, FileLabel fileLabel, int durationInDay) {
        this.contact = contact;
        this.fileLabel = fileLabel;
        this.expiryDate = System.currentTimeMillis() + (durationInDay * 24L * 60 * 60 * 1000);
    }

    public SharingTagConfig(Contact contact, FileLabel fileLabel, long expiryDate, String password) {
        this.contact = contact;
        this.fileLabel = fileLabel;
        this.expiryDate = expiryDate;
        this.password = password;
    }

    public SharingTagConfig(JSONObject json) {
        this.contact = new Contact(json.getJSONObject("contact"));
        this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        this.expiryDate = json.getLong("expiryDate");

        if (json.has("password")) {
            this.password = json.getString("password");
        }

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

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public long getExpiryDate() {
        return this.expiryDate;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contact", this.contact.toJSON());
        json.put("fileLabel", this.fileLabel.toJSON());
        json.put("expiryDate", this.expiryDate);

        if (null != this.password) {
            json.put("password", this.password);
        }

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
        return this.toJSON();
    }
}
