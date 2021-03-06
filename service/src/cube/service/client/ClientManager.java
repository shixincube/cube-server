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

package cube.service.client;

import cell.core.cellet.Cellet;
import cell.core.talk.TalkContext;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import cube.service.client.event.MessageReceiveEvent;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactPluginContext;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * 客户端管理器。
 */
public final class ClientManager {

    private final static ClientManager instance = new ClientManager();

    private Cellet cellet;

    private ExecutorService executor;

    /**
     * 存储所有在线的客户端。
     */
    private ConcurrentMap<Long, ServerClient> clientMap;

    private ConcurrentMap<Long, ServerClient> talkContextIndex;

    private ClientManager() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);
        this.clientMap = new ConcurrentHashMap<>();
        this.talkContextIndex = new ConcurrentHashMap<>();
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
    public void start(Cellet cellet, Kernel kernel) {
        this.cellet = cellet;

        (new Thread() {
            @Override
            public void run() {
                int count = 10;

                while (null == ContactManager.getInstance().getPluginSystem()) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                setupContactPlugin(ContactManager.getInstance().getPluginSystem());

                // 检查消息模块
                count = 10;
                AbstractModule messagingModule = null;
                while (null == (messagingModule = kernel.getModule("Messaging"))) {
                    try {
                        Thread.sleep(1000L);
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
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    setupMessagingPlugin(pluginSystem);
                }
            }
        }).start();
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

    /**
     *
     * @param id
     * @param talkContext
     */
    public void login(Long id, TalkContext talkContext) {
        ServerClient client = this.clientMap.get(id);
        if (null == client) {
            client = new ServerClient(id, this.cellet, talkContext);
            this.clientMap.put(id, client);
        }
        else {
            client.resetTalkContext(talkContext);
        }

        this.talkContextIndex.put(talkContext.getSessionId(), client);
    }

    /**
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
                    }
                }
            }
        });
    }
}
