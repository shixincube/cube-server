/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.copilot.PersonalityTrait;
import cube.common.JSONable;
import org.json.JSONObject;

public class CopilotSetting implements JSONable {

    public final PersonalityTrait personalityTrait;

    public CopilotSetting(JSONObject json) {
        this.personalityTrait = PersonalityTrait.Random;
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
