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
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 融合评测里表示单一被测实体的类。
 */
public class Comprehensive implements JSONable {

    private String name;

    private Attribute attribute;

    private List<String> fileCodes;

    private List<FileLabel> fileLabels;

    private Map<String, Painting> paintingMap;

    private List<Scale> scales;

    private List<String> resultKeywords;

    private List<ComprehensiveSection> resultSections;

    public Comprehensive(JSONObject json) {
        this.name = json.getString("name");
        this.attribute = new Attribute(json.getJSONObject("attribute"));

        this.fileCodes = new ArrayList<>();
        this.fileLabels = new ArrayList<>();
        if (json.has("files")) {
            JSONArray fileLabelArray = json.getJSONArray("files");
            for (int i = 0; i < fileLabelArray.length(); ++i) {
                JSONObject fileLabelJson = fileLabelArray.getJSONObject(i);
                FileLabel fileLabel = new FileLabel(fileLabelJson);
                this.fileLabels.add(fileLabel);
                this.fileCodes.add(fileLabel.getFileCode());
            }
        }
        else if (json.has("fileCodes")) {
            JSONArray fileCodeArray = json.getJSONArray("fileCodes");
            this.fileCodes.addAll(JSONUtils.toStringList(fileCodeArray));
        }

        this.paintingMap = new HashMap<>();

        this.scales = new ArrayList<>();
        JSONArray scaleArray = json.getJSONArray("scales");
        for (int i = 0; i< scaleArray.length(); ++i) {
            JSONObject scaleJson = scaleArray.getJSONObject(i);
            this.scales.add(new Scale(scaleJson));
        }

        if (json.has("resultKeywords")) {
            this.resultKeywords = JSONUtils.toStringList(json.getJSONArray("resultKeywords"));
        }

        if (json.has("resultSections")) {
            this.resultSections = new ArrayList<>();
            JSONArray array = json.getJSONArray("resultSections");
            for (int i = 0; i < array.length(); ++i) {
                this.resultSections.add(new ComprehensiveSection(array.getJSONObject(i)));
            }
        }
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public boolean hasFileLabels() {
        return (!this.fileLabels.isEmpty());
    }

    public List<String> getFileCodes() {
        return this.fileCodes;
    }

    public void addFileLabel(FileLabel fileLabel) {
        this.fileLabels.add(fileLabel);
    }

    public String getKeywordWithGender() {
        if (this.attribute.isMale()) {
            return "男方" + this.resultSections.get(0).title;
        }
        else {
            return "女方" + this.resultSections.get(0).title;
        }
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

    public void addResultKeywords(String keyword) {
        if (null == this.resultKeywords) {
            this.resultKeywords = new ArrayList<>();
        }
        this.resultKeywords.add(keyword);
    }

    public void addComprehensiveSection(ComprehensiveSection section) {
        if (null == this.resultSections) {
            this.resultSections = new ArrayList<>();
        }
        this.resultSections.add(section);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());

        if (!this.fileLabels.isEmpty()) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.fileLabels) {
                array.put(fileLabel.toCompactJSON());
            }
            json.put("files", array);
        }
        else if (!this.fileCodes.isEmpty()) {
            json.put("fileCodes", JSONUtils.toStringArray(this.fileCodes));
        }

        JSONArray array = new JSONArray();
        for (Scale scale : this.scales) {
            array.put(scale.toJSON());
        }
        json.put("scales", array);

        if (null != this.resultKeywords) {
            json.put("resultKeywords", JSONUtils.toStringArray(this.resultKeywords));
        }

        if (null != this.resultSections) {
            JSONArray sectionArray = new JSONArray();
            for (ComprehensiveSection section : this.resultSections) {
                sectionArray.put(section.toJSON());
            }
            json.put("resultSections", sectionArray);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
