/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import cube.hub.Product;
import cube.hub.data.DataHelper;
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
            json.put("account", DataHelper.filterContactAvatarFileLabel(this.account));
        }
        return json;
    }
}
