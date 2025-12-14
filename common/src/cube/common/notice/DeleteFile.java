/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;

/**
 * 删除文件。
 */
public class DeleteFile extends NoticeData {

    public DeleteFile(String domain, String fileCode) {
        super(FileStorageAction.DeleteFile.name);
        this.put(NoticeData.DOMAIN, domain);
        this.put(NoticeData.FILE_CODE, fileCode);
    }
}
