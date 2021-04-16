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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联系人的附录。
 */
public class ContactAppendix extends Entity {

    private Contact owner;

    /**
     * 其他联系人对该联系人的备注名。
     */
    private Map<Long, String> remarkNames;

    /**
     * 不对当前联系人进行提示的联系人。
     */
    private List<Long> noNoticeContacts;

    /**
     * 不对当前联系人进行提示的群组。
     */
    private List<Long> noNoticeGroups;

    /**
     * 已配置的数据。
     */
    private Map<String, JSONObject> assignedData;

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
        this.noNoticeContacts = new Vector<>();
        this.noNoticeGroups = new Vector<>();
        this.assignedData = new ConcurrentHashMap<>();
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
        this.noNoticeContacts = new Vector<>();
        this.noNoticeGroups = new Vector<>();
        this.assignedData = new ConcurrentHashMap<>();

        JSONArray remarkNamesArray = json.getJSONArray("remarkNames");
        for (int i = 0; i < remarkNamesArray.length(); ++i) {
            JSONObject item = remarkNamesArray.getJSONObject(i);
            Long id = item.getLong("id");
            String name = item.getString("name");
            this.remarkNames.put(id, name);
        }

        if (json.has("noNoticeContacts")) {
            JSONArray array = json.getJSONArray("noNoticeContacts");
            for (int i = 0; i < array.length(); ++i) {
                this.noNoticeContacts.add(array.getLong(i));
            }
        }

        if (json.has("noNoticeGroups")) {
            JSONArray array = json.getJSONArray("noNoticeGroups");
            for (int i = 0; i < array.length(); ++i) {
                this.noNoticeGroups.add(array.getLong(i));
            }
        }

        if (json.has("assignedData")) {
            JSONObject assignedDataJson = json.getJSONObject("assignedData");
            Iterator<String> keys = assignedDataJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                this.assignedData.put(key, assignedDataJson.getJSONObject(key));
            }
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

    /**
     *
     * @param contactId
     * @return
     */
    public boolean isNoNoticeContact(Long contactId) {
        return this.noNoticeContacts.contains(contactId);
    }

    /**
     *
     * @param contactId
     */
    public void addNoNoticeContact(Long contactId) {
        if (!this.noNoticeContacts.contains(contactId)) {
            this.noNoticeContacts.add(contactId);
        }
    }

    /**
     *
     * @param contactId
     */
    public void removeNoNoticeContact(Long contactId) {
        this.noNoticeContacts.remove(contactId);
    }

    /**
     *
     * @param groupId
     * @return
     */
    public boolean isNoNoticeGroup(Long groupId) {
        return this.noNoticeGroups.contains(groupId);
    }

    /**
     *
     * @param groupId
     */
    public void addNoNoticeGroup(Long groupId) {
        if (!this.noNoticeGroups.contains(groupId)) {
            this.noNoticeGroups.add(groupId);
        }
    }

    /**
     *
     * @param groupId
     */
    public void removeNoNoticeGroup(Long groupId) {
        this.noNoticeGroups.remove(groupId);
    }

    /**
     * 设置已配置的数据。
     *
     * @param key
     * @param value
     */
    public void setAssignedData(String key, JSONObject value) {
        this.assignedData.put(key, value);
    }

    /**
     * 对指定联系人打包附录数据。
     *
     * @param contact
     * @return
     */
    public JSONObject packJSON(Contact contact) {
        JSONObject json = new JSONObject();
        json.put("owner", this.owner.toCompactJSON());
        String remarkName = this.remarkNames.get(contact.getId());
        if (null == remarkName) {
            remarkName = "";
        }
        json.put("remarkName", remarkName);

        // 打包自己的数据
        if (contact.getId().equals(this.owner.getId())) {
            JSONArray array = new JSONArray();
            for (Long id : this.noNoticeContacts) {
                array.put(id.longValue());
            }
            json.put("noNoticeContacts", array);

            array = new JSONArray();
            for (Long id : this.noNoticeGroups) {
                array.put(id.longValue());
            }
            json.put("noNoticeGroups", array);

            JSONObject dataMap = new JSONObject();
            for (Map.Entry<String, JSONObject> e : this.assignedData.entrySet()) {
                dataMap.put(e.getKey(), e.getValue());
            }
            json.put("assignedData", dataMap);
        }

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

        JSONArray array = new JSONArray();
        for (Long id : this.noNoticeContacts) {
            array.put(id.longValue());
        }
        json.put("noNoticeContacts", array);

        array = new JSONArray();
        for (Long id : this.noNoticeGroups) {
            array.put(id.longValue());
        }
        json.put("noNoticeGroups", array);

        JSONObject dataMap = new JSONObject();
        for (Map.Entry<String, JSONObject> e : this.assignedData.entrySet()) {
            dataMap.put(e.getKey(), e.getValue());
        }
        json.put("assignedData", dataMap);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
