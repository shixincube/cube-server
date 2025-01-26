/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;

/**
 * 获取文件。
 */
public class GetFile extends NoticeData {

    public GetFile(String domain, String fileCode) {
        super(FileStorageAction.GetFile.name);
        this.put(NoticeData.DOMAIN, domain);
        this.put(NoticeData.FILE_CODE, fileCode);
        this.put("transmitting", false);
    }

    public GetFile(String domain, String fileCode, boolean transmitting) {
        super(FileStorageAction.GetFile.name);
        this.put(NoticeData.DOMAIN, domain);
        this.put(NoticeData.FILE_CODE, fileCode);
        this.put("transmitting", transmitting);
    }
}
