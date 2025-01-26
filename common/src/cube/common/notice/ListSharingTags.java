/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;
import cube.util.ConfigUtils;

/**
 * 批量获取分享标签。
 */
public class ListSharingTags extends NoticeData {

    public final static String ACTION = FileStorageAction.ListSharingTags.name;

    public final static String BEGIN = "begin";

    public final static String END = "end";

    public final static String BEGIN_TIME = "beginTime";

    public final static String END_TIME = "endTime";

    public final static String VALID = "valid";

    public final static String ORDER = "order";

    public ListSharingTags(long contactId, String domain, int begin, int end, boolean valid) {
        super(ListSharingTags.ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN, begin);
        this.put(END, end);
        this.put(VALID, valid);
        this.put(ORDER, ConfigUtils.ORDER_DESC);
    }

    public ListSharingTags(long contactId, String domain, long beginTime, long endTime, boolean valid) {
        super(ListSharingTags.ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN_TIME, beginTime);
        this.put(END_TIME, endTime);
        this.put(VALID, valid);
        this.put(ORDER, ConfigUtils.ORDER_ASC);
    }
}
