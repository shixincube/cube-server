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

package cube.service.ferry;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.entity.AuthDomain;
import cube.common.entity.Contact;
import cube.common.entity.IceServer;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.ferry.*;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.ferry.plugin.WriteMessagePlugin;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 摆渡数据服务。
 */
public class FerryService extends AbstractModule {

    public final static String NAME = "Ferry";

    private final FerryCellet cellet;

    private FerryStorage storage;

    private Timer timer;

    private FerryAdapter adapter;

    private Map<String, Ticket> tickets;

    private Queue<FerryPacket> pushQueue;

    private Object pushMutex = new Object();
    private boolean pushing = false;

    public FerryService(FerryCellet cellet) {
        super();
        this.cellet = cellet;
        this.tickets = new ConcurrentHashMap<>();
        this.pushQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void start() {
        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(FerryService.NAME)) {
            config = config.getJSONObject(FerryService.NAME);
            if (config.getString("type").equalsIgnoreCase("SQLite")) {
                this.storage = new FerryStorage(StorageType.SQLite, config);
            }
            else {
                this.storage = new FerryStorage(StorageType.MySQL, config);
            }
        }
        else {
            config.put("file", "storage/Ferry.db");
            this.storage = new FerryStorage(StorageType.SQLite, config);
        }

        (new Thread() {
            @Override
            public void run() {
                storage.open();
                storage.execSelfChecking(null);
            }
        }).start();

        if (null == this.timer) {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // 配置
                    setup();

                    (new Thread() {
                        @Override
                        public void run() {
                            timer.cancel();
                            timer = null;
                        }
                    }).start();
                }
            }, 10 * 1000);
        }
    }

    @Override
    public void stop() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        this.teardown();

        if (null != this.adapter) {
            this.adapter.stop();
            this.adapter = null;
        }

        this.pushQueue.clear();
        this.tickets.clear();

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public FerryAdapter getAdapter() {
        return this.adapter;
    }

    public void checkIn(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");

        Logger.d(this.getClass(), "#checkIn - " + domain);

        Ticket ticket = new Ticket(domain, talkContext);
        this.tickets.put(domain, ticket);

        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        if (!authService.hasDomain(domain)) {
            // 获取访问节点
            FerryStorage.AccessPoint accessPoint = this.storage.readAccessPoint(domain);
            if (null == accessPoint) {
                List<FerryStorage.AccessPoint> list = this.storage.readAccessPoints();
                if (!list.isEmpty()) {
                    accessPoint = list.get(0);
                }
            }

            if (null != accessPoint) {
                List<IceServer> iceServers = new ArrayList<>();
                iceServers.add(accessPoint.iceServer);
                // 创建新的域
                authService.createDomainApp(domain, Utils.randomString(16),
                        Long.toString(System.currentTimeMillis()), accessPoint.mainEndpoint,
                        accessPoint.httpEndpoint, accessPoint.httpsEndpoint, iceServers);
            }
            else {
                Logger.e(this.getClass(), "#checkIn - No find domain access point");
            }
        }
    }

    public void checkOut(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");

        Logger.d(this.getClass(), "#checkOut - " + domain);

        this.tickets.remove(domain);
    }

    /**
     * 判断指定域是否在线。
     *
     * @param domain
     * @return
     */
    public boolean isOnlineDomain(String domain) {
        return this.tickets.containsKey(domain);
    }

    /**
     * 向 Boat 推送数据。
     *
     * @param domain
     * @param packet
     */
    public void pushToBoat(String domain, FerryPacket packet) {
        if (!this.tickets.containsKey(domain)) {
            Logger.w(this.getClass(), "#pushToBoat - Can NOT find domain talk context: " + domain);
            return;
        }

        // 设置 Domain
        packet.setDomain(domain);

        this.pushQueue.offer(packet);

        synchronized (this.pushMutex) {
            if (this.pushing) {
                return;
            }

            this.pushing = true;
        }

        (new Thread() {
            @Override
            public void run() {
                FerryPacket ferryPacket = pushQueue.poll();
                while (null != ferryPacket) {
                    Ticket ticket = tickets.get(ferryPacket.getDomain());
                    if (null != ticket) {
                        // 向 Ferry 推送数据
                        cellet.speak(ticket.talkContext, ferryPacket.toDialect());
                    }

                    ferryPacket = pushQueue.poll();
                }

                synchronized (pushMutex) {
                    pushing = false;
                }
            }
        }).start();
    }

    public AuthDomain getDomain(String domainName) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        return authService.getAuthDomain(domainName);
    }

    public List<DomainMember> listDomainMember(String domainName) {
        return this.storage.queryMembers(domainName);
    }

    public void addDomainMember(Contact contact, DomainMember domainMember) {
        // 写入新成员
        this.storage.writeMember(domainMember);

        // 复制数据到新的域
        ContactManager.getInstance().copyContact(contact, domainMember.getDomain().getName());

        FerryPacket ferryPacket = new FerryPacket(FerryPort.JoinDomainMember);
        ferryPacket.getDialect().addParam("member", domainMember.toJSON());
        this.pushToBoat(domainMember.getDomain().getName(), ferryPacket);
    }

    public void removeDomainMember(DomainMember domainMember) {
        // TODO
    }

    private void setup() {
        AbstractModule messagingModule = this.getKernel().getModule("Messaging");
        if (null != messagingModule) {
            messagingModule.getPluginSystem().register("WriteMessage", new WriteMessagePlugin(this));
        }
    }

    private void teardown() {
    }
}
