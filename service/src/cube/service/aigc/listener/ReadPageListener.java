/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.aigc.Page;

public interface ReadPageListener {

    void onCompleted(String url, Page page);
}
