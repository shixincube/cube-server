/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2026 Ambrose Xu.
 */

package cube.ferry;

import cube.common.JSONable;
import org.json.JSONObject;

public class GnosisAgent implements JSONable {

    public final static String VitalSigns = "VitalSigns";

    public final static String Persons = "Persons";

    public final String name;

    public final boolean mock;

    public JSONObject result;

    public GnosisAgent(String name, boolean mock) {
        this.name = name;
        this.mock = mock;
    }

    public GnosisAgent(JSONObject json) {
        this.name = json.getString("name");
        this.mock = json.getBoolean("mock");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("mock", this.mock);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
