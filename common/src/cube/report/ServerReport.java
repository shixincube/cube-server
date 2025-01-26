/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import org.json.JSONObject;

/**
 * 服务器状态报告。
 */
public class ServerReport extends Report {

    public ServerReport() {
        super("ServerReport");
    }

    public void setBinding(String bindingAddress, int bindingPort) {

    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        return json;
    }
}
