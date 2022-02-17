/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

import java.io.File;

/**
 * 处理结果流描述。
 */
public class ProcessResultStream implements JSONable {

    public String fullPath;

    public String streamName;

    public String fileName;

    public ProcessResultStream(File file) {
        this.fullPath = file.getAbsolutePath();
        this.fileName = file.getName();
        this.streamName = this.fileName;
    }

    public ProcessResultStream(JSONObject json) {
        this.fullPath = json.getString("fullPath");
        this.streamName = json.getString("streamName");
        this.fileName = json.getString("fileName");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fullPath", this.fullPath);
        json.put("streamName", this.streamName);
        json.put("fileName", this.fileName);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
