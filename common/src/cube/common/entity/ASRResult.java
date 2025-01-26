/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 语音识别结果。
 */
public class ASRResult implements JSONable {

    private JSONObject data;

    public ASRResult(JSONObject json) {
        this.data = json;
    }

    public ASRResult(FileLabel fileLabel, JSONObject json) {
        this.data = json;

        this.data.put("fileCode", fileLabel.getFileCode());
        this.data.put("fileLabel", fileLabel.toCompactJSON());

        if (this.data.has("list")) {
            JSONArray list = this.data.getJSONArray("list");
            for (int i = 0; i < list.length(); ++i) {
                JSONObject item = list.getJSONObject(i);
                if (item.has("start_timestamps")) {
                    item.remove("start_timestamps");
                }
                if (item.has("end_timestamps")) {
                    item.remove("end_timestamps");
                }
            }
        }
    }

    public String getFileCode() {
        return this.data.getString("fileCode");
    }

    @Override
    public JSONObject toJSON() {
        return this.data;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
