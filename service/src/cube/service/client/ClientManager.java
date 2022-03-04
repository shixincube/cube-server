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

package cube.service.client;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.entity.Client;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.file.OperationWorkflow;
import cube.file.OperationWork;
import cube.file.event.FileWorkflowEvent;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.client.event.MessageReceiveEvent;
import cube.service.client.event.MessageSendEvent;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactPluginContext;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端管理器。
 */
public final class ClientManager {

    private final static ClientManager instance = new ClientManager();

    private ClientCellet cellet;

    private ExecutorService executor;

    /**
     * 存储所有在线的客户端。
     */
    private ConcurrentMap<Long, ServerClient> clientMap;

    private ConcurrentMap<Long, ServerClient> talkContextIndex;

    private ClientStorage clientStorage;

    private ConcurrentLinkedQueue<FileWorkflowEvent> eventQueue;

    private AtomicBoolean sendingEvent;

    private ClientManager() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);
        this.clientMap = new ConcurrentHashMap<>();
        this.talkContextIndex = new ConcurrentHashMap<>();
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.sendingEvent = new AtomicBoolean(false);
    }

    public static ClientManager getInstance() {
        return ClientManager.instance;
    }

    /**
     * 启动客户端管理器。
     *
     * @param cellet
     * @param kernel
     */
    public void start(ClientCellet cellet, Kernel kernel) {
        this.cellet = cellet;

        // 使用授权模块的配置
        JSONObject config = ConfigUtils.readStorageConfig().getJSONObject(AuthService.NAME);
        if (config.getString("type").equalsIgnoreCase("SQLite")) {
            this.clientStorage = new ClientStorage(StorageType.SQLite, config);
        }
        else {
            this.clientStorage = new ClientStorage(StorageType.MySQL, config);
        }

        this.clientStorage.open();

        (new Thread() {
            @Override
            public void run() {
                // 自检数据表
                clientStorage.execSelfChecking(null);

                int count = 10;

                while (null == ContactManager.getInstance().getPluginSystem()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                setupContactPlugin(ContactManager.getInstance().getPluginSystem());

                // 配置消息模块
                count = 10;
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

                if (null != messagingModule) {
                    PluginSystem<?> pluginSystem = null;

                    while (null == (pluginSystem = messagingModule.getPluginSystem())) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    setupMessagingPlugin(pluginSystem);
                }

                // 配置文件处理模块
                count = 10;
                AbstractModule fileProcessor = null;
                while (null == (fileProcessor = kernel.getModule("FileProcessor"))) {
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

                if (null != fileProcessor) {
                    PluginSystem<?> pluginSystem = null;

                    while (null == (pluginSystem = fileProcessor.getPluginSystem())) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    setupFileProcessorPlugin(pluginSystem);
                }
            }
        }).start();
    }

    /**
     * 停止。
     */
    public void stop() {
        if (null != this.clientStorage) {
            this.clientStorage.close();
        }

        this.executor.shutdown();
    }

    private void setupContactPlugin(PluginSystem<?> pluginSystem) {
        pluginSystem.register(ContactHook.SignIn, new Plugin() {
            @Override
            public void onAction(PluginContext context) {
                onSignIn(context);
            }

            @Override
            public void setup() {
                // Nothing
            }
            @Override
            public void teardown() {
                // Nothing
            }
        });
        pluginSystem.register(ContactHook.SignOut, new Plugin() {
            @Override
            public void onAction(PluginContext context) {
                onSignOut(context);
            }

            @Override
            public void setup() {
                // Nothing
            }
            @Override
            public void teardown() {
                // Nothing
            }
        });
        pluginSystem.register(ContactHook.DeviceTimeout, new Plugin() {
            @Override
            public void onAction(PluginContext context) {
                onDeviceTimeout(context);
            }

            @Override
            public void setup() {
                // Nothing
            }
            @Override
            public void teardown() {
                // Nothing
            }
        });
    }

    private void setupMessagingPlugin(PluginSystem<?> pluginSystem) {
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

    private void setupFileProcessorPlugin(PluginSystem<?> pluginSystem) {
        pluginSystem.register("WorkflowStarted", new Plugin() {
            @Override
            public void setup() {}

            @Override
            public void teardown() {}

            @Override
            public void onAction(PluginContext context) {
                onFileProcessor("WorkflowStarted", context);
            }
        });

        pluginSystem.register("WorkflowStopped", new Plugin() {
            @Override
            public void setup() {}

            @Override
            public void teardown() {}

            @Override
            public void onAction(PluginContext context) {
                onFileProcessor("WorkflowStopped", context);
            }
        });

        pluginSystem.register("WorkBegun", new Plugin() {
            @Override
            public void setup() {}

            @Override
            public void teardown() {}

            @Override
            public void onAction(PluginContext context) {
                onFileProcessor("WorkBegun", context);
            }
        });

        pluginSystem.register("WorkEnded", new Plugin() {
            @Override
            public void setup() {}

            @Override
            public void teardown() {}

            @Override
            public void onAction(PluginContext context) {
                onFileProcessor("WorkEnded", context);
            }
        });
    }

    /**
     * 客户端登录。
     *
     * @param actionDialect
     * @param talkContext
     * @return 返回是否登录成功。
     */
    public boolean login(ActionDialect actionDialect, TalkContext talkContext) {
        Long id = actionDialect.getParamAsLong("id");
        String name = actionDialect.getParamAsString("name");
        String password = actionDialect.getParamAsString("password");

        Client desc = this.clientStorage.getClient(name, password);
        if (null == desc) {
            // 没有找到客户端
            return false;
        }

        ServerClient client = this.clientMap.get(id);
        if (null == client) {
            client = new ServerClient(id, this.cellet, talkContext, desc);
            this.clientMap.put(id, client);
        }
        else {
            client.resetTalkContext(talkContext);
        }

        this.talkContextIndex.put(talkContext.getSessionId(), client);
        return true;
    }

    /**
     * 客户端连接断开。
     *
     * @param talkContext
     */
    public void quit(TalkContext talkContext) {
        ServerClient client = this.talkContextIndex.remove(talkContext.getSessionId());
        if (null != client) {
            client.disable(new TimeoutCallback() {
                @Override
                public void on(ServerClient client) {
                    clientMap.remove(client.getId());
                    Logger.i(ClientManager.class, "Server client \"" + client.getId() + "\" timeout quit.");
                }
            });
        }
    }

    /**
     * 获取指定会话上下文对应的客户端。
     *
     * @param talkContext
     * @return
     */
    public ServerClient getClient(TalkContext talkContext) {
        return this.talkContextIndex.get(talkContext.getSessionId());
    }

    /**
     * 指定客户端添加事件监听器。
     *
     * @param clientId
     * @param eventName
     * @param eventParam
     */
    public void addEventListener(Long clientId, String eventName, JSONObject eventParam) {
        ServerClient serverClient = this.clientMap.get(clientId);
        if (null == serverClient) {
            return;
        }

        if (null == eventParam) {
            serverClient.addEvent(eventName);
            return;
        }

        // 解析事件
        if (Events.ReceiveMessage.equals(eventName)) {
            String domain = eventParam.getString("domain");
            if (eventParam.has("contactId")) {
                Long contactId = eventParam.getLong("contactId");

                Contact contact = ContactManager.getInstance().getContact(domain, contactId);
                MessageReceiveEvent event = new MessageReceiveEvent(contact);
                serverClient.addEvent(event);
            }
            else if (eventParam.has("groupId")) {
                Long groupId = eventParam.getLong("groupId");

                Group group = ContactManager.getInstance().getGroup(groupId, domain);
                if (null != group) {
                    MessageReceiveEvent event = new MessageReceiveEvent(group);
                    serverClient.addEvent(event);
                }
            }
        }
        else if (Events.SendMessage.equals(eventName)) {
            String domain = eventParam.getString("domain");
            if (eventParam.has("contactId")) {
                Long contactId = eventParam.getLong("contactId");

                Contact contact = ContactManager.getInstance().getContact(domain, contactId);
                MessageSendEvent event = new MessageSendEvent(contact);
                serverClient.addEvent(event);
            }
        }
    }

    /**
     * 指定客户端移除事件监听器。
     *
     * @param clientId
     * @param eventName
     * @param eventParam
     */
    public void removeEventListener(Long clientId, String eventName, JSONObject eventParam) {
        ServerClient serverClient = this.clientMap.get(clientId);
        if (null == serverClient) {
            return;
        }

        serverClient.removeEvent(eventName);

        if (Events.ReceiveMessage.equals(eventName)) {
            String domain = eventParam.getString("domain");
            if (eventParam.has("contactId")) {
                Long contactId = eventParam.getLong("contactId");

                Contact contact = ContactManager.getInstance().getContact(domain, contactId);
                MessageReceiveEvent event = new MessageReceiveEvent(contact);
                serverClient.removeEvent(event);
            }
            else if (eventParam.has("groupId")) {
                Long groupId = eventParam.getLong("groupId");

                Group group = ContactManager.getInstance().getGroup(groupId, domain);
                if (null != group) {
                    MessageReceiveEvent event = new MessageReceiveEvent(group);
                    serverClient.removeEvent(event);
                }
            }
        }
        else if (Events.SendMessage.equals(eventName)) {
            String domain = eventParam.getString("domain");
            if (eventParam.has("contactId")) {
                Long contactId = eventParam.getLong("contactId");

                Contact contact = ContactManager.getInstance().getContact(domain, contactId);
                MessageSendEvent event = new MessageSendEvent(contact);
                serverClient.removeEvent(event);
            }
        }
    }

    private void onSignIn(PluginContext pluginContext) {
        final ContactPluginContext context = (ContactPluginContext) pluginContext;

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                for (ServerClient client : clientMap.values()) {
                    if (client.hasEvent(ContactHook.SignIn)) {
                        client.sendEvent(ContactHook.SignIn, context.toJSON());
                    }
                }
            }
        });
    }

    private void onSignOut(PluginContext pluginContext) {
        final ContactPluginContext context = (ContactPluginContext) pluginContext;

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                for (ServerClient client : clientMap.values()) {
                    if (client.hasEvent(ContactHook.SignOut)) {
                        client.sendEvent(ContactHook.SignOut, context.toJSON());
                    }
                }
            }
        });
    }

    private void onDeviceTimeout(PluginContext pluginContext) {
        final ContactPluginContext context = (ContactPluginContext) pluginContext;

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                for (ServerClient client : clientMap.values()) {
                    if (client.hasEvent(ContactHook.DeviceTimeout)) {
                        client.sendEvent(ContactHook.DeviceTimeout, context.toJSON());
                    }
                }
            }
        });
    }

    private void onMessagingPush(PluginContext pluginContext) {
        final Message message = (Message) pluginContext.get("message");

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                for (ServerClient client : clientMap.values()) {
                    synchronized (client) {
                        if (client.hasEvent(MessageReceiveEvent.NAME)) {
                            // 查找对应的事件
                            MessageReceiveEvent event = client.queryReceiveEvent(message);
                            if (null != event) {
                                client.sendEvent(MessageReceiveEvent.NAME, event.toJSON());
                            }
                        }

                        if (client.hasEvent(MessageSendEvent.NAME)) {
                            // 查找对应的事件
                            MessageSendEvent event = client.querySendEvent(message);
                            if (null != event) {
                                client.sendEvent(MessageSendEvent.NAME, event.toJSON());
                            }
                        }
                    }
                }
            }
        });
    }

    private synchronized void onFileProcessor(String eventName, PluginContext pluginContext) {
        OperationWorkflow workflow = (OperationWorkflow) pluginContext.get("workflow");

        long clientId = workflow.getClientId();
        if (clientId == 0) {
            // 不是来自服务器客户端的任务
            return;
        }

        // OperationWork 可以是 null 值
        OperationWork work = (OperationWork) pluginContext.get("work");

        // 创建事件
        FileWorkflowEvent event = new FileWorkflowEvent(eventName, workflow, work);
        event.resultFile = (File) pluginContext.get("resultFile");

        // 事件进入队列
        this.eventQueue.offer(event);

        if (this.sendingEvent.get()) {
            return;
        }

        this.sendingEvent.set(true);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                while (!eventQueue.isEmpty()) {
                    FileWorkflowEvent workflowEvent = eventQueue.poll();
                    if (null != workflowEvent) {
                        String eventName = workflowEvent.getName();
                        long clientId = workflowEvent.getWorkflow().getClientId();
                        for (ServerClient client : clientMap.values()) {
                            if (client.getId().longValue() == clientId) {
                                synchronized (client) {
                                    if (eventName.equals("WorkflowStopped")) {
                                        File resultFile = workflowEvent.resultFile;
                                        if (null != resultFile && resultFile.exists()) {
                                            // 传输文件数据
                                            client.transmitStream(resultFile.getName(), resultFile);
                                            // 删除结果文件
                                            resultFile.delete();
                                        }
                                    }

                                    client.sendEvent(workflowEvent.getName(), workflowEvent.toJSON());
                                }
                            }
                        }
                    }
                }

                sendingEvent.set(false);
            }
        });
    }
}
