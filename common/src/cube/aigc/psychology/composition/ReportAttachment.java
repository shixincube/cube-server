/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.attachment.Attachment;

public class ReportAttachment extends Attachment {

    public final static String TYPE = "PsychologyReport";

    public ReportAttachment(long sn) {
        super(TYPE, sn);
    }
}
