/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.service.multipointcomm.signaling.Signaling;

/**
 * 面向媒体单元会话的门户接口。
 */
public interface Portal {

    public void emit(CommFieldEndpoint endpoint, Signaling signaling);

    public CommField getCommField(Long commFieldId);

}
