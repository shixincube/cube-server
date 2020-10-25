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

import cell.core.talk.TalkContext;
import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.UniqueKey;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人实体。
 */
public class Contact extends Entity {

    /**
     * 联系人 ID 。
     */
    private Long id;

    /**
     * 联系人显示名。
     */
    private String name;

    /**
     * 联系携带的上下文 JSON 数据。
     */
    private JSONObject context;

    /**
     * 联系人的设备列表。
     */
    private List<Device> deviceList;

    /**
     * 联系人的唯一索引键。
     */
    private String uniqueKey;

    public Contact(Long id, String domainName, String name) {
        super(domainName);
        this.id = id;
        this.uniqueKey = UniqueKey.make(id, domainName);
        this.name = name;
        this.deviceList = new ArrayList<>(1);
    }

    public Contact(Long id, Domain domain, String name) {
        super(domain);
        this.id = id;
        this.uniqueKey = UniqueKey.make(id, domain);
        this.name = name;
        this.deviceList = new ArrayList<>(1);
    }

    public Contact(JSONObject json) {
        this(json, "");
    }

    public Contact(JSONObject json, String domain) {
        super();

        this.deviceList = new ArrayList<>(1);

        try {
            this.id = json.getLong("id");
            this.domain = (null != domain && domain.length() > 1) ? new Domain(domain) : new Domain(json.getString("domain"));
            this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
            this.name = json.getString("name");

            if (json.has("devices")) {
                JSONArray array = json.getJSONArray("devices");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject devJson = array.getJSONObject(i);
                    Device device = new Device(devJson);
                    this.addDevice(device);
                }
            }

            if (json.has("context")) {
                this.context = json.getJSONObject("context");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Contact(JSONObject json, TalkContext talkContext) {
        super();

        this.deviceList = new ArrayList<>(1);

        try {
            this.id = json.getLong("id");
            this.domain = new Domain(json.getString("domain"));
            this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
            this.name = json.getString("name");

            if (json.has("devices")) {
                JSONArray array = json.getJSONArray("devices");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject devJson = array.getJSONObject(i);
                    Device device = new Device(devJson);
                    this.addDevice(device);
                }
            }

            if (json.has("context")) {
                this.context = json.getJSONObject("context");
            }

            // 绑定设备到上下文
            if (json.has("device")) {
                JSONObject deviceJson = json.getJSONObject("device");
                Device device = new Device(deviceJson, talkContext);
                this.addDevice(device);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取联系人的 ID 。
     *
     * @return 返回联系人的 ID 。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 获取唯一索引键。
     *
     * @return 返回唯一索引键。
     */
    public String getUniqueKey() {
        return this.uniqueKey;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JSONObject getContext() {
        return this.context;
    }

    public void setContext(JSONObject context) {
        this.context = context;
    }

    public Device addDevice(Device device) {
        Device invalid = null;
        int index = this.deviceList.indexOf(device);
        if (index >= 0) {
            invalid = this.deviceList.get(index);
            this.deviceList.remove(index);
        }

        this.deviceList.add(device);

        return invalid;
    }

    public Device addDevice(String name, String platform, TalkContext talkContext) {
        Device invalid = null;
        Device device = new Device(name, platform, talkContext);

        int index = this.deviceList.indexOf(device);
        if (index >= 0) {
            invalid = this.deviceList.get(index);
            this.deviceList.remove(index);
        }

        this.deviceList.add(device);

        return invalid;
    }

    public void removeDevice(Device device) {
        this.deviceList.remove(device);
    }

    /**
     * 返回设备数量。
     *
     * @return
     */
    public int numDevices() {
        return this.deviceList.size();
    }

    /**
     * 通过客户端的 TalkContext 匹配获取对应的设备。
     * 该方法主要提供给网关节点使用。
     * @param talkContext
     * @return
     */
    public Device getDevice(TalkContext talkContext) {
        Device device = null;
        for (int i = 0, size = this.deviceList.size(); i < size; ++i) {
            device = this.deviceList.get(i);
            if (device.getTalkContext() == talkContext) {
                return device;
            }
        }
        return null;
    }

    /**
     * 获取与指定源设备相同描述的设备。
     * @param src 指定对比的源设备。
     * @return 返回该联系保存的设备实例。
     */
    public Device getDevice(Device src) {
        for (Device device : this.deviceList) {
            if (device.equals(src)) {
                return device;
            }
        }

        return null;
    }

    /**
     * 获取设备列表。
     * @return 返回设备列表。
     */
    public List<Device> getDeviceList() {
        return new ArrayList<Device>(this.deviceList);
    }

    /**
     * 是否包含指定设备。
     * @param device
     * @return
     */
    public boolean hasDevice(Device device) {
        return this.deviceList.contains(device);
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Contact) {
            Contact other = (Contact) object;
            if (other.id.longValue() == this.id.longValue() && other.domain.equals(this.domain)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode() * 3 + this.domain.hashCode() * 5;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("domain", this.domain.getName());
            json.put("name", this.name);

            if (!this.deviceList.isEmpty()) {
                JSONArray array = new JSONArray();
                for (int i = 0, len = this.deviceList.size(); i < len; ++i) {
                    Device device = this.deviceList.get(i);
                    array.put(device.toJSON());
                }
                json.put("devices", array);
            }

            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("name", this.name);
            if (null != this.context) {
                json.put("context", this.context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
