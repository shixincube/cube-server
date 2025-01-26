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
public class LoadFile extends NoticeData {

    public LoadFile(String domain, String fileCode) {
        super(FileStorageAction.LoadFile.name);
        this.put(NoticeData.DOMAIN, domain);
        this.put(NoticeData.FILE_CODE, fileCode);
    }
}
