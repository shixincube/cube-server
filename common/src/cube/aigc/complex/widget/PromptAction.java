/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import org.json.JSONObject;

public class PromptAction extends Action {

    public final static String NAME = "PromptAction";

    public String prompt;

    public boolean direct = true;

    public PromptAction(String prompt) {
        super(NAME);
        this.prompt = prompt;
    }

    public PromptAction(JSONObject json) {
        super(json);
        this.prompt = json.getString("prompt");
        this.direct = json.getBoolean("direct");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("prompt", this.prompt);
        json.put("direct", this.direct);
        return json;
    }
}
