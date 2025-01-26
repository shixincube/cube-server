/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

/**
 * 遍历访问记录。
 */
public class TraverseVisitTrace extends NoticeData {

    public final static String ACTION = "TraverseVisitTrace";

    public final static String DOMAIN = "domain";

    public final static String CONTACT_ID = "contactId";

    public final static String SHARING_CODE = "sharingCode";

    public TraverseVisitTrace(String domain, long contactId, String sharingCode) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(CONTACT_ID, contactId);
        this.put(SHARING_CODE, sharingCode);
    }
}
