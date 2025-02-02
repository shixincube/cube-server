/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import cube.common.entity.Material;
import cube.vision.BoundingBox;
import cube.vision.Box;
import org.json.JSONObject;

import java.util.List;

/**
 * 素材。
 */
public abstract class Thing extends Material {

    protected Label paintingLabel;

    /**
     * 是否是反向推理来的元素。
     */
    protected boolean backwardReasoning = false;

    public Thing(Thing other) {
        super(other);
        this.paintingLabel = other.paintingLabel;
        this.backwardReasoning = other.backwardReasoning;
    }

    public Thing(String label, BoundingBox boundingBox, Box box) {
        super(label, boundingBox, box);
        this.paintingLabel = Label.parse(this.label);
    }

    public Thing(Label paintingLabel) {
        super(paintingLabel.name);
        this.paintingLabel = paintingLabel;
    }

    public Thing(JSONObject json) {
        super(json);
        this.paintingLabel = Label.parse(this.label);
    }

    public Label getLabel() {
        return this.paintingLabel;
    }

    public boolean isBackwardReasoning() {
        return this.backwardReasoning;
    }

    public int getWidth() {
        return this.box.width;
    }

    public int getHeight() {
        return this.box.height;
    }

    public int numComponents() {
        return 0;
    }

    protected <T> T getMaxAreaThing(List<T> list) {
        int max = 0;
        T result = null;
        for (T t : list) {
            if (t instanceof Thing) {
                Thing thing = (Thing) t;
                int area = thing.area;
                if (area > max) {
                    max = area;
                    result = t;
                }
            }
        }
        return result;
    }

    public List<Thing> getSubThings(Label label) {
        return null;
    }
}
