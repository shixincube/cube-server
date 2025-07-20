/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.attachment;

import cube.common.entity.FileLabel;

public class FileAttachment extends Attachment {

    public final static String TYPE = "File";

    public FileLabel fileLabel;

    public FileAttachment(FileLabel fileLabel) {
        super(TYPE);
        this.fileLabel = fileLabel;
    }
}
