/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class GuideFlowable {

    protected String name;

    protected String displayName;

    protected String instruction;

    protected List<GuidanceSection> sectionList;

    protected GuideListener listener;

    public GuideFlowable() {
        this.sectionList = new ArrayList<>();
    }

    public GuideFlowable(JSONObject json) {
        this.sectionList = new ArrayList<>();
        JSONArray array = json.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sectionList.add(new GuidanceSection(array.getJSONObject(i)));
        }
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getInstruction() {
        return this.instruction;
    }

    public void setListener(GuideListener listener) {
        this.listener = listener;
    }

    public Question getQuestion(String sn) {
        for (GuidanceSection section : this.sectionList) {
            Question question = section.getQuestion(sn);
            if (null != question) {
                return question;
            }
        }
        return null;
    }

    public abstract Question getCurrentQuestion();

    public abstract void input(String currentQuestionAnswer, Answer candidate);

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("instruction", this.instruction);
        JSONArray array = new JSONArray();
        for (GuidanceSection section : this.sectionList) {
            array.put(section.toJSON());
        }
        json.put("sections", array);
        return json;
    }
}
