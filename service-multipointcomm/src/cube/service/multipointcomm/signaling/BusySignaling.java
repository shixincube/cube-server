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

package cube.service.multipointcomm.signaling;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;

/**
 * Busy 信令。
 */
public class BusySignaling extends Signaling {

    private Contact caller;

    private Contact callee;

    public BusySignaling(CommField commField, Contact contact, Device device, Long rtcSN) {
        super(MultipointCommAction.Busy.name, commField, contact, device, rtcSN);
    }

    public BusySignaling(JSONObject json) {
        super(json);

        try {
            if (json.has("caller")) {
                this.caller = new Contact(json.getJSONObject("caller"));
            }
            if (json.has("callee")) {
                this.callee = new Contact(json.getJSONObject("callee"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void copy(BusySignaling source) {
        this.caller = source.caller;
        this.callee = source.callee;
    }

    public void setCaller(Contact caller) {
        this.caller = caller;
    }

    public Contact getCaller() {
        return this.caller;
    }

    public void setCallee(Contact callee) {
        this.callee = callee;
    }

    public Contact getCallee() {
        return this.callee;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (null != this.caller) {
                json.put("caller", this.caller.toBasicJSON());
            }
            if (null != this.callee) {
                json.put("callee", this.callee.toBasicJSON());
            }
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
