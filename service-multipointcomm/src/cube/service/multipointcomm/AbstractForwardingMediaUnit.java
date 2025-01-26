/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;

/**
 * 抽象的媒体单元。
 */
public abstract class AbstractForwardingMediaUnit implements MediaUnit {

    public AbstractForwardingMediaUnit() {
    }

    /**
     * 准备数据通道。
     *
     * @param commField
     * @param endpoint
     */
    public abstract void preparePipeline(CommField commField, CommFieldEndpoint endpoint);

    /**
     * {@inheritDoc}
     */
    public void onTick(long now) {
        // Nothing
    }
}
