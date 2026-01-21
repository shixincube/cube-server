/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv.listener;

import cube.common.entity.FileLabel;
import cube.common.entity.Material;
import cube.common.state.CVStateCode;

import java.util.List;

public interface MatchSimilarityListener {

    void onCompleted(FileLabel fileLabel, List<String> templateNames, List<Material> materials);

    void onFailed(FileLabel fileLabel, CVStateCode stateCode);
}
