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

import java.util.ArrayList;
import java.util.List;

public class ComprehensiveReport implements JSONable {

    public final Theme theme;

    public final List<Comprehensive> comprehensives;

    public AIGCStateCode state = AIGCStateCode.Processing;

    public boolean finished = false;

    public List<ComprehensiveSection> sections;

    public ComprehensiveReport(Theme theme, List<Comprehensive> comprehensives) {
        this.theme = theme;
        this.comprehensives = comprehensives;
        this.sections = new ArrayList<>();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
