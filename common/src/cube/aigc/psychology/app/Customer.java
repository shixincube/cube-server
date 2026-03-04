/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.common.JSONable;
import cube.util.ConfigUtils;
import cube.util.Gender;
import org.json.JSONObject;

public class Customer implements JSONable {

    public long id;

    public String name;

    public Gender gender;

    public int age;

    public String mobile;

    public String comment;

    public Customer() {
        this.id = ConfigUtils.generateSerialNumber();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
