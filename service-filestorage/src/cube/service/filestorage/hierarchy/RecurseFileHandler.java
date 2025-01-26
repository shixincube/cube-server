/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

import cube.common.entity.FileLabel;

/**
 * 递归目录里的文件句柄。
 */
public interface RecurseFileHandler {

    /**
     * 数据回调。
     *
     * @param directory
     * @param fileLabel
     * @return 返回 {@code false} 退出递归。
     */
    boolean handle(Directory directory, FileLabel fileLabel);

}
