/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import org.json.JSONObject;

/**
 * 通知数据。
 */
public class NoticeData extends JSONObject {

    public final static String ACTION = "action";

    public final static String ASYNC_NOTIFIER = "_async_notifier";

    public final static String CONTACT_ID = "contactId";

    public final static String DOMAIN = "domain";

    public final static String PARAMETER = "parameter";

    public final static String CODE = "code";

    public final static String DATA = "data";

    public final static String FILE_CODE = "fileCode";

    public final static String PATH = "path";

    public final static String FILE_LABEL = "fileLabel";

    public NoticeData(String action) {
        super();
        this.put(NoticeData.ACTION, action);
    }

    public NoticeData(JSONObject json, String... names) {
        super(json, names);
        this.put(NoticeData.ACTION, json.getString(NoticeData.ACTION));
    }

    public String getAction() {
        return this.getString(NoticeData.ACTION);
    }
}
