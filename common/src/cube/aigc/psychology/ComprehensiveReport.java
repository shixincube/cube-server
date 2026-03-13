/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.composition.Comprehensive;
import cube.common.JSONable;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

import java.util.List;

public class ComprehensiveReport implements JSONable {

    private final Theme theme;

    private final List<Comprehensive> comprehensives;

    private AIGCStateCode state = AIGCStateCode.Processing;

    public ComprehensiveReport(Theme theme, List<Comprehensive> comprehensives) {
        this.theme = theme;
        this.comprehensives = comprehensives;
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
