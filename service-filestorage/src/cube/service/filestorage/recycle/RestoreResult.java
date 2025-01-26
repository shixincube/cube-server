/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.recycle;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 恢复数据的结果。
 */
public class RestoreResult implements JSONable {

    public final List<Trash> successList;

    public final List<Trash> failureList;

    public RestoreResult() {
        this.successList = new ArrayList<>();
        this.failureList = new ArrayList<>();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        for (Trash trash : this.successList) {
            array.put(trash.toJSON());
        }
        json.put("successList", array);

        array = new JSONArray();
        for (Trash trash : this.failureList) {
            array.put(trash.toJSON());
        }
        json.put("failureList", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        for (Trash trash : this.successList) {
            array.put(trash.toCompactJSON());
        }
        json.put("successList", array);

        array = new JSONArray();
        for (Trash trash : this.failureList) {
            array.put(trash.toCompactJSON());
        }
        json.put("failureList", array);

        return json;
    }
}
