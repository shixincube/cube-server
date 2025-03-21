/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.hub;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.cache.SharedMemoryCache;
import cube.common.action.FileStorageAction;
import cube.common.entity.FileLabel;
import cube.common.entity.Message;
import cube.core.AbstractModule;
import cube.core.Cache;
import cube.core.Kernel;
import cube.core.Module;
import cube.hub.*;
import cube.hub.data.ChannelCode;
import cube.hub.event.*;
import cube.hub.signal.*;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
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

    private Path workPath;

    private SignalController signalController;
    private EventController eventController;

    private ChannelManager channelManager;

    private Queue<Message> messageQueue;
    private boolean queueProcessing;

    private Cache realTimeCache;

    public HubService(HubCellet cellet, ExecutorService executor) {
        this.cellet = cellet;
        this.executor = executor;
        this.messageQueue = new LinkedList<>();
        this.queueProcessing = false;

        this.workPath = Paths.get("storage/hub/");
        if (!Files.exists(this.workPath)) {
            try {
                Files.createDirectories(this.workPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void start() {
        JSONObject cacheConfig = new JSONObject();
        try {
            cacheConfig.put("configFile", "config/hub-cache.properties");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.realTimeCache = new SharedMemoryCache("HubCache");
        this.realTimeCache.configure(cacheConfig);

        this.signalController = new SignalController(this.cellet);
        this.eventController = new EventController(this, this.workPath);

        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(NAME)) {
            this.channelManager = new ChannelManager(config.getJSONObject(NAME));
        }
        else {
            this.channelManager = new ChannelManager(config.getJSONObject(AuthService.NAME));
        }

        (new Thread() {
            @Override
            public void run() {
                realTimeCache.start();

                setupMessagingPlugin();

                channelManager.start(executor);

                // 更新状态
                started.set(true);
            }
        }).start();

        WeChatHub.getInstance().setService(this);
    }

    @Override
    public void stop() {
        // 关闭信令控制器
        if (null != this.signalController) {
            this.signalController.dispose();
        }

        if (null != this.channelManager) {
            this.channelManager.stop();
        }

        if (null != this.realTimeCache) {
            this.realTimeCache.stop();
        }

        this.started.set(false);
    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
    }

    protected Cache getRealTimeCache() {
        return this.realTimeCache;
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

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public Path getWorkPath() {
        return this.workPath;
    }

    public void quit(TalkContext talkContext) {
        if (null == this.signalController) {
            return;
        }

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

                try {
                    // 捕获是否是阻塞事件
                    if (!signalController.capture(event)) {
                        eventController.receive(event);
                    }
                } catch (Exception e) {
                    Logger.e(HubService.class, "#triggerEvent", e);
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

    /**
     * 处理来自 API 层的请求。
     *
     * @param actionDialect
     * @param responder
     */
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
                        Logger.w(HubService.this.getClass(),
                                "#processChannel - channel code expired : " + channelCode.code);
                        responder.respondDispatcher(sn, HubStateCode.Expired.code, signal);
                        return;
                    }

                    if (Product.WeChat == channelCode.product) {
                        if (signal instanceof RollPollingSignal) {
                            RollPollingSignal rollPollingSignal = (RollPollingSignal) signal;
                            List<Message> messageList = WeChatHub.getInstance().queryCachedMessage(channelCode,
                                    rollPollingSignal.getConversationType(),
                                    rollPollingSignal.getConversationName(),
                                    rollPollingSignal.getLimit());
                            RollPollingEvent event = new RollPollingEvent(rollPollingSignal.getConversationType(),
                                    rollPollingSignal.getConversationName(),
                                    messageList);
                            responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                        }
                        else if (signal instanceof GetAccountSignal) {
                            AccountEvent event = WeChatHub.getInstance().getAccount(channelCode);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof GetConversationsSignal) {
                            // 获取最近会话
                            ConversationsEvent event = WeChatHub.getInstance().getRecentConversations(channelCode,
                                    (GetConversationsSignal) signal);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof GetContactZoneSignal) {
                            // 获取联系人分区
                            ContactZoneEvent event = WeChatHub.getInstance().getContactBook(channelCode,
                                    (GetContactZoneSignal) signal);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof GetMessagesSignal) {
                            // 获取消息
                            MessagesEvent event = WeChatHub.getInstance().getMessages(channelCode,
                                    (GetMessagesSignal) signal);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof GetGroupSignal) {
                            // 获取群组数据
                            GroupDataEvent event = WeChatHub.getInstance().getGroupData(channelCode,
                                    (GetGroupSignal) signal);
                            if (null != event) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof GetFileLabelSignal) {
                            // 获取文件标签
                            GetFileLabelSignal getFileLabelSignal = (GetFileLabelSignal) signal;
                            FileLabel fileLabel = getFileLabel(getFileLabelSignal.getFileCode());
                            if (null != fileLabel) {
                                FileLabelEvent event = new FileLabelEvent(sn, fileLabel);
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof SendMessageSignal) {
                            // 发送消息
                            SendMessageSignal sendMessageSignal = (SendMessageSignal) signal;
                            Event resultEvent = WeChatHub.getInstance().transportSignal(
                                    channelCode, sendMessageSignal);
                            if (null != resultEvent) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, resultEvent);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof AddFriendSignal) {
                            // 添加好友
                            AddFriendSignal addFriendSignal = (AddFriendSignal) signal;
                            Event resultEvent = WeChatHub.getInstance().transportSignal(
                                    channelCode, addFriendSignal);
                            if (null != resultEvent) {
                                responder.respondDispatcher(sn, HubStateCode.Ok.code, resultEvent);
                            }
                            else {
                                responder.respondDispatcher(sn, HubStateCode.Failure.code, signal);
                            }
                        }
                        else if (signal instanceof LoginQRCodeSignal) {
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

    public void processPutFile(ActionDialect actionDialect, Responder responder) {
        final long sn = actionDialect.getParamAsJson(this.performerKey).getLong("sn");
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject labelJson = actionDialect.getParamAsJson("fileLabel");

                JSONObject notification = new JSONObject();
                notification.put("action", FileStorageAction.PutFile.name);
                notification.put("fileLabel", labelJson);

                AbstractModule module = getKernel().getModule("FileStorage");
                Object result = module.notify(notification);
                FileLabel fileLabel = new FileLabel((JSONObject) result);
                FileLabelEvent event = new FileLabelEvent(sn, fileLabel);
                responder.respondDispatcher(sn, HubStateCode.Ok.code, event);
            }
        });
    }

    private FileLabel getFileLabel(String fileCode) {
        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.GetFile.name);
        notification.put("domain", AuthConsts.DEFAULT_DOMAIN);
        notification.put("fileCode", fileCode);
        notification.put("transmitting", false);

        AbstractModule module = this.getKernel().getModule("FileStorage");
        Object result = module.notify(notification);
        if (null == result) {
            Logger.w(this.getClass(), "#getFileLabel - file storage module error");
            return null;
        }

        FileLabel fileLabel = new FileLabel((JSONObject) result);
        return fileLabel;
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

        PluginSystem pluginSystem = null;

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
            public HookResult launch(PluginContext context) {
                onMessagingPush(context);
                return null;
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
