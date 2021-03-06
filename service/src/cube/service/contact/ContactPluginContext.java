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

package cube.service.contact;

import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.plugin.PluginContext;
import org.json.JSONObject;

/**
 * 联系人模块插件上下文。
 */
public class ContactPluginContext extends PluginContext {

    private Contact contact;

    private Device device;

    public ContactPluginContext(Contact contact, Device device) {
        super();
        this.contact = contact;
        this.device = device;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    @Override
    public Object get(String name) {
        if (name.equals("contact")) {
            return this.contact;
        }
        else if (name.equals("device")) {
            return this.device;
        }
        else {
            return null;
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contact", this.contact.toJSON());
        json.put("device", this.device.toJSON());
        return json;
    }
}
