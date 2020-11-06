/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.service.filestorage.system;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.JSONable;

/**
 * 文件描述符。
 */
public class FileDescriptor implements JSONable {

    private String fileSystem;

    private JSONObject descriptor;

    public FileDescriptor(String fileSystem) {
        this.fileSystem = fileSystem;
        this.descriptor = new JSONObject();
    }

    public void attr(String name, String value) {
        try {
            this.descriptor.put(name, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void attr(String name, long value) {
        try {
            this.descriptor.put(name, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void attr(String name, int value) {
        try {
            this.descriptor.put(name, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String attr(String name) {
        String value = null;
        try {
            value = this.descriptor.getString(name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public JSONObject toJSON() {
        try {
            this.descriptor.put("system", this.fileSystem);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this.descriptor;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
