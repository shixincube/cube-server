/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.robot;

import cube.common.JSONable;
import org.json.JSONObject;

import java.io.File;

/**
 * 脚本文件。
 */
public class ScriptFile implements JSONable {

    public String name;

    public long size;

    public long lastModified;

    public String absolutePath;

    public String relativePath;

    public ScriptFile(File file, String relativePath) {
        this.name = file.getName();
        this.size = file.length();
        this.lastModified = file.lastModified();
        this.absolutePath = file.getAbsolutePath();
        this.relativePath = relativePath;
    }

    public ScriptFile(JSONObject json) {
        this.name = json.getString("name");
        this.size = json.getLong("size");
        this.lastModified = json.getLong("lastModified");
        this.absolutePath = json.getString("absolutePath");
        this.relativePath = json.getString("relativePath");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("size", this.size);
        json.put("lastModified", this.lastModified);
        json.put("absolutePath", this.absolutePath);
        json.put("relativePath", this.relativePath);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
