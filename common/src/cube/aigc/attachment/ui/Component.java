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

package cube.aigc.attachment.ui;

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 互动组件。
 */
public abstract class Component {

    protected final long id;

    protected final String name;

    protected boolean disposable;

//    protected Map<String, String> attributes;

    public Component(String name) {
        this.id = Utils.generateSerialNumber();
        this.name = name;
        this.disposable = false;
//        this.attributes = new HashMap<>();
    }

    public Component(JSONObject json) {
        this.id = json.getLong("id");
        this.name = json.getString("name");
        this.disposable = json.getBoolean("disposable");
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isDisposable() {
        return this.disposable;
    }

    public void setDisposable(boolean value) {
        this.disposable = value;
    }

//    public void addAttribute(String key, String value) {
//        this.attributes.put(key, value);
//    }

//    public String removeAttribute(String key) {
//        return this.attributes.remove(key);
//    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("name", this.name);
        json.put("disposable", this.disposable);

//        for (Map.Entry<String, String> e : this.attributes.entrySet()) {
//            json.put(e.getKey(), e.getValue());
//        }
        return json;
    }
}
