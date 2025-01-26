/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONObject;

public class MandalaFlower implements JSONable {

    public String name;

    public String url;

    public MandalaFlower(String name) {
        this.name = name;
        this.url = "http://static.shixincube.com/data/mandala/" + name + ".jpg";
    }

    public MandalaFlower(JSONObject json) {
        this.name = json.getString("name");
        this.url = json.getString("url");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("url", this.url);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
