/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 来自客户端数据通道的包结构。
 */
public class Packet implements JSONable {

    /**
     * 封包的唯一序号。
     */
    public final Long sn;

    /**
     * 封包名称。
     */
    public final String name;

    /**
     * 封包的负载数据。
     */
    public final JSONObject data;

    /**
     * 构造函数。
     *
     * @param dialect 指定动作方言。
     */
    public Packet(ActionDialect dialect) {
        this.name = dialect.getName();
        this.sn = dialect.getParamAsLong("sn");
        this.data = dialect.getParamAsJson("data");
    }

    /**
     * 构造函数。
     *
     * @param name 指定封包名。
     * @param data 指定封包负载。
     */
    public Packet(String name, JSONObject data) {
        this(Utils.generateSerialNumber(), name, data);
    }

    /**
     * 构造函数。
     *
     * @param sn 指定序列号。
     * @param name 指定封包名。
     * @param data 指定封包负载。
     */
    public Packet(Long sn, String name, JSONObject data) {
        this.sn = sn;
        this.name = name;
        this.data = data;
    }

    /**
     * 序列化为动作方言。
     *
     * @return 返回动作方言。
     */
    public ActionDialect toDialect() {
        ActionDialect result = new ActionDialect(this.name);
        result.addParam("sn", this.sn.longValue());
        result.addParam("data", this.data);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("sn", this.sn);
            json.put("name", this.name);
            json.put("data", this.data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 提取状态码。
     *
     * @param packet
     * @return
     */
    public static int extractCode(Packet packet) {
        return packet.data.getInt("code");
    }

    /**
     * 提取数据负载内容。
     *
     * @param packet
     * @return
     */
    public static JSONObject extractDataPayload(Packet packet) {
        return packet.data.getJSONObject("data");
    }
}
