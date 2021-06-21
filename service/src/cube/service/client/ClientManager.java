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
import cell.util.log.Logger;
import cube.core.Kernel;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactPluginContext;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 客户端管理器。
 */
public final class ClientManager {

    private final static ClientManager instance = new ClientManager();

    private Cellet cellet;

    /**
     * 存储所有在线的客户端。
     */
    private ConcurrentMap<Long, ServerClient> clientMap;

    private ConcurrentMap<Long, ServerClient> talkContextIndex;

    private ClientManager() {
        this.clientMap = new ConcurrentHashMap<>();
        this.talkContextIndex = new ConcurrentHashMap<>();
    }

    public static ClientManager getInstance() {
        return ClientManager.instance;
    }

    public void start(Cellet cellet, Kernel kernel) {
        this.cellet = cellet;

        (new Thread() {
            @Override
            public void run() {
                while (null == ContactManager.getInstance().getPluginSystem()) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignIn, new Plugin() {
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
                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignOut, new Plugin() {
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
                ContactManager.getInstance().getPluginSystem().register(ContactHook.DeviceTimeout, new Plugin() {
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
        }).start();
    }

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
     * 指定客户端申请监听事件。
     *
     * @param id
     * @param eventName
     * @param eventParam
     */
    public void listenEvent(Long id, String eventName, JSONObject eventParam) {
        ServerClient serverClient = this.clientMap.get(id);
        if (null == serverClient) {
            return;
        }

        if (null == eventParam) {
            serverClient.addEvent(eventName);
            return;
        }

        // 解析事件
        if (Events.ReceiveMessage.equals(eventName)) {

        }
    }

    private void onSignIn(PluginContext pluginContext) {
        ContactPluginContext context = (ContactPluginContext) pluginContext;

        for (ServerClient client : this.clientMap.values()) {
            if (client.hasEvent(ContactHook.SignIn)) {
                client.sendEvent(ContactHook.SignIn, context.toJSON());
            }
        }
    }

    private void onSignOut(PluginContext pluginContext) {
        ContactPluginContext context = (ContactPluginContext) pluginContext;

        for (ServerClient client : this.clientMap.values()) {
            if (client.hasEvent(ContactHook.SignOut)) {
                client.sendEvent(ContactHook.SignOut, context.toJSON());
            }
        }
    }

    private void onDeviceTimeout(PluginContext pluginContext) {
        ContactPluginContext context = (ContactPluginContext) pluginContext;

        for (ServerClient client : this.clientMap.values()) {
            if (client.hasEvent(ContactHook.DeviceTimeout)) {
                client.sendEvent(ContactHook.DeviceTimeout, context.toJSON());
            }
        }
    }
}
