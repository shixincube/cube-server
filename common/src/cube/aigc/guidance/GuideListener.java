/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import cube.common.entity.GeneratingRecord;

public interface GuideListener {

    void onResponse(AbstractGuideFlow guideFlow, GeneratingRecord response);
}
