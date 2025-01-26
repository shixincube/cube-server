/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 已检测到的对象。
 */
public class DetectedObject extends Entity {

    private String className;

    private double probability;

    private BoundingBox bbox;

    /**
     * 构造函数。
     *
     * @param className
     * @param probability
     * @param bbox
     */
    public DetectedObject(String className, double probability, BoundingBox bbox) {
        this.className = className;
        this.probability = probability;
        this.bbox = bbox;
    }

    public String getClassName() {
        return this.className;
    }

    public double getProbability() {
        return this.probability;
    }

    public BoundingBox getBoundingBox() {
        return this.bbox;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", this.className);
        json.put("probability", this.probability);
        json.put("bound", this.bbox.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
