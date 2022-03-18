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

import cell.util.log.Logger;
import cube.common.entity.ClientDescription;
import cube.common.entity.Message;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.hub.event.Event;
import cube.hub.EventBuilder;
import cube.hub.HubStateCode;
import cube.hub.Type;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/**
 * Hub 控制器。
 */
public class HubService extends AbstractModule {

    public final static String NAME = "Hub";

    private ExecutorService executor;

    private Queue<Message> messageQueue;
    private boolean queueProcessing;

    public HubService(ExecutorService executor) {
        this.executor = executor;
        this.messageQueue = new LinkedList<>();
        this.queueProcessing = false;
    }

    @Override
    public void start() {
        (new Thread() {
            @Override
            public void run() {
                setupMessagingPlugin();
            }
        }).start();
    }

    @Override
    public void stop() {

    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public void triggerEvent(JSONObject data, Responder responder) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                ClientDescription description = new ClientDescription(data.getJSONObject("client"));

                // 解析事件
                Event event = EventBuilder.build(data.getJSONObject("event"));

                EventController.getInstance().receive(event, description);

                responder.respond(HubStateCode.Ok.code, new JSONObject());
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
                                ClientDescription description = new ClientDescription(payload.getJSONObject("client"));
                                Event event = EventBuilder.build(payload.getJSONObject("data"));
                                EventController.getInstance().receive(event, description);
                            }
                            else if (Type.Signal.equals(type)) {
                                // TODO
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
