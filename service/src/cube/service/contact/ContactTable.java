/**
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

package cube.service.contact;

import cube.common.Domain;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 联系人表。
 */
public class ContactTable {

    private Domain domain;

    protected ConcurrentHashMap<Long, Contact> onlineContacts;

    public ContactTable(Domain domain) {
        this.domain = domain;
        this.onlineContacts = new ConcurrentHashMap<>();
    }

    public Domain getDomain() {
        return this.domain;
    }

    /**
     * 返回指定 ID 的联系人。
     *
     * @param id
     * @return
     */
    public Contact get(Long id) {
        return this.onlineContacts.get(id);
    }

    /**
     * 更新联系人。
     *
     * @param contact
     * @param device
     * @return
     */
    public Contact add(Contact contact, Device device) {
        Contact current = this.onlineContacts.get(contact.getId());
        if (null == current) {
            current = contact;
            this.onlineContacts.put(contact.getId(), contact);
        }
        else {
            current.setName(contact.getName());

            JSONObject context = contact.getContext();
            if (null != context) {
                current.setContext(context);
            }

            current.resetTimestamp();

            current.addDevice(device);
        }

        return current;
    }

    /**
     * 移除联系人的设备。
     *
     * @param contact
     * @param device
     */
    public void remove(Contact contact, Device device) {
        Contact current = this.onlineContacts.get(contact.getId());
        if (null != current) {
            current.removeDevice(device);
            current.resetTimestamp();
        }
    }

    /**
     * 移除联系人。
     *
     * @param contact
     */
    public void remove(Contact contact) {
        this.onlineContacts.remove(contact.getId());
    }
}
