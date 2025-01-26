/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.FileLabel;
import org.json.JSONObject;

/**
 * 屏幕快照事件。
 */
public class ScreenshotEvent extends WeChatEvent {

    public final static String NAME = "Screenshot";

    public ScreenshotEvent(FileLabel fileLabel) {
        super(NAME);
        setFileLabel(fileLabel);
    }

    public ScreenshotEvent(JSONObject json) {
        super(json);
    }
}
