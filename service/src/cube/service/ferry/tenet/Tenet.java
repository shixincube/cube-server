/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.ferry.tenet;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 信条。
 */
public abstract class Tenet implements JSONable {

    private final String port;

    private String domain;

    private long timestamp;

    public Tenet(String port, String domain, long timestamp) {
        this.port = port;
        this.domain = domain;
        this.timestamp = timestamp;
    }

    public String getPort() {
        return this.port;
    }

    public String getDomain() {
        return this.domain;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("port", this.port);
        json.put("domain", this.domain);
        json.put("timestamp", this.timestamp);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
