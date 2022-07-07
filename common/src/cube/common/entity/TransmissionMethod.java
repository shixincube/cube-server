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

import org.json.JSONObject;

/**
 * 传输方式。
 */
public class TransmissionMethod extends Entity {

    /**
     * 消息实体。
     */
    private Message message;

    /**
     * 传输目标。
     */
    private AbstractContact target;

    /**
     * 操作时使用的设备。
     */
    private Device device;

    public TransmissionMethod(Message message, AbstractContact target, Device device) {
        super();
        this.message = message;
        this.target = target;
        this.device = device;
    }

    public TransmissionMethod(JSONObject json) {
        super();

        if (json.has("message")) {
            this.message = new Message(json.getJSONObject("message"));
        }

        if (json.has("target")) {
            JSONObject data = json.getJSONObject("target");
            if (Group.isGroup(data)) {
                this.target = new Group(data);
            }
            else if (AnonymousContact.isAnonymous(data)) {
                this.target = new AnonymousContact(data);
            }
            else {
                this.target = new Contact(data);
            }
        }

        if (json.has("device")) {
            this.device = new Device(json.getJSONObject("device"));
        }
    }

    public Message getMessage() {
        return this.message;
    }

    public AbstractContact getTarget() {
        return this.target;
    }

    public Device getDevice() {
        return this.device;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.message) {
            json.put("message", this.message.toJSON());
        }
        if (null != this.target) {
            json.put("target", this.target.toJSON());
        }
        if (null != this.device) {
            json.put("device", this.device.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
