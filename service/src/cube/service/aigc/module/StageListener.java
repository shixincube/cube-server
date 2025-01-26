/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import java.util.List;

public interface StageListener {

    void onPerform(Stage stage, Module module, List<String> answerList);
}
