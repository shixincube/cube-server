/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import org.json.JSONObject;

public class Answer {

    public final String code;

    public final String content;

    public Answer(JSONObject json) {
        this.code = json.getString("code");
        this.content = json.getString("content");
    }
}
