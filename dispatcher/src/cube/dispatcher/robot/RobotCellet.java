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

package cube.dispatcher.robot;

import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.robot.handler.*;
import cube.dispatcher.util.Tickable;
import cube.util.HttpServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Robot Cellet 服务单元。
 */
public class RobotCellet extends AbstractCellet implements Tickable {

    public final static String NAME = "Robot";

    protected static String ROBOT_API_URL = "http://127.0.0.1:2280/event/callback/AfKbNrmDvJQWxkMFNlYQfFHVrRZkbjNi";

    protected static String ROBOT_CALLBACK_URL = "http://127.0.0.1:7010/robot/event/kLBNGSmrTbmNlBfTYcIaKqYQiDPKoTkE";

    /**
     * 执行机。
     */
    private Performer performer;

    private boolean enabled = false;

    private boolean registered = false;

    private int tickCount = 0;

    public RobotCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");
        this.performer.addTickable(this);

        // 配置
        if (this.performer.getProperties().containsKey("robot.enabled")) {
            this.enabled = Boolean.parseBoolean(
                    this.performer.getProperties().getProperty("robot.enabled", "false"));
        }
        if (this.performer.getProperties().containsKey("robot.api")) {
            ROBOT_API_URL = this.performer.getProperties().getProperty("robot.api").trim();
        }
        if (this.performer.getProperties().containsKey("robot.callback")) {
            ROBOT_CALLBACK_URL = this.performer.getProperties().getProperty("robot.callback").trim();
        }

        if (this.enabled) {
            Manager.getInstance().start();

            setupHandler();

            registerCallback();
        }
        else {
            Logger.w(this.getClass(), "Robot service is NOT enabled");
        }

        return true;
    }

    @Override
    public void uninstall() {
        this.performer.removeTickable(this);

        if (this.enabled) {
            Manager.getInstance().stop();

            deregisterCallback();
        }
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        ContextHandler eventHandler = new ContextHandler();
        eventHandler.setContextPath(RoboengineEventHandler.CONTEXT_PATH);
        eventHandler.setHandler(new RoboengineEventHandler(this.performer));
        httpServer.addContextHandler(eventHandler);

        httpServer.addContextHandler(new RegisterCallback(this.performer));
        httpServer.addContextHandler(new DeregisterCallback(this.performer));
        httpServer.addContextHandler(new GetOnlineList(this.performer));
        httpServer.addContextHandler(new GetAccountList(this.performer));
        httpServer.addContextHandler(new GetAccount(this.performer));
        httpServer.addContextHandler(new Perform(this.performer));
        httpServer.addContextHandler(new Cancel(this.performer));
    }

    private void registerCallback() {
        HttpClient client = new HttpClient();

        try {
            client.start();

            JSONObject data = new JSONObject();
            data.put("url", ROBOT_CALLBACK_URL);

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(ROBOT_API_URL).content(provider).send();
            if (response.getStatus() != HttpStatus.OK_200) {
                this.registered = false;
                Logger.w(this.getClass(), "#registerCallback - register callback URL failed: " + response.getStatus());
            }
            else {
                this.registered = true;
                Logger.i(this.getClass(), "#registerCallback - register callback: " + ROBOT_CALLBACK_URL);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#registerCallback", e);
        } finally {
            if (client.isStarted()) {
                try {
                    client.stop();
                } catch (Exception e) {
                }
            }
        }
    }

    private void deregisterCallback() {
        HttpClient client = new HttpClient();

        try {
            client.start();

            JSONObject data = new JSONObject();
            data.put("url", ROBOT_CALLBACK_URL);
            data.put("deregister", true);

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(ROBOT_API_URL).content(provider).send();
            if (response.getStatus() != HttpStatus.OK_200) {
                Logger.w(this.getClass(), "#deregisterCallback - deregister callback URL failed: " + response.getStatus());
            }
            else {
                Logger.i(this.getClass(), "#deregisterCallback - deregister callback: " + ROBOT_CALLBACK_URL);
            }

            this.registered = false;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#deregisterCallback", e);
        } finally {
            if (client.isStarted()) {
                try {
                    client.stop();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onTick(long now) {
        if (!this.enabled) {
            return;
        }

        ++this.tickCount;
        if (this.tickCount >= Integer.MAX_VALUE) {
            this.tickCount = 1;
        }

        if (this.tickCount % 3 == 0) {
            // 每30秒执行一次
            if (!this.registered) {
                this.registerCallback();
            }
        }

        if (this.tickCount % 6 == 0) {
            // 检查 Roboengine 服务是否可用
            checkRoboengine();
        }
    }

    private boolean checkRoboengine() {
        HttpClient client = new HttpClient();

        try {
            client.start();

            JSONObject data = new JSONObject();

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(ROBOT_API_URL).content(provider).send();
            Logger.d(this.getClass(), "#checkRoboengine - " + response.getStatus());
        } catch (ExecutionException e) {
            this.registered = false;
            Logger.w(this.getClass(), "#checkRoboengine - Roboengine unavailable", e);
            return false;
        } catch (TimeoutException e) {
            this.registered = false;
            Logger.w(this.getClass(), "#checkRoboengine - Roboengine unavailable", e);
            return false;
        } catch (InterruptedException e) {
            this.registered = false;
            Logger.w(this.getClass(), "#checkRoboengine - Roboengine unavailable", e);
            return false;
        } catch (Exception e) {
            this.registered = false;
            Logger.w(this.getClass(), "#checkRoboengine - Roboengine unavailable", e);
            return false;
        } finally {
            if (client.isStarted()) {
                try {
                    client.stop();
                } catch (Exception e) {
                }
            }
        }

        return true;
    }
}
