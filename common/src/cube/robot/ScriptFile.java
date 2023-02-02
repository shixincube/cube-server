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
