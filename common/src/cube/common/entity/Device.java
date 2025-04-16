/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.core.talk.TalkContext;
import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 设备实体描述。
 */
public class Device implements JSONable {

    /**
     * 对应的联系人。
     */
    protected Contact contact;

    /**
     * 设备名称。
     */
    private String name;

    /**
     * 设备平台描述。
     */
    private String platform;

    /**
     * 设备当前接入的地址。
     */
    private String address;

    /**
     * 设备当前接入的端口。
     */
    private int port = 0;

    /**
     * 设备当前的会话上下文。
     */
    private TalkContext talkContext;

    /**
     * 客户端提供的令牌码。
     */
    private String token;

    /**
     * 设备信息的 Hash 值。
     */
    private int hash = 0;

    /**
     * 构造函数。
     *
     * @param name 指定设备名。
     * @param platform 指定平台描述。
     */
    public Device(String name, String platform) {
        this.name = name;
        this.platform = platform;
    }

    /**
     * 构造函数。
     *
     * @param name 指定设备名。
     * @param platform 指定平台描述。
     * @param talkContext 指定上下文。
     */
    public Device(String name, String platform, TalkContext talkContext) {
        this.name = name;
        this.platform = platform;
        this.address = talkContext.getSessionHost();
        this.port = talkContext.getSessionPort();
        this.talkContext = talkContext;
    }

    /**
     * 构造函数。
     *
     * @param deviceJson 指定符合格式的 JSON 数据。
     */
    public Device(JSONObject deviceJson) {
        this.name = deviceJson.getString("name");
        this.platform = deviceJson.getString("platform");

        if (deviceJson.has("address")) {
            this.address = deviceJson.getString("address");
        }

        if (deviceJson.has("port")) {
            this.port = deviceJson.getInt("port");
        }
    }

    /**
     * 构造函数。
     *
     * @param deviceJson 指定符合格式的 JSON 数据。
     * @param talkContext 指定会话上下文。
     */
    public Device(JSONObject deviceJson, TalkContext talkContext) {
        try {
            this.name = deviceJson.getString("name");
            this.platform = deviceJson.getString("platform");

            if (deviceJson.has("address")) {
                this.address = deviceJson.getString("address");
            }
            else {
                this.address = talkContext.getSessionHost();
            }

            if (deviceJson.has("port")) {
                this.port = deviceJson.getInt("port");
            }
            else {
                this.port = talkContext.getSessionPort();
            }

            this.talkContext = talkContext;
        } catch (JSONException e) {
            e.printStackTrace();
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
     * 获取设备名称。
     *
     * @return 返回设备名称。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取平台描述。
     *
     * @return 返回平台描述。
     */
    public String getPlatform() {
        return this.platform;
    }

    /**
     * 获取设备地址。
     *
     * @return 返回字符串形式的设备地址。
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * 获取设备的通信端口。
     *
     * @return 返回设备的通信端口。
     */
    public int getPort() {
        return this.port;
    }

    /**
     * 设置设备对应的令牌编码。
     *
     * @param token 令牌编码。
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取令牌编码。
     *
     * @return 返回令牌编码。
     */
    public String getToken() {
        return this.token;
    }

    /**
     * 返回会话上下文。
     *
     * @return 返回会话上下文。
     */
    public TalkContext getTalkContext() {
        return this.talkContext;
    }

    /**
     * 判断设备是否在线。
     *
     * @return 如果设备在线返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean isOnline() {
        return (null != this.talkContext) && this.talkContext.isValid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("platform", this.platform);
        if (null != this.address) {
            json.put("address", this.address);
        }
        if (0 != this.port) {
            json.put("port", this.port);
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("platform", this.platform);
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Device) {
            Device other = (Device) obj;
            if (this.name.equals(other.name) && this.platform.equals(other.platform)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (0 == this.hash) {
            this.hash = this.name.hashCode() * 3 + this.platform.hashCode() * 7;
        }

        return this.hash;
    }

    @Override
    public String toString() {
        return this.name + "/" + this.platform;
    }

    /**
     * 创建无设备描述的设备。
     *
     * @return
     */
    public static Device createDevice() {
        return new Device("Unknown", "Unknown");
    }
}
