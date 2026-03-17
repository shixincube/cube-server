/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.composition.Comprehensive;
import cube.common.JSONable;
import cube.common.state.AIGCStateCode;
import cube.util.ConfigUtils;
import cube.util.Gender;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 心理融合报告。
 */
public class ComprehensiveReport implements JSONable {

    public final long sn;

    public final Theme theme;

    public final List<Comprehensive> comprehensives;

    public AIGCStateCode state = AIGCStateCode.Processing;

    public boolean finished = false;

    public List<ComprehensiveSection> sections;

    public ComprehensiveReport(Theme theme, List<Comprehensive> comprehensives) {
        this.sn = ConfigUtils.generateSerialNumber();
        this.theme = theme;
        this.comprehensives = comprehensives;
        this.sections = new ArrayList<>();
    }

    public ComprehensiveReport(JSONObject json) {
        this.sn = json.getLong("sn");
        this.theme = Theme.parse(json.getString("theme"));

        this.comprehensives = new ArrayList<>();
        JSONArray array = json.getJSONArray("comprehensives");
        for (int i = 0; i < array.length(); ++i) {
            this.comprehensives.add(new Comprehensive(array.getJSONObject(i)));
        }

        this.state = AIGCStateCode.parse(json.getInt("state"));
        this.finished = json.getBoolean("finished");

        this.sections = new ArrayList<>();
        array = json.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sections.add(new ComprehensiveSection(array.getJSONObject(i)));
        }
    }

    public Comprehensive getComprehensiveByGender(Gender gender) {
        for (Comprehensive comprehensive : this.comprehensives) {
            Gender current = Gender.parse(comprehensive.getAttribute().gender);
            if (current == gender) {
                return comprehensive;
            }
        }
        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("theme", this.theme.code);

        JSONArray array = new JSONArray();
        for (Comprehensive comprehensive : this.comprehensives) {
            array.put(comprehensive.toJSON());
        }
        json.put("comprehensives", array);

        json.put("state", this.state.code);
        json.put("finished", this.finished);

        array = new JSONArray();
        for (ComprehensiveSection section : this.sections) {
            array.put(section.toJSON());
        }
        json.put("sections", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
