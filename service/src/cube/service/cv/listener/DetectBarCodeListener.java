/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv.listener;

import cube.common.entity.BarCodeInfo;
import cube.common.state.CVStateCode;

import java.util.List;

public interface DetectBarCodeListener {

    void onCompleted(List<BarCodeInfo> barCodeInfos);

    void onFailed(List<String> fileCodes, CVStateCode stateCode);
}
