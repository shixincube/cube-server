/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.dispatcher.hub;

import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.util.CachedQueueExecutor;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.handler.FileHandler;
import cube.dispatcher.hub.handler.OpenChannel;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * Hub 模块的 Cellet 服务单元。
 */
public class HubCellet extends AbstractCellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "Hub";

    /**
     * 线程池。
     */
    private ExecutorService executor;

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * 任务缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> taskQueue;

    public HubCellet() {
        super(HubCellet.NAME);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);
        this.performer = (Performer) this.getNucleus().getParameter("performer");

        setupHandler();

        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        this.executor.execute(this.borrowTask(talkContext, primitive, true));
    }

    @Override
    public void onListened(TalkContext talkContext, PrimitiveInputStream primitiveStream) {
        super.onListened(talkContext, primitiveStream);
    }

    protected PassThroughTask borrowTask(TalkContext talkContext, Primitive primitive, boolean sync) {
        PassThroughTask task = this.taskQueue.poll();
        if (null == task) {
            task = new PassThroughTask(this, talkContext, primitive, this.performer, sync);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive, sync);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnTask(PassThroughTask task) {
        task.markResponseTime();

        this.taskQueue.offer(task);
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        // 文件下载
        ContextHandler fileHandler = new ContextHandler();
        fileHandler.setContextPath(FileHandler.CONTEXT_PATH);
        fileHandler.setHandler(new FileHandler(this.performer));
        httpServer.addContextHandler(fileHandler);

        // 打开管道
        ContextHandler openHandler = new ContextHandler();
        openHandler.setContextPath(OpenChannel.CONTEXT_PATH);
        openHandler.setHandler(new OpenChannel(this.performer));
        httpServer.addContextHandler(openHandler);
    }
}