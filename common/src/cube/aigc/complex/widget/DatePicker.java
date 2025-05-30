/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 日期选择器组件。
 */
public class DatePicker extends Widget {

    public final static String TYPE_DATE = "date";

    public final static String TYPE_DATE_TIME = "datetime";

    public final static String TYPE_DATE_RANGE = "daterange";

    private String type;

    private boolean clearable;

    private JSONArray range;

    public DatePicker(String type) {
        super("DatePicker");
        this.type = type;
        this.clearable = false;
    }

    public void setRange(long start, long end) {
        this.range = new JSONArray();
        this.range.put(start);
        this.range.put(end);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("type", this.type);
        json.put("clearable", this.clearable);

        if (null != this.range) {
            json.put("range", this.range);
        }

        return json;
    }
}
