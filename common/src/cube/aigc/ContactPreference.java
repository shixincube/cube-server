/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 联系人偏好设置。
 */
public class ContactPreference implements JSONable {

    private long contactId;

    private JSONArray models;

    public ContactPreference(long contactId, JSONArray models) {
        this.contactId = contactId;
        this.models = models;
    }

    public ContactPreference(JSONObject json) {
        this.contactId = json.getLong("contactId");
        this.models = json.getJSONArray("models");
    }

    public JSONArray getModels() {
        return this.models;
    }

    public boolean containsModel(String modelName) {
        for (int i = 0; i < this.models.length(); ++i) {
            String name = this.models.getString(i);
            if (name.equalsIgnoreCase(modelName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("models", this.models);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
