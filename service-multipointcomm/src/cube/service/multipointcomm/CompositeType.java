/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

/**
 * 混合流类型。
 */
public enum CompositeType {

    /**
     * 仅 Audio 通道。
     */
    Audio,

    /**
     * 仅 Video 通道。
     */
    Video,

    /**
     * 两个通道同时。
     */
    Both
}
