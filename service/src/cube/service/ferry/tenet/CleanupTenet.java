/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.ferry.tenet;

import cube.ferry.FerryPort;
import org.json.JSONObject;

/**
 * 清空信条。
 */
public class CleanupTenet extends Tenet {

    private boolean all;

    public CleanupTenet(String domain, long timestamp) {
        super(FerryPort.Cleanup, domain, timestamp);
        this.all = true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("all", this.all);
        return json;
    }
}
