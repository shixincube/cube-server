/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import org.json.JSONObject;

/**
 * 计算分享标签。
 */
public class CountSharingTags extends NoticeData {

    public final static String ACTION = "CountSharingTags";

    public final static String DOMAIN = "domain";

    public final static String CONTACT_ID = "contactId";

    public final static String VALID_NUMBER = "valid";

    public final static String INVALID_NUMBER = "invalid";

    public CountSharingTags(String domain, long contactId) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(CONTACT_ID, contactId);
    }

    public static int parseValidNumber(JSONObject json) {
        return json.has(VALID_NUMBER) ? json.getInt(VALID_NUMBER) : 0;
    }

    public static int parseInvalidNumber(JSONObject json) {
        return json.has(INVALID_NUMBER) ? json.getInt(INVALID_NUMBER) : 0;
    }
}
