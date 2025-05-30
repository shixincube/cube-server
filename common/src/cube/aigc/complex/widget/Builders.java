/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import cell.util.log.Logger;
import org.json.JSONObject;

public final class Builders {

    private Builders() {
    }

    public static Widget buildWidget(JSONObject json) {
        try {
            String name = json.getString("name");
            if (ListTile.NAME.equals(name)) {
                return new ListTile(json);
            }
            else if (ListView.NAME.equals(name)) {
                return new ListView(json);
            }
            else {
                Logger.w(Builders.class, "#buildWidget - Unknown name: " + name);
                return null;
            }
        } catch (Exception e) {
            Logger.e(Builders.class, "#buildWidget", e);
            return null;
        }
    }

    public static Action buildAction(JSONObject json) {
        try {
            String name = json.getString("name");
            if (PromptAction.NAME.equals(name)) {
                return new PromptAction(json);
            }
            else {
                Logger.w(Builders.class, "#buildAction - Unknown name: " + name);
                return null;
            }
        } catch (Exception e) {
            Logger.e(Builders.class, "#buildAction", e);
            return null;
        }
    }
}
