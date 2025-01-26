/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.RiskManagementAction;

/**
 * 批量获取联系人行为。
 */
public class ListContactBehaviors extends NoticeData {

    public final static String ACTION = RiskManagementAction.ListContactBehaviors.name;

    public final static String BEGIN_TIME = "beginTime";

    public final static String END_TIME = "endTime";

    public final static String BEHAVIOR = "behavior";

    public ListContactBehaviors(long contactId, String domain, long beginTime, long endTime) {
        super(ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN_TIME, beginTime);
        this.put(END_TIME, endTime);
    }

    public ListContactBehaviors(long contactId, String domain, long beginTime, long endTime, String behavior) {
        super(ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN_TIME, beginTime);
        this.put(END_TIME, endTime);
        this.put(BEHAVIOR, behavior);
    }
}
