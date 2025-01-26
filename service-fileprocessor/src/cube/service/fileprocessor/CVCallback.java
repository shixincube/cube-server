/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cube.common.state.FileProcessorStateCode;

/**
 * 回调。
 */
public interface CVCallback {

    /**
     * 成功回调。
     * @param result
     */
    public void handleSuccess(CVResult result);

    /**
     * 失败回调。
     * @param result
     */
    public void handleFailure(FileProcessorStateCode stateCode, CVResult result);
}
