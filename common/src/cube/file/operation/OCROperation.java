/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.file.operation;

import cube.common.JSONable;
import cube.common.action.FileProcessorAction;
import cube.file.FileOperation;
import org.json.JSONObject;

/**
 * OCR 操作配置。
 */
public class OCROperation implements FileOperation, JSONable {

    public final static String LANG_CHINESE = "chi_sim";

    public final static String LANG_ENGLISH = "eng";

    private String language;

    private boolean singleTextLine = false;

    public OCROperation() {
    }

    public OCROperation(String language) {
        this.language = language;
    }

    public OCROperation(boolean singleTextLine) {
        this.singleTextLine = singleTextLine;
    }

    public OCROperation(String language, boolean singleTextLine) {
        this.language = language;
        this.singleTextLine = singleTextLine;
    }

    public OCROperation(JSONObject json) {
        if (json.has("lang")) {
            this.language = json.getString("lang");
        }

        if (json.has("singleLine")) {
            this.singleTextLine = json.getBoolean("singleLine");
        }
    }

    public String getLanguage() {
        return this.language;
    }

    public boolean isSingleTextLine() {
        return this.singleTextLine;
    }

    @Override
    public String getProcessAction() {
        return FileProcessorAction.OCR.name;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("process", this.getProcessAction());

        json.put("singleLine", this.singleTextLine);

        if (null != this.language) {
            json.put("lang", this.language);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
