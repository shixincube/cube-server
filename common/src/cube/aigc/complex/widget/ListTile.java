/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import org.json.JSONObject;

public class ListTile extends Widget {

    public final static String NAME = "ListTile";

    public final String title;

    public String subtitle;

    public boolean dense = false;

    public Widget leading;

    public Widget trailing;

    public Action onTap;

    public ListTile(String title) {
        super(NAME);
        this.title = title;
    }

    public ListTile(JSONObject json) {
        super(json);
        this.title = json.getString("title");
        this.subtitle = json.has("subtitle") ? json.getString("subtitle") : null;
        this.dense = json.has("dense") ? json.getBoolean("dense") : false;
        this.leading = json.has("leading") ? Builders.buildWidget(json.getJSONObject("leading")) : null;
        this.trailing = json.has("trailing") ? Builders.buildWidget(json.getJSONObject("trailing")) : null;
        this.onTap = json.has("onTap") ? Builders.buildAction(json.getJSONObject("onTap")) : null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("title", this.title);
        if (null != this.subtitle) {
            json.put("subtitle", this.subtitle);
        }
        json.put("dense", this.dense);
        if (null != this.leading) {
            json.put("leading", this.leading.toJSON());
        }
        if (null != this.trailing) {
            json.put("trailing", this.trailing.toJSON());
        }
        if (null != this.onTap) {
            json.put("onTap", this.onTap.toJSON());
        }
        return json;
    }
}
