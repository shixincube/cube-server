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

import cube.common.UniqueKey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 联系人的附录。
 */
public class ContactAppendix extends Entity {

    private Contact owner;

    private HashMap<Long, String> remarkNames;

    /**
     * 构造函数。
     *
     * @param owner
     */
    public ContactAppendix(Contact owner) {
        super();
        this.uniqueKey = owner.getUniqueKey() + "_appendix";
        this.owner = owner;
        this.remarkNames = new HashMap<>();
    }

    /**
     * 构造函数。
     *
     * @param owner
     * @param json
     */
    public ContactAppendix(Contact owner, JSONObject json) {
        super();
        this.uniqueKey = owner.getUniqueKey() + "_appendix";
        this.owner = owner;
        this.remarkNames = new HashMap<>();

        JSONArray remarkNamesArray = json.getJSONArray("remarkNames");
        for (int i = 0; i < remarkNamesArray.length(); ++i) {
            JSONObject item = remarkNamesArray.getJSONObject(i);
            Long id = item.getLong("id");
            String name = item.getString("name");
            this.remarkNames.put(id, name);
        }
    }

    /**
     * 返回附录所属的联系人。
     *
     * @return 返回附录所属的联系人。
     */
    public Contact getOwner() {
        return this.owner;
    }

    /**
     * 指定联系人备注在该联系人的名称。
     * 即 marker 把 this 备注为 name 。
     *
     * @param marker
     * @param name
     */
    public void remarkName(Contact marker, String name) {
        this.remarkNames.put(marker.getId(), name);
        this.resetTimestamp();
    }

    /**
     * 获取联系人在该联系上备注的名称。
     *
     * @param contact
     * @return
     */
    public String getRemarkName(Contact contact) {
        this.resetTimestamp();
        return this.remarkNames.get(contact.getId());
    }

    public JSONObject packJSON(Contact contact) {
        JSONObject json = new JSONObject();
        json.put("owner", this.owner.toCompactJSON());
        String remarkName = this.remarkNames.get(contact.getId());
        if (null == remarkName) {
            remarkName = "";
        }
        json.put("remarkName", remarkName);
        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("ownerId", this.owner.getId());

        JSONArray remarkNamesArray = new JSONArray();
        Iterator<Map.Entry<Long, String>> iter = this.remarkNames.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, String> e = iter.next();
            JSONObject item = new JSONObject();
            item.put("id", e.getKey().longValue());
            item.put("name", e.getValue());
            remarkNamesArray.put(item);
        }
        json.put("remarkNames", remarkNamesArray);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
