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

    public final long timestamp;

    public final Theme theme;

    public final List<Comprehensive> comprehensives;

    public AIGCStateCode state = AIGCStateCode.Processing;

    public boolean finished = false;

    public long elapsed;

    private String summary;

    private List<ComprehensiveSection> sections;

    public ComprehensiveReport(Theme theme, List<Comprehensive> comprehensives) {
        this.sn = ConfigUtils.generateSerialNumber();
        this.timestamp = System.currentTimeMillis();
        this.theme = theme;
        this.comprehensives = comprehensives;
        this.summary = "";
        this.sections = new ArrayList<>();
    }

    public ComprehensiveReport(JSONObject json) {
        this.sn = json.getLong("sn");
        this.timestamp = json.getLong("timestamp");
        this.elapsed = json.getLong("elapsed");
        this.theme = Theme.parse(json.getString("theme"));

        this.comprehensives = new ArrayList<>();
        JSONArray array = json.getJSONArray("comprehensives");
        for (int i = 0; i < array.length(); ++i) {
            this.comprehensives.add(new Comprehensive(array.getJSONObject(i)));
        }

        this.state = AIGCStateCode.parse(json.getInt("state"));
        this.finished = json.getBoolean("finished");

        this.summary = json.has("summary") ? json.getString("summary") : "";
        this.sections = new ArrayList<>();
        array = json.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sections.add(new ComprehensiveSection(array.getJSONObject(i)));
        }
    }

    public ComprehensiveReport(long sn, long timestamp, long elapsed, Theme theme, AIGCStateCode state) {
        this.sn = sn;
        this.timestamp = timestamp;
        this.elapsed = elapsed;
        this.theme = theme;
        this.comprehensives = new ArrayList<>();
        this.state = state;
        this.finished = true;
        this.summary = "";
        this.sections = new ArrayList<>();
    }

    public void addComprehensive(Comprehensive comprehensive) {
        this.comprehensives.add(comprehensive);
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

    public void setSummary(String text) {
        this.summary = text;
    }

    public String getSummary() {
        return this.summary;
    }

    public void addSection(ComprehensiveSection section) {
        this.sections.add(section);
    }

    public JSONArray outputSections() {
        JSONArray array = new JSONArray();
        for (ComprehensiveSection section : this.sections) {
            array.put(section.toJSON());
        }
        return array;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("timestamp", this.timestamp);
        json.put("elapsed", this.elapsed);
        json.put("theme", this.theme.code);

        JSONArray array = new JSONArray();
        for (Comprehensive comprehensive : this.comprehensives) {
            array.put(comprehensive.toJSON());
        }
        json.put("comprehensives", array);

        json.put("state", this.state.code);
        json.put("finished", this.finished);

        json.put("summary", this.summary);
        json.put("sections", this.outputSections());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
