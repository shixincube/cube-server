/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.util;

import java.io.File;

public interface FileDownloaderListener {

    void onProgress(String fileName, int progressLength, int totalLength);

    void onCompleted(File file);

    void onError(String error);
}
