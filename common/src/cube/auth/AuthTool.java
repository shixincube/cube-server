/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.auth;

/**
 * 授权管理工具。
 */
public class AuthTool {

    /**
     * 工作路劲。
     */
    private String workingPath;

    /**
     * 构造函数。
     *
     * @param workingPath 指定工作路径。
     */
    public AuthTool(String workingPath) {
        this.workingPath = workingPath;
    }
}
