/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;

/**
 * 下载文件。
 */
public class DownloadFile extends NoticeData {

    public DownloadFile(String url, String domain, long contactId) {
        super(FileStorageAction.DownloadFile.name);
        this.put(NoticeData.URL, url);
        this.put(NoticeData.DOMAIN, domain);
        this.put(NoticeData.CONTACT_ID, contactId);
    }

    public DownloadFile(String url, String domain, long contactId, int lengthLimit) {
        super(FileStorageAction.DownloadFile.name);
        this.put(NoticeData.URL, url);
        this.put(NoticeData.DOMAIN, domain);
        this.put(NoticeData.CONTACT_ID, contactId);
        this.put(NoticeData.LIMIT, lengthLimit);
    }
}
