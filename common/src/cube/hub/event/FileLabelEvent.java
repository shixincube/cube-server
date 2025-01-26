/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.FileLabel;
import org.json.JSONObject;

/**
 * 文件标签事件。
 */
public class FileLabelEvent extends WeChatEvent {

    public final static String NAME = "FileLabel";

    public FileLabelEvent(long sn, FileLabel fileLabel) {
        super(sn, NAME);
        setFileLabel(fileLabel);
    }

    public FileLabelEvent(JSONObject json) {
        super(json);
    }
}
