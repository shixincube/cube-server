/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;

import java.util.List;

public interface Generatable {

    GeneratingRecord generateText(String unitName, String prompt, GeneratingOption option,
                                  List<GeneratingRecord> history);
}
