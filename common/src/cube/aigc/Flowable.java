/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.entity.GeneratingRecord;

public interface Flowable {

    GeneratingRecord generate();
}
