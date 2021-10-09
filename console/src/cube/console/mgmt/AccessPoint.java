/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.console.mgmt;

import cell.core.net.Endpoint;
import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 服务器的接入点。
 */
public class AccessPoint extends Endpoint implements JSONable {

    public final int maxConnection;

    public AccessPoint(String host, int port, int maxConnection) {
        super(host, port);
        this.maxConnection = maxConnection;
    }

    public AccessPoint(JSONObject data) throws JSONException {
        super(data);
        this.maxConnection = data.has("maxConnection") ? data.getInt("maxConnection") : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof AccessPoint) {
            AccessPoint other = (AccessPoint) object;
            if (other.getHost().equals(this.getHost()) && other.getPort() == this.getPort() &&
                other.maxConnection == this.maxConnection) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("maxConnection", this.maxConnection);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
