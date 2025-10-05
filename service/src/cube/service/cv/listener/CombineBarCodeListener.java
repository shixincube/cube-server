/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv.listener;

import cube.common.entity.BarCode;
import cube.common.entity.FileLabel;
import cube.common.state.CVStateCode;

import java.util.List;

public interface CombineBarCodeListener {

    void onCompleted(List<BarCode> barCodeList, FileLabel fileLabel);

    void onFailed(List<BarCode> barCodeList, CVStateCode stateCode);
}
