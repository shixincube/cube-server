/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.filestorage;

import cube.common.entity.FileLabel;

/**
 * 文件块事件监听器。
 */
public interface FileChunkEventListener {

    void onCompleted(FileLabel fileLabel);

    void onFailed(String fileCode);
}
