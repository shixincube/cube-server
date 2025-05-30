/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListView extends Widget {

    public final static String NAME = "ListView";

    public List<Widget> items = new ArrayList<>();

    public ListView() {
        super(NAME);
    }

    public ListView(JSONObject json) {
        super(json);
        JSONArray array = json.getJSONArray("items");
        for (int i = 0; i < array.length(); ++i) {
            Widget item = Builders.buildWidget(array.getJSONObject(i));
            if (null != item) {
                this.items.add(item);
            }
        }
    }

    public void addItem(Widget widget) {
        this.items.add(widget);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        JSONArray array = new JSONArray();
        for (Widget item : this.items) {
            array.put(item.toJSON());
        }
        json.put("items", array);
        return json;
    }
}
