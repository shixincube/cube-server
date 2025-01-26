/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.ClientDescription;
import cube.hub.event.Event;
import org.json.JSONObject;

/**
 * 信号。
 */
public abstract class Signal implements JSONable {

    private long sn;

    private String code;

    private final String name;

    private ClientDescription description;

    public Event event;

    public Signal(String name) {
        this.sn = Utils.generateSerialNumber();
        this.name = name;
    }

    public Signal(String name, ClientDescription description) {
        this.sn = Utils.generateSerialNumber();
        this.name = name;
        this.description = description;
    }

    public Signal(JSONObject json) {
        this.sn = json.getLong("sn");
        this.name = json.getString("name");

        if (json.has("code")) {
            this.code = json.getString("code");
        }

        if (json.has("description")) {
            this.description = new ClientDescription(json.getJSONObject("description"));
        }
    }

    public long getSerialNumber() {
        return this.sn;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(ClientDescription description) {
        this.description = description;
    }

    public ClientDescription getDescription() {
        return this.description;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("name", this.name);

        if (null != this.code) {
            json.put("code", this.code);
        }

        if (null != this.description) {
            json.put("description", this.description.toCompactJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
