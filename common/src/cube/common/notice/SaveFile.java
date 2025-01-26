/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.notice;

import cube.common.action.FileStorageAction;
import cube.common.entity.FileLabel;

/**
 * 保存文件。
 */
public class SaveFile extends NoticeData {

    public SaveFile(String path, FileLabel fileLabel) {
        super(FileStorageAction.SaveFile.name);
        this.put(NoticeData.PATH, path);
        this.put(NoticeData.FILE_LABEL, fileLabel.toJSON());
    }
}
