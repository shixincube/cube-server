/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

/**
 * 联系人管理器监听器。
 */
public interface ContactManagerListener {

    /**
     * 联系人模块已启动回调。
     *
     * @param manager
     */
    void onStarted(ContactManager manager);

    /**
     * 联系人模块已关闭回调。
     *
     * @param manager
     */
    void onStopped(ContactManager manager);
}
