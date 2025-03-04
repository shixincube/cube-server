/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.attachment.Attachment;
import org.json.JSONObject;

public class ReportAttachment extends Attachment {

    public final static String TYPE = "PsychologyReport";

    private String fileCode;

    public ReportAttachment(long sn, String fileCode) {
        super(TYPE, sn);
        this.fileCode = fileCode;
    }

    public ReportAttachment(JSONObject json) {
        super(json);
        this.fileCode = json.getString("fileCode");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("fileCode", this.fileCode);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
