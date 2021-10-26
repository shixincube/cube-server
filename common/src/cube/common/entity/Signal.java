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

package cube.common.entity;

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 信号。
 */
public class Signal extends Entity {

    private Contact destContact;

    private Device destDevice;

    private Contact srcContact;

    private Device srcDevice;

    private JSONObject payload;

    public Signal() {
        super(Utils.generateSerialNumber());
    }

    public Signal(JSONObject json) {
        super(json);

        if (json.has("destContact")) {
            this.destContact = new Contact(json.getJSONObject("destContact"));
        }
        if (json.has("destDevice")) {
            this.destDevice = new Device(json.getJSONObject("destDevice"));
        }
        if (json.has("srcContact")) {
            this.srcContact = new Contact(json.getJSONObject("srcContact"));
        }
        if (json.has("srcDevice")) {
            this.srcDevice = new Device(json.getJSONObject("srcDevice"));
        }

        if (json.has("payload")) {
            this.payload = json.getJSONObject("payload");
        }
    }

    public Contact getDestContact() {
        return this.destContact;
    }

    public void setDestContact(Contact contact) {
        this.destContact = contact;
    }

    public Device getDestDevice() {
        return this.destDevice;
    }

    public Contact getSrcContact() {
        return this.srcContact;
    }

    public Device getSrcDevice() {
        return this.srcDevice;
    }

    public JSONObject getPayload() {
        return this.payload;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());

        if (null != this.destContact) {
            json.put("destContact", this.destContact.toBasicJSON());
        }

        if (null != this.destDevice) {
            json.put("destDevice", this.destDevice.toCompactJSON());
        }

        if (null != this.srcContact) {
            json.put("srcContact", this.srcContact.toBasicJSON());
        }

        if (null != this.srcDevice) {
            json.put("srcDevice", this.srcDevice.toCompactJSON());
        }

        if (null != this.payload) {
            json.put("payload", this.payload);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("payload");
        return json;
    }
}
