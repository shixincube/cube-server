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

import cube.common.entity.Contact;
import cube.hub.Product;
import org.json.JSONObject;

import java.io.File;

/**
 * 微信事件。
 */
public abstract class WeChatEvent extends Event {

    private Contact account;

    private long timestamp;

    public WeChatEvent(String name) {
        super(Product.WeChat, name);
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(long sn, String name) {
        super(Product.WeChat, sn, name);
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(String name, File file) {
        super(Product.WeChat, name, file);
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(long sn, String name, File file) {
        super(Product.WeChat, sn, name, file);
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(long sn, String name, Contact account) {
        super(Product.WeChat, sn, name);
        this.account = account;
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(String name, Contact account) {
        super(Product.WeChat, name);
        this.account = account;
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(String name, File file, Contact account) {
        super(Product.WeChat, name, file);
        this.account = account;
        this.timestamp = System.currentTimeMillis();
    }

    public WeChatEvent(JSONObject json) {
        super(json);
        if (json.has("account")) {
            this.account = new Contact(json.getJSONObject("account"));
        }

        this.timestamp = System.currentTimeMillis();
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public Contact getAccount() {
        return this.account;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.account) {
            json.put("account", this.account.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        if (null != this.account) {
            json.put("account", this.account.toCompactJSON());
        }
        return json;
    }
}
