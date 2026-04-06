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

    private String role;

    private Attribute attribute;

    private List<String> fileCodes;

    private List<String> fileUrls;

    private List<FileLabel> fileLabels;

    private Map<String, Painting> paintingMap;

    private List<String> choices;

    private List<Scale> scales;

    private List<ComprehensiveSection> sections;

    public Comprehensive(JSONObject json) {
        this.name = json.getString("name");
        this.role = json.has("role") ? json.getString("role") : null;
        this.attribute = new Attribute(json.getJSONObject("attribute"));

        this.fileCodes = new ArrayList<>();
        this.fileUrls = new ArrayList<>();
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
        else if (json.has("fileUrls")) {
            JSONArray fileUrlArray = json.getJSONArray("fileUrls");
            this.fileUrls.addAll(JSONUtils.toStringList(fileUrlArray));
        }

        this.paintingMap = new HashMap<>();

        this.scales = new ArrayList<>();
        if (json.has("scales")) {
            JSONArray scaleArray = json.getJSONArray("scales");
            for (int i = 0; i< scaleArray.length(); ++i) {
                JSONObject scaleJson = scaleArray.getJSONObject(i);
                this.scales.add(new Scale(scaleJson));
            }
        }

        if (json.has("choices")) {
            this.choices = JSONUtils.toStringList(json.getJSONArray("choices"));
        }
        else {
            this.choices = new ArrayList<>();
        }

        if (json.has("sections")) {
            this.sections = new ArrayList<>();
            JSONArray array = json.getJSONArray("sections");
            for (int i = 0; i < array.length(); ++i) {
                this.sections.add(new ComprehensiveSection(array.getJSONObject(i)));
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getRole() {
        return this.role;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public boolean isValidFile() {
        return (!this.fileLabels.isEmpty()) || (!this.fileCodes.isEmpty()) || (!this.fileUrls.isEmpty());
    }

    public boolean hasFileLabels() {
        return (!this.fileLabels.isEmpty());
    }

    public List<String> getFileCodes() {
        return this.fileCodes;
    }

    public List<String> getFileUrls() {
        return this.fileUrls;
    }

    public void addFileLabel(FileLabel fileLabel) {
        this.fileLabels.add(fileLabel);
    }

    public String getKeywordWithGender() {
        if (this.attribute.isMale()) {
            return "男方" + this.sections.get(0).title;
        }
        else {
            return "女方" + this.sections.get(0).title;
        }
    }

    public FileLabel getFileLabel() {
        if (this.fileLabels.isEmpty()) {
            return null;
        }

        return this.fileLabels.get(0);
    }

    public List<String> getChoices() {
        return this.choices;
    }

    public String buildChoicesString() {
        StringBuilder buf = new StringBuilder();
        for (String word : this.choices) {
            buf.append(word);
            buf.append("，");
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    public boolean hasScales() {
        return (!this.scales.isEmpty());
    }

    public void addScale(Scale scale) {
        this.scales.add(scale);
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

    public void addComprehensiveSection(ComprehensiveSection section) {
        if (null == this.sections) {
            this.sections = new ArrayList<>();
        }
        this.sections.add(section);
    }

    public ComprehensiveSection getComprehensiveSection() {
        return this.sections.get(0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());

        if (null != this.role) {
            json.put("role", this.role);
        }

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
        else if (!this.fileUrls.isEmpty()) {
            json.put("fileUrls", JSONUtils.toStringArray(this.fileUrls));
        }

        JSONArray array = new JSONArray();
        for (Scale scale : this.scales) {
            array.put(scale.toJSON());
        }
        json.put("scales", array);

        json.put("choices", JSONUtils.toStringArray(this.choices));

        if (null != this.sections) {
            JSONArray sectionArray = new JSONArray();
            for (ComprehensiveSection section : this.sections) {
                sectionArray.put(section.toJSON());
            }
            json.put("sections", sectionArray);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
