/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.attachment;

import cube.aigc.complex.widget.Button;
import cube.aigc.complex.widget.Widget;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Thing 类型附件。
 */
public class ThingAttachment extends Attachment {

    public final static String TYPE = "Thing";

    public String content;

//    public String avatar;

//    public String header;

//    public String headerExtra;

//    public String description;

//    public String footer;

    public List<Button> actions;

    public ThingAttachment(JSONObject json) {
        super(json);
        this.content = json.getString("content");

        if (json.has("actions")) {
            this.actions = new ArrayList<>();
            JSONArray actionArray = json.getJSONArray("actions");
            for (int i = 0; i < actionArray.length(); ++i) {
                JSONObject data = actionArray.getJSONObject(i);
                if (Button.isButton(data)) {
                    this.actions.add(new Button(data));
                }
            }
        }
    }

    public ThingAttachment(String content) {
        super(TYPE);
        this.content = content;
    }

    public void addActionButton(Button button) {
        if (null == this.actions) {
            this.actions = new ArrayList<>();
        }

        this.actions.add(button);
    }

    public Button getActionButton(long id) {
        if (null == this.actions) {
            return null;
        }

        for (Button btn : this.actions) {
            if (btn.getId() == id) {
                return btn;
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("content", this.content);

        if (null != this.actions) {
            JSONArray actionArray = new JSONArray();
            for (Widget widget : this.actions) {
                actionArray.put(widget.toJSON());
            }
            json.put("actions", actionArray);
        }

        return json;
    }
}
