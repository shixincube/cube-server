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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 配置。
 */
public class ConfigInfo implements JSONable {

    public List<ModelConfig> models;

    public List<Notification> notifications;

    public ContactPreference contactPreference;

    public long timeoutMs;

    public int usage;

    public List<Usage> usages;

    public ConfigInfo(List<ModelConfig> models, List<Notification> notifications, ContactPreference contactPreference) {
        this.models = new ArrayList<>(models);
        this.notifications = notifications;
        this.contactPreference = contactPreference;
        this.timeoutMs = 60 * 1000;
        this.usage = 0;
        this.fixModelsWithPreference();
    }

    public ConfigInfo(JSONObject json) {
        this.models = new ArrayList<>();
        JSONArray modelArray = json.getJSONArray("models");
        for (int i = 0; i < modelArray.length(); ++i) {
            ModelConfig mc = new ModelConfig(modelArray.getJSONObject(i));
            this.models.add(mc);
        }

        this.notifications = new ArrayList<>();
        JSONArray notificationArray = json.getJSONArray("notifications");
        for (int i = 0; i < notificationArray.length(); ++i) {
            Notification notification = new Notification(notificationArray.getJSONObject(i));
            this.notifications.add(notification);
        }

        this.timeoutMs = json.getLong("timeoutMs");
        this.usage = json.getInt("usage");

        if (json.has("preference")) {
            this.contactPreference = new ContactPreference(json.getJSONObject("preference"));
            this.fixModelsWithPreference();
        }

        if (json.has("usages")) {
            JSONArray array = json.getJSONArray("usages");
            this.usages = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                this.usages.add(new Usage(array.getJSONObject(i)));
            }
        }
    }

    private void fixModelsWithPreference() {
        if (null == this.contactPreference) {
            return;
        }

        Iterator<ModelConfig> iter = this.models.iterator();
        while (iter.hasNext()) {
            ModelConfig mc = iter.next();
            if (!this.contactPreference.containsModel(mc.getModel())) {
                // 删除不包含的模型配置
                iter.remove();
            }
        }
    }

    public JSONArray getModelsAsJSONArray() {
        JSONArray array = new JSONArray();
        for (ModelConfig mc : this.models) {
            array.put(mc.toJSON());
        }
        return array;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        JSONArray modelArray = new JSONArray();
        for (ModelConfig model : this.models) {
            modelArray.put(model.toJSON());
        }

        JSONArray notificationArray = new JSONArray();
        for (Notification notification : this.notifications) {
            notificationArray.put(notification.toJSON());
        }

        if (null != this.contactPreference) {
            json.put("preference", this.contactPreference.toJSON());
        }

        if (null != this.usages) {
            JSONArray array = new JSONArray();
            for (Usage usage : this.usages) {
                array.put(usage.toCompactJSON());
            }
            json.put("usages", array);
        }

        json.put("models", modelArray);
        json.put("notifications", notificationArray);
        json.put("timeoutMs", this.timeoutMs);
        json.put("usage", this.usage);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
