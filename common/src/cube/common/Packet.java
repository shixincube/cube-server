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

package cube.common;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;

/**
 * 来自客户端数据通道的包结构。
 */
public class Packet implements JSONable {

    /**
     * 封包的唯一序号。
     */
    public Long sn;

    /**
     * 封包名称。
     */
    public String name;

    /**
     * 封包的负载数据。
     */
    public JSONObject data;

    public Packet(ActionDialect dialect) {
        this.name = dialect.getName();
        this.sn = dialect.getParamAsLong("sn");
        this.data = dialect.getParamAsJson("data");
    }

    public Packet(String name, JSONObject data) {
        this(Utils.generateSerialNumber(), name, data);
    }

    public Packet(Long sn, String name, JSONObject data) {
        this.sn = sn;
        this.name = name;
        this.data = data;
    }

    public ActionDialect toDialect() {
        ActionDialect result = new ActionDialect(this.name);
        result.addParam("sn", this.sn.longValue());
        result.addParam("data", this.data);
        return result;
    }

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

    public static ActionDialect toActionDialect(Packet packet) {
        ActionDialect result = new ActionDialect(packet.name);
        result.addParam("sn", packet.sn.longValue());
        result.addParam("data", packet.data);
        return result;
    }
}
