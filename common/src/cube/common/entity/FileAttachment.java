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

package cube.common.entity;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.JSONable;

/**
 * 文件附件。
 */
public class FileAttachment implements JSONable {

    private JSONObject fileAnchor;

    private FileLabel fileLabel;

    public FileAttachment(JSONObject json) {
        try {
            if (json.has("anchor")) {
                this.fileAnchor = json.getJSONObject("anchor");
            }
            if (json.has("label")) {
                this.fileLabel = new FileLabel(json.getJSONObject("label"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getFileCode() {
        if (null != this.fileAnchor) {
            try {
                return this.fileAnchor.getString("fileCode");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (null != this.fileLabel) {
            return this.fileLabel.getFileCode();
        }

        return null;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        if (null != this.fileAnchor) {
            try {
                json.put("anchor", this.fileAnchor);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (null != this.fileLabel) {
            try {
                json.put("label", this.fileLabel.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        return json;
    }
}
