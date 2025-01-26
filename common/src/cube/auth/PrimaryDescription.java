/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.auth;

import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 主访问主机描述。
 */
public class PrimaryDescription implements JSONable {

    /**
     * 主机主地址。
     */
    private String address;

    /**
     * 主机主端口。
     */
    private int port = 7000;

    /**
     * 文件存储描述。
     */
    private JSONObject primaryContent;

    /**
     * 构造函数。
     *
     * @param address 主地址。
     * @param primaryContent 首要信息内容。
     */
    public PrimaryDescription(String address, JSONObject primaryContent) {
        this.address = address;
        this.primaryContent = primaryContent;
    }

    /**
     * 构造函数。
     *
     * @param json 首要信息的 JSON 对象。
     */
    public PrimaryDescription(JSONObject json) {
        try {
            this.address = json.getString("address");
            this.port = json.has("port") ? json.getInt("port") : 7000;
            this.primaryContent = json.getJSONObject("primaryContent");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取地址。
     *
     * @return 返回地址信息。
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * 获取首要信息内容。
     *
     * @return 返回 JSON 格式的信息内容。
     */
    public JSONObject getPrimaryContent() {
        return this.primaryContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("address", this.address);
            json.put("port", this.port);
            json.put("primaryContent", this.primaryContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
