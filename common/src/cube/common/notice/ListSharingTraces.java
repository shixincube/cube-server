/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;

/**
 * 批量获取访问记录。
 */
public class ListSharingTraces extends NoticeData {

    public final static String ACTION = FileStorageAction.ListSharingTraces.name;

    public final static String SHARING_CODE = "sharingCode";

    public final static String BEGIN = "begin";

    public final static String END = "end";

    public final static String BEGIN_TIME = "beginTime";

    public final static String END_TIME = "endTime";

    public ListSharingTraces(long contactId, String domain, String sharingCode, int begin, int end) {
        super(ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(SHARING_CODE, sharingCode);
        this.put(BEGIN, begin);
        this.put(END, end);
    }

    public ListSharingTraces(long contactId, String domain, long beginTime, long endTime) {
        super(ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN_TIME, beginTime);
        this.put(END_TIME, endTime);
    }
}
