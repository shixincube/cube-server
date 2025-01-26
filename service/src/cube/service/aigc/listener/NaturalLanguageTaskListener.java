/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.NLTask;

/**
 * 自然语言处理任务监听器。
 * @deprecated 2024-12-13
 */
public interface NaturalLanguageTaskListener {

    void onCompleted(NLTask nlTask);

    void onFailed();
}
