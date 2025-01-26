/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;

/**
 * 媒体单元监听器。
 */
public interface MediaUnitListener {

    /**
     *
     * @param field
     */
    public void onPipelineCreated(CommField field);

    /**
     *
     * @param field
     */
    public void onPipelineReleased(CommField field);
}
