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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间片段。
 */
public class TimeSlice implements JSONable {

    public final int slice;

    public final long beginning;

    public final long ending;

    private List<Contact> contactList;

    public TimeSlice(int slice, long beginning, long ending) {
        this.slice = slice;
        this.beginning = beginning;
        this.ending = ending;
        this.contactList = new ArrayList<>();
    }

    public void addContact(Contact contact, Device device) {
        int index = this.contactList.indexOf(contact);
        if (index >= 0) {
            this.contactList.get(index).addDevice(device);
        }
        else {
            contact.addDevice(device);
            this.contactList.add(contact);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("slice", this.slice);
        json.put("beginning", this.beginning);
        json.put("ending", this.ending);

        JSONArray array = new JSONArray();
        for (Contact contact : this.contactList) {
            array.put(contact.toJSON());
        }
        json.put("list", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}