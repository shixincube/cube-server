/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import cube.aigc.Flowable;
import cube.service.aigc.AIGCService;

public abstract class FlowTask implements Flowable {

    protected AIGCService service;

    protected String query;

    public FlowTask(AIGCService service, String query) {
        this.service = service;
        this.query = query;
    }
}
