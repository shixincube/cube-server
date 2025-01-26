/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;

/**
 * 媒体单元处理回调。
 */
public interface MediaUnitCallback {

    public void on(CommField commField, CommFieldEndpoint endpoint);
}
