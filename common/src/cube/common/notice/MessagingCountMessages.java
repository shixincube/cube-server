/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import org.json.JSONObject;

/**
 * 计算消息条目数。
 */
public class MessagingCountMessages extends NoticeData {

    public final static String ACTION = "CountMessages";

    public final static String DOMAIN = "domain";

    public final static String COUNT = "count";

    public MessagingCountMessages(String domain) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(COUNT, 0);
    }

    public MessagingCountMessages(JSONObject json) {
        super(json, DOMAIN, COUNT);
    }

    public String getDomain() {
        return this.getString(DOMAIN);
    }

    public void setCount(int value) {
        this.put(COUNT, value);
    }

    public int getCount() {
        return this.getInt(COUNT);
    }
}
