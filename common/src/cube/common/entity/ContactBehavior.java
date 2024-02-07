/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 联系人行为描述。
 */
public class ContactBehavior extends Entity {

    /**
     * 联系人签入系统。
     */
    public final static String SIGN_IN = "SignIn";

    /**
     * 联系人签出系统。
     */
    public final static String SIGN_OUT = "SignOut";

    /**
     * 云存储里创建新文件。
     */
    public final static String NEW_FILE = "NewFile";

    /**
     * 云存储里删除文件。
     */
    public final static String DELETE_FILE = "DeleteFile";

    /**
     * 联系人从服务器下载文件。
     */
    public final static String DOWNLOAD_FILE = "DownloadFile";

    private Contact contact;

    private String behavior;

    private Device device;

    private JSONObject parameter;

    /**
     * 构造函数。
     *
     * @param contact 联系人。
     * @param behavior 行为描述。
     */
    public ContactBehavior(Contact contact, String behavior) {
        super(Utils.generateSerialNumber(), contact.getDomain());
        this.contact = contact;
        this.behavior = behavior;
    }

    /**
     * 构造函数。
     *
     * @param json 结构 JSON 。
     */
    public ContactBehavior(JSONObject json) {
        super(json);
        this.contact = new Contact(json.getJSONObject("contact"));
        this.behavior = json.getString("behavior");
        if (json.has("device")) {
            this.device = new Device(json.getJSONObject("device"));
        }
        if (json.has("parameter")) {
            this.parameter = json.getJSONObject("parameter");
        }
    }

    /**
     * 获取联系人。
     *
     * @return 返回联系人。
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取行为描述。
     *
     * @return 返回行为描述。
     */
    public String getBehavior() {
        return this.behavior;
    }

    /**
     * 获取行为发生时的设备。
     *
     * @return 返回行为发生时的设备。
     */
    public Device getDevice() {
        return this.device;
    }

    /**
     * 设置设备。
     *
     * @param device 指定设备。
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * 获取行为描述的参数。
     *
     * @return 返回行为描述的参数。
     */
    public JSONObject getParameter() {
        return this.parameter;
    }

    /**
     * 设置行为描述的参数。
     *
     * @param parameter 指定 JSON 格式的参数。
     */
    public void setParameter(JSONObject parameter) {
        this.parameter = parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contact", this.contact.toJSON());
        json.put("behavior", this.behavior);
        if (null != this.device) {
            json.put("device", this.device.toJSON());
        }
        if (null != this.parameter) {
            json.put("parameter", this.parameter);
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("contact", this.contact.toCompactJSON());
        json.put("behavior", this.behavior);
        if (null != this.device) {
            json.put("device", this.device.toCompactJSON());
        }
        if (null != this.parameter) {
            json.put("parameter", this.parameter);
        }
        return json;
    }
}
