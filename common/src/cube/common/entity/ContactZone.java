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
 * 联系人分区。
 */
public class ContactZone implements JSONable {

    /**
     * 域。
     */
    public final String domain;

    /**
     * 所属的联系人的 ID 。
     */
    public final long owner;

    /**
     * 分区名称。
     */
    public final String name;

    /**
     * 分区的联系人列表。
     */
    public final List<Long> contacts;

    public ContactZone(String domain, long owner, String name) {
        this.domain = domain;
        this.owner = owner;
        this.name = name;
        this.contacts = new ArrayList<>();
    }


    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("owner", this.owner);
        json.put("name", this.name);

        JSONArray array = new JSONArray();
        for (Long id : this.contacts) {
            array.put(id.longValue());
        }
        json.put("contacts", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
