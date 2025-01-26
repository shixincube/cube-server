/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;

/**
 * 批量获取分享标签。
 */
public class ListFiles extends NoticeData {

    public final static String ACTION = FileStorageAction.ListFiles.name;

    public final static String BEGIN_TIME = "beginTime";

    public final static String END_TIME = "endTime";

    public ListFiles(long contactId, String domain, long beginTime, long endTime) {
        super(ListFiles.ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN_TIME, beginTime);
        this.put(END_TIME, endTime);
    }
}
