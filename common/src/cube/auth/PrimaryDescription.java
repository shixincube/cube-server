/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.auth;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.JSONable;

/**
 * 主访问主机描述。
 */
public class PrimaryDescription implements JSONable {

    /**
     * 主机主地址。
     */
    private String address;

    /**
     * 文件存储描述。
     */
    private JSONObject primaryContent;

    /**
     * 构造函数。
     *
     * @param address 主地址。
     * @param primaryContent 首要信息内容。
     */
    public PrimaryDescription(String address, JSONObject primaryContent) {
        this.address = address;
        this.primaryContent = primaryContent;
    }

    /**
     * 构造函数。
     *
     * @param json 首要信息的 JSON 对象。
     */
    public PrimaryDescription(JSONObject json) {
        try {
            this.address = json.getString("address");
            this.primaryContent = json.getJSONObject("primaryContent");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取地址。
     *
     * @return 返回地址信息。
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * 获取首要信息内容。
     *
     * @return 返回 JSON 格式的信息内容。
     */
    public JSONObject getPrimaryContent() {
        return this.primaryContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("address", this.address);
            json.put("primaryContent", this.primaryContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
