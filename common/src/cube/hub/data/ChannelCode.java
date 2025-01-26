/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.data;

import cube.common.JSONable;
import cube.hub.Product;
import org.json.JSONObject;

/**
 * 通道码。
 */
public class ChannelCode implements JSONable {

    /**
     * 状态：可用。
     */
    public final static int ENABLED = 0;

    /**
     * 状态：禁用。
     */
    public final static int DISABLED = 1;

    /**
     * 状态：其他。
     */
    public final static int OTHER = 9;


    /**
     * 访问码。
     */
    public final String code;

    /**
     * 创建时间。
     */
    public final long creation;

    /**
     * 到期时间。
     */
    public final long expiration;

    /**
     * 产品归属。
     */
    public final Product product;

    /**
     * 状态。
     */
    public final int state;

    public ChannelCode(String code, long creation, long expiration, Product product, int state) {
        this.code = code;
        this.creation = creation;
        this.expiration = expiration;
        this.product = product;
        this.state = state;
    }

    public ChannelCode(JSONObject json) {
        this.code = json.getString("code");
        this.creation = json.getLong("creation");
        this.expiration = json.getLong("expiration");
        this.product = Product.parse(json.getString("product"));
        this.state = json.getInt("state");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("code", this.code);
        json.put("creation", this.creation);
        json.put("expiration", this.expiration);
        json.put("product", this.product.name);
        json.put("state", this.state);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
