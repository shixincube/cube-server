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
import cube.common.JSONable;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;

/**
 * 信令。
 */
public abstract class Signaling implements JSONable {

    /**
     * 信令名。
     */
    protected String name;

    /**
     * 信令的场域。
     */
    protected CommField field;

    /**
     * 传送/接收信令的联系人。
     */
    protected Contact contact;

    /**
     * 传送/接收信令的设备。
     */
    protected Device device;

    /**
     * 客户端 RTC 终端的 SN 。
     */
    protected Long endpointSN;

    /**
     * 构造函数。
     *
     * @param name
     * @param field
     * @param contact
     * @param device
     * @param endpointSN
     */
    public Signaling(String name, CommField field, Contact contact, Device device, Long endpointSN) {
        this.name = name;
        this.field = field;
        this.contact = contact;
        this.device = device;
        this.endpointSN = endpointSN;
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public Signaling(JSONObject json) {
        try {
            this.name = json.getString("name");
            this.field = new CommField(json.getJSONObject("field"));
            this.contact = new Contact(json.getJSONObject("contact"), this.field.getDomain());
            this.device = new Device(json.getJSONObject("device"));
            this.endpointSN = json.getLong("endpointSN");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return this.name;
    }

    public void setField(CommField commField) {
        this.field = commField;
    }

    public CommField getField() {
        return this.field;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    public Long getEndpointSN() {
        return this.endpointSN;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", this.name);
            json.put("field", this.field.toCompactJSON());
            json.put("contact", this.contact.toBasicJSON());
            json.put("device", this.device.toCompactJSON());
            json.put("endpointSN", this.endpointSN.longValue());
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
