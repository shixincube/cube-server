/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 条形码信息。
 */
public class BarCodeInfo extends Entity {

    private FileLabel file;

    private List<BarCode> barcodes;

    public BarCodeInfo(FileLabel file, List<BarCode> barcodes) {
        super();
        this.file = file;
        this.barcodes = barcodes;
    }

    public BarCodeInfo(JSONObject json) {
        super(json);
        this.file = new FileLabel(json.getJSONObject("file"));
        this.barcodes = new ArrayList<>();
        JSONArray array = json.getJSONArray("barcodes");
        for (int i = 0; i < array.length(); ++i) {
            BarCode barCode = new BarCode(array.getJSONObject(i));
            this.barcodes.add(barCode);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("file", this.file.toCompactJSON());
        JSONArray array = new JSONArray();
        for (BarCode barCode : this.barcodes) {
            array.put(barCode.toCompactJSON());
        }
        json.put("barcodes", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
