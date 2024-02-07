/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.aigc.attachment;

import cube.aigc.attachment.ui.Button;
import cube.aigc.attachment.ui.Component;
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
            for (Component component : this.actions) {
                actionArray.put(component.toJSON());
            }
            json.put("actions", actionArray);
        }

        return json;
    }
}
