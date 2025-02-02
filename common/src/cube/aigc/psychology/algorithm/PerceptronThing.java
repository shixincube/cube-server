/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.composition.Texture;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Thing;
import cube.util.FloatUtils;
import cube.vision.BoundingBox;
import cube.vision.Box;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PerceptronThing extends Thing {

    private String name;

    private List<PerceptronThing> children;

    private double value = 1;

    public PerceptronThing(JSONObject json) {
        super(json);
        if (json.has("name")) {
            this.name = json.getString("name");
        }
        else {
            this.name = this.paintingLabel.name();
        }

        this.children = new ArrayList<>();
        if (json.has("children")) {
            JSONArray array = json.getJSONArray("children");
            for (int i = 0; i < array.length(); ++i) {
                this.children.add(new PerceptronThing(array.getJSONObject(i)));
            }
        }

        if (json.has("value")) {
            this.value = json.getDouble("value");
        }
    }

    public PerceptronThing(Thing thing) {
        super(thing);
        this.name = this.paintingLabel.name();
        this.children = new ArrayList<>();

        if (thing instanceof PerceptronThing) {
            PerceptronThing pt = (PerceptronThing) thing;
            this.name = pt.name;
            this.children.addAll(pt.children);
            this.value = pt.value;
        }
    }

    public PerceptronThing(String name) {
        super(Label.Unknown);
        this.boundingBox = new BoundingBox(0, 0, 0, 0);
        this.box = new Box(0, 0, 0, 0);
        this.area = 0;
        this.color = "#000000";
        this.texture = new Texture();
        this.name = name;
        this.children = new ArrayList<>();
    }

    public PerceptronThing(String name, Thing[] children) {
        this(name);
        for (Thing thing : children) {
            this.children.add(new PerceptronThing(thing));
        }
    }

    public String getName() {
        return this.name;
    }

    public boolean hasChildren() {
        return (!this.children.isEmpty());
    }

    public List<PerceptronThing> getChildren() {
        return this.children;
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("name", this.name);
        JSONArray array = new JSONArray();
        for (PerceptronThing thing : this.children) {
            array.put(thing.toJSON());
        }
        json.put("children", array);
        json.put("value", this.value);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("name", this.name);
        JSONArray array = new JSONArray();
        for (PerceptronThing thing : this.children) {
            array.put(thing.toCompactJSON());
        }
        json.put("children", array);
        json.put("value", this.value);
        return json;
    }

    public static PerceptronThing createPictureSize() {
        PerceptronThing result = new PerceptronThing("PictureSize");
        result.value = 1.0 + FloatUtils.random(-0.1, 0.1);
        return result;
    }

    public static PerceptronThing createPictureLayout() {
        PerceptronThing result = new PerceptronThing("PictureLayout");
        result.value = 1.5 + FloatUtils.random(-0.1, 0.1);
        return result;
    }

    public static PerceptronThing createPictureLayout(Thing[] things) {
        PerceptronThing result = new PerceptronThing("PictureLayout", things);
        result.value = 1.8 + FloatUtils.random(-0.1, 0.1);
        return result;
    }

    public static PerceptronThing createPictureSense() {
        PerceptronThing result = new PerceptronThing("PictureSense");
        result.value = 2.0 + FloatUtils.random(-0.1, 0.1);
        return result;
    }

    public static PerceptronThing createThingPosition(Thing[] things) {
        PerceptronThing result = new PerceptronThing("ThingPosition", things);
        result.value = 2.3 + FloatUtils.random(-0.1, 0.1);
        return result;
    }

    public static PerceptronThing createThingSize(Thing[] things) {
        PerceptronThing result = new PerceptronThing("ThingSize", things);
        result.value = 2.5 + FloatUtils.random(-0.1, 0.1);
        return result;
    }
}
