/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.core.Kernel;

/**
 * Cellet 通信任务描述。
 */
public abstract class Task implements Runnable {

    /**
     * 框架的内核实例。
     */
    protected Kernel kernel;

    /**
     * 对应的 Cellet 实例。
     */
    protected Cellet cellet;

    /**
     * 当前任务对应的上下文。
     */
    protected TalkContext talkContext;

    /**
     * 当前接收到的原语数据。
     */
    protected Primitive primitive;

    /**
     * 构造函数。
     *
     * @param cellet 指定 Cellet 实例。
     * @param talkContext 指定会话上下文。
     * @param primitive 指定原语数据。
     */
    public Task(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        this.cellet = cellet;
        this.talkContext = talkContext;
        this.primitive = primitive;
        this.kernel = (Kernel) cellet.getNucleus().getParameter("kernel");
    }

    /**
     * 获取令牌码。
     *
     * @param actionDialect
     * @return 如果在动作方言中找不到令牌码返回 {@code null} 值。
     */
    public String getTokenCode(ActionDialect actionDialect) {
        if (actionDialect.containsParam("token")) {
            return actionDialect.getParamAsString("token");
        }

        return null;
    }

    @Override
    abstract public void run();
}
