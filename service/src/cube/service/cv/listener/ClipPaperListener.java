/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv.listener;

import cube.common.entity.ProcessedFile;
import cube.common.state.CVStateCode;

import java.util.List;

public interface ClipPaperListener {

    void onCompleted(List<ProcessedFile> processedFiles, long elapsed);

    void onFailed(List<String> fileCodes, CVStateCode stateCode);
}
