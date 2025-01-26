/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

/**
 * 递归目录句柄。
 */
public interface RecurseDirectoryHandler {

    /**
     * 数据回调。
     *
     * @param directory
     * @return 返回 {@code false} 退出递归。
     */
    boolean handle(Directory directory);

}
