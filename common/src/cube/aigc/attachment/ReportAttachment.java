/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.attachment;

import cube.common.entity.FileLabel;
import org.json.JSONObject;

public class ReportAttachment extends Attachment {

    public final static String TYPE = "PsychologyReport";

    private FileLabel fileLabel;

    public ReportAttachment(long sn, FileLabel fileLabel) {
        super(TYPE, sn);
        this.fileLabel = fileLabel;
    }

    public ReportAttachment(JSONObject json) {
        super(json);
        this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("fileLabel", this.fileLabel.toCompactJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
