/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONObject;

public class Texture implements JSONable {

    public double max;

    public double avg;

    public double squareDeviation;

    public double standardDeviation;

    public double hierarchy;

    public double density;

    public Texture() {
        this.max = 0;
        this.avg = 0;
        this.squareDeviation = 0;
        this.standardDeviation = 0;
        this.hierarchy = 0;
        this.density = 1;
    }

    public Texture(JSONObject json) {
        this.max = Double.parseDouble(json.getString("max"));
        this.avg = Double.parseDouble(json.getString("avg"));
        this.squareDeviation = Double.parseDouble(json.getString("squareDeviation"));
        this.standardDeviation = Double.parseDouble(json.getString("standardDeviation"));
        this.hierarchy = Double.parseDouble(json.getString("hierarchy"));
        this.density = Double.parseDouble(json.getString("density"));
    }

    public boolean isValid() {
        return this.max > 0 && this.avg > 0 && this.density > 0.0014d;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("max", Double.toString(this.max));
        json.put("avg", Double.toString(this.avg));
        json.put("squareDeviation", Double.toString(this.squareDeviation));
        json.put("standardDeviation", Double.toString(this.standardDeviation));
        json.put("hierarchy", Double.toString(this.hierarchy));
        json.put("density", Double.toString(this.density));
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
