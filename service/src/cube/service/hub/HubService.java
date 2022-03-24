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

package cube.service.hub;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.entity.Message;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.hub.*;
import cube.hub.dao.ChannelCode;
import cube.hub.event.Event;
import cube.hub.signal.*;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * Hub 控制器。
 */
public class HubService extends AbstractModule {

    public final static String NAME = "Hub";

    private final String performerKey = "_performer";

    private HubCellet cellet;

    private ExecutorService executor;

    private SignalController signalController;
    private EventController eventController;

    private ChannelManager channelManager;

    private Queue<Message> messageQueue;
    private boolean queueProcessing;

    public HubService(HubCellet cellet, ExecutorService executor) {
        this.cellet = cellet;
        this.executor = executor;
        this.messageQueue = new LinkedList<>();
        this.queueProcessing = false;
    }

    @Override
    public void start() {
        this.signalController = new SignalController(this.cellet);
        this.eventController = new EventController();

        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(NAME)) {
            this.channelManager = new ChannelManager(config.getJSONObject(NAME));
        }

        (new Thread() {
            @Override
            public void run() {
                setupMessagingPlugin();

                channelManager.start();
            }
        }).start();

        WeChatHub.getInstance().setService(this);
    }

    @Override
    public void stop() {
        if (null != this.channelManager) {
            this.channelManager.stop();
        }
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
    }

    public EventController getEventController() {
        return this.eventController;
    }

    public SignalController getSignalController() {
        return this.signalController;
    }

    public ChannelManager getChannelManager() {
        return this.channelManager;
    }

    public void quit(TalkContext talkContext) {
        this.signalController.removeClient(talkContext);
    }

    public void triggerEvent(JSONObject data, Responder responder) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 解析事件
                Event event = EventBuilder.build(data);
                if (null == event) {
                    Logger.w(HubService.class, "Build event failed");
                    responder.respond(HubStateCode.UnsupportedEvent.code, new JSONObject());
                    return;
                }

                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#triggerEvent - " + event.getName()
                            + " (" + event.getSerialNumber() + ")");
                }

                // 捕获是否是阻塞事件
                if (!signalController.capture(event)) {
                    eventController.receive(event);
                }

                responder.respond(HubStateCode.Ok.code, new JSONObject());
            }
        });
    }

    public void transmitSignal(JSONObject data, Responder responder) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 解析信令
                Signal signal = SignalBuilder.build(data);

                if (ReadySignal.NAME.equals(signal.getName())) {
                    ((ReadySignal)signal).talkContext = responder.getTalkContext();
                }

                Signal ack = signalController.receive(signal);

                responder.respond(HubStateCode.Ok.code, ack.toJSON());
            }
        });
    }

    public void processChannel(ActionDialect actionDialect, Responder responder) {
        final long sn = actionDialect.getParamAsJson(this.performerKey).getLong("sn");
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                if (actionDialect.containsParam("signal")) {
                    JSONObject signalJson = actionDialect.getParamAsJson("signal");
                    // 解析信令
                    Signal signal = SignalBuilder.build(signalJson);
                    if (null == signal) {
                        responder.respondDispatcher(sn, HubStateCode.UnsupportedSignal.code, new AckSignal());
                        return;
                    }

                    ChannelCode channelCode = channelManager.getChannelCode(signal.getCode());

                    // 判断管道码信令
                    if (ChannelCodeSignal.NAME.equals(signal.getName())) {
                        if (null == channelCode) {
                            responder.respondDispatcher(sn, HubStateCode.Unauthorized.code, signal);
                        }
                        else {
                            ChannelCodeSignal responseSignal = new ChannelCodeSignal(channelCode);
                            responder.respondDispatcher(sn, HubStateCode.Ok.code, responseSignal);
                        }
                        return;
                    }

                    // 校验令牌
                    if (null == channelCode) {
                        responder.respondDispatcher(sn, HubStateCode.Unauthorized.code, signal);
                        return;
                    }

                    if (System.currentTimeMillis() >= channelCode.expiration) {
                        // 过期
                        Logger.w(HubService.this.getClass(), "#processChannel - channel code expired : " + channelCode.code);
                        responder.respondDispatcher(sn, HubStateCode.Expired.code, signal);
                        return;
                    }

                    if (Product.WeChat == channelCode.product) {
                        if (signal instanceof LoginQRCodeSignal) {
                            // 获取登录二维码
                            Event event = WeChatHub.getInstance().openChannel(channelCode);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof LogoutSignal) {
                            // 退出登录
                            Event event = WeChatHub.getInstance().closeChannel(channelCode);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else {
                            responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                        }
                    }
                    else {
                        responder.respondDispatcher(sn, HubStateCode.InvalidParameter.code, signal);
                    }
                }
                else {
                    Logger.e(HubService.this.getClass(), "Unknown channel request");
                }
            }
        });
    }

    private void setupMessagingPlugin() {
        Kernel kernel = this.getKernel();
        int count = 10;
        AbstractModule messagingModule = null;
        while (null == (messagingModule = kernel.getModule("Messaging"))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            --count;
            if (count == 0) {
                break;
            }
        }

        if (null == messagingModule) {
            Logger.e(this.getClass(), "#setupMessagingPlugin - Not find messaging service");
            return;
        }

        PluginSystem<?> pluginSystem = null;

        while (null == (pluginSystem = messagingModule.getPluginSystem())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        pluginSystem.register("PostPush", new Plugin() {
            @Override
            public void setup() {
                // Nothing
            }
            @Override
            public void teardown() {
                // Nothing
            }

            @Override
            public void onAction(PluginContext context) {
                onMessagingPush(context);
            }
        });
    }

    private void onMessagingPush(PluginContext pluginContext) {
        Message message = (Message) pluginContext.get("message");
        JSONObject payload = message.getPayload();
        if (payload.has("type")) {
            // 判断消息类型数据是否需要进行处理
            String type = payload.getString("type");
            // 处理 Hub 相关的消息
            if (Type.Event.equals(type) || Type.Signal.equals(type)) {
                synchronized (this.messageQueue) {
                    this.messageQueue.offer(message);

                    if (this.queueProcessing) {
                        return;
                    }

                    this.queueProcessing = true;
                }

                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        while (!messageQueue.isEmpty()) {
                            Message message = null;
                            synchronized (messageQueue) {
                                message = messageQueue.poll();
                            }
                            if (null == message) {
                                break;
                            }

                            JSONObject payload = message.getPayload();
                            String type = payload.getString("type");
                            if (Type.Event.equals(type)) {
                                Event event = EventBuilder.build(payload.getJSONObject("event"));
                                eventController.receive(event);
                            }
                            else if (Type.Signal.equals(type)) {
                                Signal signal = SignalBuilder.build(payload.getJSONObject("signal"));
                                signalController.receive(signal);
                            }
                        }

                        synchronized (messageQueue) {
                            queueProcessing = false;
                        }
                    }
                });
            }
        }
    }


}
