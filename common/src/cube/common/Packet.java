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

package cube.common;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 来自客户端数据通道的包结构。
 */
public class Packet implements JSONable {

    /**
     * 封包的唯一序号。
     */
    public final Long sn;

    /**
     * 封包名称。
     */
    public final String name;

    /**
     * 封包的负载数据。
     */
    public final JSONObject data;

    /**
     * 构造函数。
     *
     * @param dialect 指定动作方言。
     */
    public Packet(ActionDialect dialect) {
        this.name = dialect.getName();
        this.sn = dialect.getParamAsLong("sn");
        this.data = dialect.getParamAsJson("data");
    }

    /**
     * 构造函数。
     *
     * @param name 指定封包名。
     * @param data 指定封包负载。
     */
    public Packet(String name, JSONObject data) {
        this(Utils.generateSerialNumber(), name, data);
    }

    /**
     * 构造函数。
     *
     * @param sn 指定序列号。
     * @param name 指定封包名。
     * @param data 指定封包负载。
     */
    public Packet(Long sn, String name, JSONObject data) {
        this.sn = sn;
        this.name = name;
        this.data = data;
    }

    /**
     * 序列化为动作方言。
     *
     * @return 返回动作方言。
     */
    public ActionDialect toDialect() {
        ActionDialect result = new ActionDialect(this.name);
        result.addParam("sn", this.sn.longValue());
        result.addParam("data", this.data);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("sn", this.sn);
            json.put("name", this.name);
            json.put("data", this.data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 提取状态码。
     *
     * @param packet
     * @return
     */
    public static int extractCode(Packet packet) {
        return packet.data.getInt("code");
    }

    /**
     * 提取数据负载内容。
     *
     * @param packet
     * @return
     */
    public static JSONObject extractDataPayload(Packet packet) {
        return packet.data.getJSONObject("data");
    }
}
