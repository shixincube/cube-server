/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.cv;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.cv.handler.*;
import cube.util.HttpServer;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务单元。
 */
public class CVCellet extends AbstractCellet {

    public final static String NAME = "CV";

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> taskQueue;

    public CVCellet() {
        super(NAME);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");

        HttpServer httpServer = this.performer.getHttpServer();
        httpServer.addContextHandler(new MakeBarCode(this.performer));
        httpServer.addContextHandler(new DetectBarCode(this.performer));
        httpServer.addContextHandler(new ObjectDetection(this.performer));
        httpServer.addContextHandler(new PoseEstimation(this.performer));
        httpServer.addContextHandler(new HandEstimation(this.performer));
        httpServer.addContextHandler(new ClipPaper(this.performer));

        return true;
    }

    @Override
    public void uninstall() {
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        this.performer.execute(this.borrowTask(talkContext, primitive));
    }

    protected PassThroughTask borrowTask(TalkContext talkContext, Primitive primitive) {
        PassThroughTask task = this.taskQueue.poll();
        if (null == task) {
            task = new PassThroughTask(this, talkContext, primitive, this.performer);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnTask(PassThroughTask task) {
        task.markResponseTime();

        this.taskQueue.offer(task);
    }
}
