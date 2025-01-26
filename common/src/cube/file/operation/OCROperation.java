/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
