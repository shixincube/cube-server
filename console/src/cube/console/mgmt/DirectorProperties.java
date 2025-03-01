/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 调度机下联的业务单元属性。
 */
public class DirectorProperties implements JSONable {

    public final String address;

    public final int port;

    public final List<String> cellets;

    public final int weight;

    public DirectorProperties(String address, int port, List<String> cellets, int weight) {
        this.address = address;
        this.port = port;
        this.cellets = cellets;
        this.weight = weight;
    }

    public DirectorProperties(JSONObject json) throws JSONException {
        this.address = json.getString("address");
        this.port = json.getInt("port");

        this.cellets = new ArrayList<>();
        JSONArray array = json.getJSONArray("cellets");
        for (int i = 0; i < array.length(); ++i) {
            String cellet = array.getString(i);
            this.cellets.add(cellet);
        }

        this.weight = json.getInt("weight");
    }

    public String getCelletsAsString() {
        StringBuilder buf = new StringBuilder();
        for (String cellet : this.cellets) {
            buf.append(cellet).append(",");
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    public boolean equalsCellets(DirectorProperties other) {
        if (this.cellets.size() != other.cellets.size()) {
            return false;
        }

        for (String cellet : this.cellets) {
            if (other.cellets.indexOf(cellet) < 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof DirectorProperties) {
            DirectorProperties other = (DirectorProperties) object;
            if (other.address.equals(this.address) && other.port == this.port) {
                return true;
            }
        }
        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("address", this.address);
        json.put("port", this.port);
        json.put("weight", this.weight);

        JSONArray array = new JSONArray();
        for (String cellet : this.cellets) {
            array.put(cellet);
        }
        json.put("cellets", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
