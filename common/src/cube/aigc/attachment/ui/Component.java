/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    protected Object context;

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

    public Object getContext() {
        return this.context;
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
