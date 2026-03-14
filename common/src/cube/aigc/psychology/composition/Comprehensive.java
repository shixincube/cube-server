/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.ComprehensiveSection;
import cube.aigc.psychology.Painting;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comprehensive implements JSONable {

    private String name;

    private Attribute attribute;

    private List<FileLabel> fileLabels;

    private Map<String, Painting> paintingMap;

    private List<Scale> scales;

    private List<String> resultKeywords;

    private List<ComprehensiveSection> resultSections;

    public Comprehensive(JSONObject json) {
        this.name = json.getString("name");
        this.attribute = new Attribute(json.getJSONObject("attribute"));

        this.fileLabels = new ArrayList<>();
        JSONArray fileLabelArray = json.getJSONArray("fileLabels");
        for (int i = 0; i < fileLabelArray.length(); ++i) {
            JSONObject fileLabelJson = fileLabelArray.getJSONObject(i);
            this.fileLabels.add(new FileLabel(fileLabelJson));
        }
        this.paintingMap = new HashMap<>();

        this.scales = new ArrayList<>();
        JSONArray scaleArray = json.getJSONArray("scales");
        for (int i = 0; i< scaleArray.length(); ++i) {
            JSONObject scaleJson = scaleArray.getJSONObject(i);
            this.scales.add(new Scale(scaleJson));
        }
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public FileLabel getFileLabel() {
        if (this.fileLabels.isEmpty()) {
            return null;
        }

        return this.fileLabels.get(0);
    }

    public Scale getScale() {
        if (this.scales.isEmpty()) {
            return null;
        }

        return this.scales.get(0);
    }

    public void setPainting(String fileCode, Painting painting) {
        this.paintingMap.put(fileCode, painting);
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
