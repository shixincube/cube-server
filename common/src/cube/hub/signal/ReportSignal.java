/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 让客户端提交报告信令。
 */
public class ReportSignal extends Signal {

    public final static String NAME = "Report";

    public ReportSignal() {
        super(NAME);
    }

    public ReportSignal(JSONObject json) {
        super(json);
    }
}
