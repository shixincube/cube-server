/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.state.MultipointCommStateCode;
import cube.service.multipointcomm.signaling.Signaling;

/**
 * 一般函数回调接口。
 */
public interface SignalingCallback {
    
    /**
     * 回调时被调用。
     *
     * @param stateCode
     * @param signaling
     */
    public void on(MultipointCommStateCode stateCode, Signaling signaling);
}
