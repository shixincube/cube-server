/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cell.core.net.Endpoint;
import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 服务器的接入点。
 */
public class AccessPoint extends Endpoint implements JSONable {

    public final int maxConnection;

    public AccessPoint(String host, int port, int maxConnection) {
        super(host, port);
        this.maxConnection = maxConnection;
    }

    public AccessPoint(JSONObject data) throws JSONException {
        super(data);
        this.maxConnection = data.has("maxConnection") ? data.getInt("maxConnection") : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof AccessPoint) {
            AccessPoint other = (AccessPoint) object;
            if (other.getHost().equals(this.getHost()) && other.getPort() == this.getPort() &&
                other.maxConnection == this.maxConnection) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("maxConnection", this.maxConnection);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
