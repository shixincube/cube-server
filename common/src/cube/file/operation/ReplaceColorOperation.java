/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import cube.vision.Color;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 替换图像颜色。
 */
public class ReplaceColorOperation extends ImageOperation {

    public final static String Operation = "ReplaceColor";

    private Color targetColor;

    private Color replaceColor;

    private int fuzzFactor = 20;

    private List<ReplaceColorOperation> operations;

    public ReplaceColorOperation(Color targetColor, Color replaceColor) {
        this.targetColor = targetColor;
        this.replaceColor = replaceColor;
    }

    public ReplaceColorOperation(Color targetColor, Color replaceColor, int fuzzFactor) {
        this.targetColor = targetColor;
        this.replaceColor = replaceColor;
        this.fuzzFactor = fuzzFactor;
    }

    public ReplaceColorOperation(List<ReplaceColorOperation> operations) {
        this.operations = operations;
    }

    public ReplaceColorOperation(JSONObject json) {
        super(json);

        if (json.has("operations")) {
            JSONArray array = json.getJSONArray("operations");
            this.operations = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); ++i) {
                ReplaceColorOperation rco = new ReplaceColorOperation(array.getJSONObject(i));
                this.operations.add(rco);
            }
        }
        else {
            this.targetColor = new Color(json.getJSONObject("target"));
            this.replaceColor = new Color(json.getJSONObject("replace"));
            this.fuzzFactor = json.getInt("fuzz");
        }
    }

    public Color getTargetColor() {
        return this.targetColor;
    }

    public Color getReplaceColor() {
        return this.replaceColor;
    }

    public void setFuzzFactor(int fuzzFactor) {
        this.fuzzFactor = fuzzFactor;
    }

    public int getFuzzFactor() {
        return this.fuzzFactor;
    }

    public List<ReplaceColorOperation> getOperationList() {
        return this.operations;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        if (null != this.operations) {
            JSONArray array = new JSONArray();
            for (ReplaceColorOperation rco : this.operations) {
                array.put(rco.toJSON());
            }
            json.put("operations", array);
        }
        else {
            json.put("target", this.targetColor.toJSON());
            json.put("replace", this.replaceColor.toJSON());
            json.put("fuzz", this.fuzzFactor);
        }

        return json;
    }
}
