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

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;

import java.util.List;

/**
 * Ringing 信令。
 */
public class RingingSignaling extends Signaling {

    private List<Contact> callees;

    public RingingSignaling(CommField field, Contact contact, Device device, List<Contact> callees) {
        super(MultipointCommAction.Ringing.name, field, contact, device);
        this.callees = callees;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray array = new JSONArray();
            for (Contact callee : this.callees) {
                array.put(callee.toBasicJSON());
            }
            json.put("callees", array);
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
