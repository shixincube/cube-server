/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.handler.*;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hub 模块的 Cellet 服务单元。
 */
public class HubCellet extends AbstractCellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "Hub";

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * 访问控制器。
     */
    private Controller controller;

    /**
     * 任务缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> taskQueue;

    /**
     * 守护任务定时器。
     */
    private Timer daemonTimer;

    public HubCellet() {
        super(HubCellet.NAME);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");

        this.controller = new Controller();

        setupHandler();

        this.daemonTimer = new Timer();
        this.daemonTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTick();
            }
        }, 5000, 10 * 60 * 1000);

        return true;
    }

    @Override
    public void uninstall() {
        if (null != this.daemonTimer) {
            this.daemonTimer.cancel();
            this.daemonTimer = null;
        }
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        this.performer.execute(this.borrowTask(talkContext, primitive, true));
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

    private void onTick() {
        CacheCenter.getInstance().selfChecking();
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        // 文件下载
        ContextHandler fileHandler = new ContextHandler();
        fileHandler.setContextPath(FileHandler.CONTEXT_PATH);
        fileHandler.setHandler(new FileHandler(this.performer, this.controller));
        httpServer.addContextHandler(fileHandler);

        // 打开管道
        ContextHandler openHandler = new ContextHandler();
        openHandler.setContextPath(OpenChannel.CONTEXT_PATH);
        openHandler.setHandler(new OpenChannel(this.performer, this.controller));
        httpServer.addContextHandler(openHandler);

        // 关闭管道
        ContextHandler closeHandler = new ContextHandler();
        closeHandler.setContextPath(CloseChannel.CONTEXT_PATH);
        closeHandler.setHandler(new CloseChannel(this.performer, this.controller));
        httpServer.addContextHandler(closeHandler);

        // 获取账号数据
        ContextHandler accountHandler = new ContextHandler();
        accountHandler.setContextPath(AccountHandler.CONTEXT_PATH);
        accountHandler.setHandler(new AccountHandler(this.performer, this.controller));
        httpServer.addContextHandler(accountHandler);

        // 获取会话数据
        ContextHandler convsHandler = new ContextHandler();
        convsHandler.setContextPath(ConversationsHandler.CONTEXT_PATH);
        convsHandler.setHandler(new ConversationsHandler(this.performer, this.controller));
        httpServer.addContextHandler(convsHandler);

        // 轮询消息数据
        ContextHandler rollPollingHandler = new ContextHandler();
        rollPollingHandler.setContextPath(RollPollingHandler.CONTEXT_PATH);
        rollPollingHandler.setHandler(new RollPollingHandler(this.performer, this.controller));
        httpServer.addContextHandler(rollPollingHandler);

        // 获取消息数据
        ContextHandler messagesHandler = new ContextHandler();
        messagesHandler.setContextPath(MessagesHandler.CONTEXT_PATH);
        messagesHandler.setHandler(new MessagesHandler(this.performer, this.controller));
        httpServer.addContextHandler(messagesHandler);

        // 获取通讯录数据
        ContextHandler bookHandler = new ContextHandler();
        bookHandler.setContextPath(ContactBookHandler.CONTEXT_PATH);
        bookHandler.setHandler(new ContactBookHandler(this.performer, this.controller));
        httpServer.addContextHandler(bookHandler);

        // 获取群组数据
        ContextHandler groupHandler = new ContextHandler();
        groupHandler.setContextPath(GroupHandler.CONTEXT_PATH);
        groupHandler.setHandler(new GroupHandler(this.performer, this.controller));
        httpServer.addContextHandler(groupHandler);

        // 发送消息操作
        ContextHandler sendMsgHandler = new ContextHandler();
        sendMsgHandler.setContextPath(SendMessageHandler.CONTEXT_PATH);
        sendMsgHandler.setHandler(new SendMessageHandler(this.performer, this.controller));
        httpServer.addContextHandler(sendMsgHandler);

        // 添加、删除朋友
        ContextHandler friendHandler = new ContextHandler();
        friendHandler.setContextPath(FriendHandler.CONTEXT_PATH);
        friendHandler.setHandler(new FriendHandler(this.performer, this.controller));
        httpServer.addContextHandler(friendHandler);
    }
}
