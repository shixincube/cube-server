/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material.house;

import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Thing;
import org.json.JSONObject;

/**
 * 房前小路。
 */
public class Path extends Thing {

    private boolean curve = false;

    private boolean cobbled = false;

    public Path(JSONObject json) {
        super(json);

        if (Label.HouseCurvePath == this.paintingLabel) {
            this.curve = true;
        }
        else if (Label.HouseCobbledPath == this.paintingLabel) {
            this.cobbled = true;
        }
    }

    public boolean isCurve() {
        return this.curve;
    }

    public boolean isCobbled() {
        return this.cobbled;
    }
}
