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
