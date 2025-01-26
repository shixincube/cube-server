/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

import java.util.List;

/**
 * 具备存储能力的接口。
 */
public interface Storagable {

    /**
     * 开启存储。
     */
    void open();

    /**
     * 关闭存储。
     */
    void close();

    /**
     * 执行自检。
     *
     * @param domainNameList 存储需要支持的域的域名称列表。
     */
    void execSelfChecking(List<String> domainNameList);
}
