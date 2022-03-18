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

package cube.hub.event;

import cube.common.JSONable;
import cube.common.entity.FileLabel;
import cube.hub.Product;
import org.json.JSONObject;

import java.io.File;

/**
 * 事件描述。
 */
public abstract class Event implements JSONable {

    protected Product product;

    protected String name;

    protected File file;

    protected FileLabel fileLabel;

    public Event(Product product, String name) {
        this.product = product;
        this.name = name;
    }

    public Event(Product product, String name, File file) {
        this.product = product;
        this.name = name;
        this.file = file;
    }

    public Event(JSONObject json) {
        this.product = Product.parse(json.getString("product"));
        this.name = json.getString("name");

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }
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

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("product", this.product.name);
        json.put("name", this.name);

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toCompactJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
