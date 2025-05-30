/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.resource;

import cube.aigc.complex.attachment.CardAttachment;
import cube.aigc.complex.attachment.ThingAttachment;
import cube.aigc.complex.widget.Button;

/**
 * 附件构建器。
 */
public class AttachmentBuilder {

    public AttachmentBuilder() {
    }

    public CardAttachment buildCard(String content, Button okButton) {
        return null;
    }

    public ThingAttachment buildThing(String content, Button viewButton) {
        ThingAttachment attachment = new ThingAttachment(content);
        attachment.addActionButton(viewButton);
        return attachment;
    }
}
