/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module.task;

import cube.aigc.Generatable;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import cube.service.aigc.module.FlowTask;
import cube.service.contact.ContactManager;

public class QueryUser extends FlowTask {

    public final static String TASK_NAME = "{{app_query_user}}";

    public QueryUser(AIGCService service, String query) {
        super(service, query);
    }

    @Override
    public GeneratingRecord generate(Generatable generator) {
        
        return null;
    }
}
