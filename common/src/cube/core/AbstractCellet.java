/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.core;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cube.benchmark.ResponseTime;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象的服务 Cellet 单元。
 */
public abstract class AbstractCellet extends Cellet {

    protected AtomicLong listenedCounter = new AtomicLong(0L);

    protected ConcurrentLinkedQueue<ResponseTime> responseTimeList = new ConcurrentLinkedQueue<>();

    protected int maxListLength = 50;

    public AbstractCellet(String name) {
        super(name);
    }

    /**
     * 获取数据接收计数器。
     *
     * @return 返回数据接收计数器。
     */
    public AtomicLong getListenedCounter() {
        return this.listenedCounter;
    }

    /**
     * 获取应答时间记录。
     *
     * @return 返回应答时间记录。
     */
    public Queue<ResponseTime> getResponseTimeList() {
        return this.responseTimeList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        this.listenedCounter.incrementAndGet();
    }

    protected ResponseTime markResponseTime() {
        ResponseTime time = new ResponseTime();
        time.beginning = System.currentTimeMillis();

        this.responseTimeList.add(time);
        if (this.responseTimeList.size() > this.maxListLength) {
            this.responseTimeList.poll();
        }

        return time;
    }
}
