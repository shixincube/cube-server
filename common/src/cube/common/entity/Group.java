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

package cube.common.entity;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;

import java.util.List;
import java.util.Vector;

/**
 * 群组实体。
 */
public class Group extends Contact {

    /**
     * 群的所有人。
     */
    private Contact owner;

    /**
     * 群组的成员列表。
     */
    private Vector<Contact> members;

    /**
     * 构造函数。
     *
     * @param id 群组 ID 。
     * @param domain 群组域。
     * @param name 群组显示名。
     * @param owner 群组所有人。
     */
    public Group(Long id, String domain, String name, Contact owner) {
        super(id, domain, name);
        this.owner = owner;
        this.members = new Vector<>();
        this.members.add(owner);
    }

    /**
     * 构造函数。
     *
     * @param json JSON 形式的群组数据。
     */
    public Group(JSONObject json) {
        super(json);
        this.members = new Vector<>();

        try {
            JSONObject ownerJson = json.getJSONObject("owner");
            this.owner = new Contact(ownerJson);
            this.addContact(this.owner);

            if (json.has("members")) {
                JSONArray array = json.getJSONArray("members");
                for (int i = 0, len = array.length(); i < len; ++i) {
                    JSONObject obj = array.getJSONObject(i);
                    Contact contact = new Contact(obj);
                    this.addContact(contact);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Contact getOwner() {
        return this.owner;
    }

    public void addContact(Contact contact) {
        if (this.members.contains(contact)) {
            return;
        }

        this.members.add(contact);
    }

    public boolean removeContact(Contact contact) {
        if (contact.getId().longValue() == this.owner.getId().longValue()) {
            return false;
        }

        return this.members.remove(contact);
    }

    public List<Contact> getMembers() {
        return this.members;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray array = new JSONArray();
            for (Contact contact : this.members) {
                array.put(contact.toJSON());
            }
            json.put("members", array);

            json.put("owner", this.owner.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * 判断是否是群组结构的 JSON 数据格式。
     *
     * @param json 待判断的 JSON 数据。
     * @return 如果 JSON 符合群组数据结构返回 {@code true} 。
     */
    public static boolean isGroup(JSONObject json) {
        if (json.has("members")) {
            return true;
        }
        else {
            return false;
        }
    }
}
