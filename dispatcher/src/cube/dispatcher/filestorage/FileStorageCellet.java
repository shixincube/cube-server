/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.dispatcher.filestorage;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cube.common.action.FileStorageAction;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 文件存储模块的 Cellet 单元。
 */
public class FileStorageCellet extends AbstractCellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "FileStorage";

    protected static String APP_LOGIN_URL = "https://127.0.0.1:8080/index.html";

    /**
     * 文件块存储。
     */
    private FileChunkStorage fileChunkStorage;

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * 任务缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> taskQueue;

    public FileStorageCellet() {
        super(NAME);
        this.fileChunkStorage = new FileChunkStorage("cube-fs-files");
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.performer = (Performer) nucleus.getParameter("performer");

        // 配置 App
        if (this.performer.getProperties().containsKey("app.login")) {
            APP_LOGIN_URL = this.performer.getProperties().getProperty("app.login").trim();
        }

        // 打开存储管理器
        this.fileChunkStorage.open(this, this.performer);

        // 配置 HTTP/HTTPS 服务的句柄
        HttpServer httpServer = this.performer.getHttpServer();

        // 添加句柄
        ContextHandler fileHandler = new ContextHandler();
        fileHandler.setContextPath(FileHandler.PATH);
        fileHandler.setHandler(new FileHandler(this.fileChunkStorage, this.performer));
        httpServer.addContextHandler(fileHandler);

        ContextHandler operationHandler = new ContextHandler();
        operationHandler.setContextPath(FileOperationHandler.PATH);
        operationHandler.setHandler(new FileOperationHandler(this.performer));
        httpServer.addContextHandler(operationHandler);

        ContextHandler sharingHandler = new ContextHandler();
        sharingHandler.setContextPath(FileSharingHandler.PATH);
        sharingHandler.setHandler(new FileSharingHandler(this.performer));
        httpServer.addContextHandler(sharingHandler);

        return true;
    }

    @Override
    public void uninstall() {
        this.fileChunkStorage.close();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        String action = primitive.getStuff(0).getValueAsString();
        if (FileStorageAction.CreateSharingTag.name.equals(action)) {
            MarathonTask task = new MarathonTask(this, talkContext, primitive, this.performer);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            if (!task.start()) {
                // 应答系统忙
                task.responseBusy();
            }
        }
        else {
            this.performer.execute(this.borrowTask(talkContext, primitive, true));
        }
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
}
