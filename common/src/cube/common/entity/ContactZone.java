/*
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联系人分区。
 */
public class ContactZone extends Entity {

    /**
     * 所属的联系人的 ID 。
     */
    public final long owner;

    /**
     * 分区名称。
     */
    public final String name;

    /**
     * 是否需要验证。
     */
    public boolean needsVerify = false;

    /**
     * 状态。
     */
    private ContactZoneState state;

    /**
     * 分区的联系人列表。
     */
    private final List<ContactZoneMember> members;

    public ContactZone(Long id, String domain, long owner, String name, ContactZoneState state) {
        super(id, domain);
        this.owner = owner;
        this.name = name;
        this.state = state;
        this.members = new ArrayList<>();
    }

    public void addContact(ContactZoneMember member) {
        for (ContactZoneMember current : this.members) {
            if (current.contactId.equals(member.contactId)) {
                return;
            }
        }

        this.members.add(member);
    }

    public void addContact(Long id) {
        this.addContact(id, null);
    }

    public void addContact(Long id, String postscript) {
        for (ContactZoneMember member : this.members) {
            if (member.contactId.equals(id)) {
                return;
            }
        }

        ContactZoneMember member = new ContactZoneMember(id, postscript);
        this.members.add(member);
    }

    public void removeContact(Long id) {
        for (ContactZoneMember member : this.members) {
            if (member.contactId.equals(id)) {
                this.members.remove(member);
                break;
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("owner", this.owner);
        json.put("name", this.name);
        json.put("state", this.state.code);
        json.put("needsVerify", this.needsVerify);

        JSONArray array = new JSONArray();
        for (ContactZoneMember member : this.members) {
            array.put(member.toJSON());
        }
        json.put("contacts", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
