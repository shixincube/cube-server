/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

/**
 * 业务模块装载异常。
 *
 * <p>由 {@link ActionModule#setup(AIGCHost)} 抛出，宿主据此按
 * {@link ModuleDescriptor#optional} 决定「降级为未就绪」还是「拒绝加载」。</p>
 *
 * <p>本异常为受检异常，以强制宿主显式捕获并落实装载失败策略，
 * 避免模块作者误以为宿主会兜住失败。</p>
 */
public class ModuleException extends Exception {

    private final static long serialVersionUID = 1L;

    /**
     * 构造函数。
     *
     * @param message 异常描述。
     */
    public ModuleException(String message) {
        super(message);
    }

    /**
     * 构造函数。
     *
     * @param message 异常描述。
     * @param cause 引发本异常的原始异常。
     */
    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
