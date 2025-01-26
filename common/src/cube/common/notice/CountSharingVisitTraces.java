/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

/**
 * 计算访问记录数量。
 */
public class CountSharingVisitTraces extends NoticeData {

    public final static String ACTION = "CountSharingVisitTraces";

    public final static String DOMAIN = "domain";

    public final static String CONTACT_ID = "contactId";

    public final static String SHARING_CODE = "sharingCode";

    public final static String TOTAL = "total";

    public CountSharingVisitTraces(String domain, long contactId, String sharingCode) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(CONTACT_ID, contactId);
        this.put(SHARING_CODE, sharingCode);
    }
}
