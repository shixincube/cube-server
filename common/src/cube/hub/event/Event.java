/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.hub.event;

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.ClientDescription;
import cube.common.entity.FileLabel;
import cube.hub.Product;
import org.json.JSONObject;

import java.io.File;

/**
 * 事件描述。
 */
public abstract class Event implements JSONable {

    private long sn;

    private String code;

    protected Product product;

    protected String name;

    protected File file;

    protected FileLabel fileLabel;

    protected ClientDescription description;

    public Event(Product product, String name) {
        this.product = product;
        this.sn = Utils.generateSerialNumber();
        this.name = name;
    }

    public Event(Product product, String name, File file) {
        this.product = product;
        this.sn = Utils.generateSerialNumber();
        this.name = name;
        this.file = file;
    }

    public Event(Product product, long sn, String name) {
        this.product = product;
        this.sn = sn;
        this.name = name;
    }

    public Event(Product product, long sn, String name, File file) {
        this.product = product;
        this.sn = sn;
        this.name = name;
        this.file = file;
    }

    public Event(JSONObject json) {
        this.product = Product.parse(json.getString("product"));
        this.sn = json.getLong("sn");
        this.name = json.getString("name");

        if (json.has("code")) {
            this.code = json.getString("code");
        }

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }

        if (json.has("description")) {
            this.description = new ClientDescription(json.getJSONObject("description"));
        }
    }

    public long getSerialNumber() {
        return this.sn;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public Product getProduct() {
        return this.product;
    }

    public String getName() {
        return this.name;
    }

    public File getFile() {
        return this.file;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void setDescription(ClientDescription description) {
        this.description = description;
    }

    public ClientDescription getDescription() {
        return this.description;
    }

    public Long getPretenderId() {
        return (null != this.description) ? this.description.getPretender().getId() : null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("product", this.product.name);
        json.put("sn", this.sn);
        json.put("name", this.name);

        if (null != this.code) {
            json.put("code", this.code);
        }

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }

        if (null != this.description) {
            json.put("description", this.description.toCompactJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("product", this.product.name);
        json.put("sn", this.sn);
        json.put("name", this.name);

        if (null != this.code) {
            json.put("code", this.code);
        }

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toCompactJSON());
        }

        return json;
    }
}
