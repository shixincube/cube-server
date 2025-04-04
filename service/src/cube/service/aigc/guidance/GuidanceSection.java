/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GuidanceSection {

    public final String evaluation;

    private List<Question> questionList;

    public GuidanceSection(JSONObject json) {
        this.evaluation = json.getString("evaluation");

        this.questionList = new ArrayList<>();
        JSONArray array = json.getJSONArray("questions");
        for (int i = 0; i < array.length(); ++i) {
            this.questionList.add(new Question(array.getJSONObject(i)));
        }
    }
}
