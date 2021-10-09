/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
