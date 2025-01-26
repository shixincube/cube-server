/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

import cube.common.JSONable;

/**
 * 文件操作接口。
 */
public interface FileOperation extends JSONable {

    /**
     * 获取操作对应的动作。
     *
     * @return
     */
    public String getProcessAction();
}
