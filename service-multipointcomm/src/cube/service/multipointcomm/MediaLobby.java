/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import java.util.Collection;

/**
 * 描述一个媒体通道上的所有关联对象及其数据。
 */
public interface MediaLobby {

    /**
     * 获取所有的会话。
     *
     * @return
     */
    public Collection<? extends MediaSession> getSessions();

}
