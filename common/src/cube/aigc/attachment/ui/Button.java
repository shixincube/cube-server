/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.attachment.ui;

import org.json.JSONObject;

/**
 * 按钮组件。
 */
public class Button extends Component {

    public final static String NAME = "Button";

    private String text;

    private ButtonListener listener;

    public Button(JSONObject json) {
        super(json);
        this.text = json.getString("text");
    }

    public Button(String text, ButtonListener listener, Object context) {
        super(NAME);
        this.text = text;
        this.listener = listener;
        this.context = context;
    }

    public static boolean isButton(JSONObject json) {
        return json.has("name") && json.getString("name").equals(NAME);
    }

    public ButtonListener getListener() {
        return this.listener;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("text", this.text);
        json.put("loading", false);
        return json;
    }
}
