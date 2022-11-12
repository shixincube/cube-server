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

import cell.core.talk.TalkContext;
import cube.common.Domain;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人实体。
 */
public class Contact extends AbstractContact {

    /**
     * 联系人的设备列表。
     */
    private List<Device> deviceList;

    /**
     * 附录。
     */
    private ContactAppendix appendix;

    /**
     * 构造函数。
     */
    public Contact() {
        super();
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domainName 联系人所在的域。
     */
    public Contact(long id, String domainName) {
        super(id, domainName, "");
        this.deviceList = new ArrayList<>(1);
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domainName 所在域的域名。
     * @param name 显示名。
     */
    public Contact(Long id, String domainName, String name) {
        super(id, domainName, name);
        this.deviceList = new ArrayList<>(1);
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domainName 所在域的域名。
     * @param name 显示名。
     * @param timestamp 时间戳。
     */
    public Contact(Long id, String domainName, String name, long timestamp) {
        super(id, domainName, name, timestamp);
        this.deviceList = new ArrayList<>(1);
    }

    /**
     * 构造函数。
     *
     * @param id 联系人 ID 。
     * @param domain 所在域。
     * @param name 显示名。
     */
    public Contact(Long id, Domain domain, String name) {
        super(id, domain, name);
        this.deviceList = new ArrayList<>(1);
    }

    /**
     * 构造函数。
     *
     * @param externalId 外部 ID 。
     */
    public Contact(String externalId) {
        super(externalId);
    }

    /**
     * 构造函数。
     *
     * @param json 符合格式的 JSON 数据。
     */
    public Contact(JSONObject json) {
        this(json, "");
    }

    /**
     * 构造函数。
     *
     * @param json 符合格式的 JSON 数据。
     * @param domain 指定的域。
     */
    public Contact(JSONObject json, Domain domain) {
        this(json, domain.getName());
    }

    /**
     * 构造函数。
     *
     * @param json 符合格式的 JSON 数据。
     * @param domain 指定的域。
     */
    public Contact(JSONObject json, String domain) {
        super(json, domain);

        this.deviceList = new ArrayList<>(1);

        if (json.has("devices")) {
            JSONArray array = json.getJSONArray("devices");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject devJson = array.getJSONObject(i);
                Device device = new Device(devJson);
                this.addDevice(device);
            }
        }
    }

    /**
     * 构造函数。
     *
     * @param json 符合格式的 JSON 数据。
     * @param talkContext 关联的 Talk Context 。
     */
    public Contact(JSONObject json, TalkContext talkContext) {
        super(json, null);

        this.deviceList = new ArrayList<>(1);

        if (json.has("devices")) {
            JSONArray array = json.getJSONArray("devices");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject devJson = array.getJSONObject(i);
                Device device = new Device(devJson);
                this.addDevice(device);
            }
        }

        // 绑定设备到上下文
        if (json.has("device")) {
            JSONObject deviceJson = json.getJSONObject("device");
            Device device = new Device(deviceJson, talkContext);
            this.addDevice(device);
        }
    }

    /**
     * 添加设备。
     *
     * @param device 指定设备。
     * @return 返回之前已经添加的同类型设备。
     */
    public Device addDevice(Device device) {
        Device invalid = null;
        int index = this.deviceList.indexOf(device);
        if (index >= 0) {
            invalid = this.deviceList.get(index);
            this.deviceList.remove(index);
        }

        device.contact = this;
        this.deviceList.add(device);

        return invalid;
    }

    /**
     * 添加设备。
     *
     * @param name 设备名称。
     * @param platform 平台描述。
     * @param talkContext 对应的会话上下文。
     * @return 返回之前已经添加的同类型设备。
     */
    public Device addDevice(String name, String platform, TalkContext talkContext) {
        Device invalid = null;
        Device device = new Device(name, platform, talkContext);

        int index = this.deviceList.indexOf(device);
        if (index >= 0) {
            invalid = this.deviceList.get(index);
            this.deviceList.remove(index);
        }

        device.contact = this;
        this.deviceList.add(device);

        return invalid;
    }

    /**
     * 移除设备。
     *
     * @param device 待移除设备。
     */
    public void removeDevice(Device device) {
        this.deviceList.remove(device);
    }

    /**
     * 返回设备数量。
     *
     * @return 返回设备数量。
     */
    public int numDevices() {
        return this.deviceList.size();
    }

    /**
     * 通过客户端的 TalkContext 匹配获取对应的设备。
     * 该方法主要提供给网关节点使用。
     *
     * @param talkContext 会话上下文。
     * @return 返回在此上下文上通信的设备。
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
     *
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
     *
     * @return 返回设备列表。
     */
    public List<Device> getDeviceList() {
        return new ArrayList<Device>(this.deviceList);
    }

    /**
     * 是否包含指定设备。
     *
     * @param device 指定设备实例。
     * @return 如果包含该设备返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean hasDevice(Device device) {
        return this.deviceList.contains(device);
    }

    /**
     * 设置附录。
     * @see ContactAppendix
     *
     * @param appendix 指定联系人附录。
     */
    public void setAppendix(ContactAppendix appendix) {
        this.appendix = appendix;
    }

    /**
     * 获取附录。
     * @see ContactAppendix
     *
     * @return 返回联系人附录。
     */
    public ContactAppendix getAppendix() {
        return this.appendix;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        if (null != this.deviceList && !this.deviceList.isEmpty()) {
            JSONArray array = new JSONArray();
            for (int i = 0, len = this.deviceList.size(); i < len; ++i) {
                Device device = this.deviceList.get(i);
                array.put(device.toJSON());
            }
            json.put("devices", array);
        }

        return json;
    }

    /**
     * {@inheritDoc}
     */
    public JSONObject toCompactJSON() {
        return super.toCompactJSON();
    }

    /**
     * 在格式数据里增加指定设备。
     *
     * @param device 指定设备。
     * @return 返回包含该设备的数据结构。
     */
    public JSONObject toJSON(Device device) {
        JSONObject json = this.toJSON();
        json.put("device", device.toJSON());
        return json;
    }
}
