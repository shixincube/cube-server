/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Attribute;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

import java.util.List;

public class Comprehensive implements JSONable {

    private String name;

    private Attribute attribute;

    private List<FileLabel> fileLabels;

    private List<Scale> scales;

    public Comprehensive() {

    }

    @Override
    public JSONObject toJSON() {
        return null;
    }

    @Override
    public JSONObject toCompactJSON() {
        return null;
    }
}
