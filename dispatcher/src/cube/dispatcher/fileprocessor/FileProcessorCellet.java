/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.fileprocessor;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.common.action.FileProcessorAction;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.fileprocessor.handler.GetMediaSourceHandler;
import cube.dispatcher.fileprocessor.handler.MediaStreamHandler;
import cube.dispatcher.fileprocessor.handler.SteganographicHandler;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 文件处理模块网关的 Cellet 服务单元。
 */
public class FileProcessorCellet extends AbstractCellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "FileProcessor";

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * 任务缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> taskQueue;

    public FileProcessorCellet() {
        super(NAME);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");

        // 配置 HTTP/HTTPS 服务的句柄
        HttpServer httpServer = this.performer.getHttpServer();

        // 媒体流处理句柄
        ContextHandler mediaListHandler = new ContextHandler();
        mediaListHandler.setContextPath(MediaStreamHandler.CONTEXT_PATH);
        mediaListHandler.setHandler(new MediaStreamHandler(this.performer));
        httpServer.addContextHandler(mediaListHandler);

        // 获取媒体源句柄
        ContextHandler getMediaSourceHandler = new ContextHandler();
        getMediaSourceHandler.setContextPath(GetMediaSourceHandler.CONTEXT_PATH);
        getMediaSourceHandler.setHandler(new GetMediaSourceHandler(this.performer));
        httpServer.addContextHandler(getMediaSourceHandler);

        // 隐写数据句柄
        ContextHandler steganoHandler = new ContextHandler();
        steganoHandler.setContextPath(SteganographicHandler.CONTEXT_PATH);
        steganoHandler.setHandler(new SteganographicHandler(this.performer));
        httpServer.addContextHandler(steganoHandler);

        MediaFileManager.getInstance().setPerformer(this.performer);

        // 校验工具
        MediaFileManager.getInstance().check();

        return true;
    }

    @Override
    public void uninstall() {
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = actionDialect.getName();

        if (FileProcessorAction.GetMediaSource.name.equals(action)) {
            this.performer.execute(new GetMediaSourceTask(this, talkContext, primitive, this.performer));
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
