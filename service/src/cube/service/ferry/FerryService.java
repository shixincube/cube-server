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

import cell.adapter.CelletAdapter;
import cell.adapter.CelletAdapterFactory;
import cell.adapter.CelletAdapterListener;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.entity.*;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.ferry.*;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.ferry.plugin.WriteMessagePlugin;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 摆渡数据服务。
 */
public class FerryService extends AbstractModule implements CelletAdapterListener {

    public final static String NAME = "Ferry";

    private final FerryCellet cellet;

    private FerryStorage storage;

    private Timer timer;

    private CelletAdapter contactsAdapter;

    private Map<String, Ticket> tickets;

    private Queue<FerryPacket> pushQueue;

    private Map<Integer, AckBundle> ackBundles;

    private Object pushMutex = new Object();
    private boolean pushing = false;

    public FerryService(FerryCellet cellet) {
        super();
        this.cellet = cellet;
        this.tickets = new ConcurrentHashMap<>();
        this.pushQueue = new ConcurrentLinkedQueue<>();
        this.ackBundles = new ConcurrentHashMap<>();
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
                contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
                contactsAdapter.addListener(FerryService.this);

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

        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }

        this.teardown();

        this.pushQueue.clear();
        this.tickets.clear();

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        for (AckBundle bundle : this.ackBundles.values()) {
            synchronized (bundle) {
                bundle.notify();
            }
        }
        this.ackBundles.clear();
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public void checkIn(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");

        Logger.d(this.getClass(), "#checkIn - " + domain + "@" + dialect.getParamAsString("address"));

        Ticket ticket = new Ticket(domain, dialect.getParamAsJson("licence"), talkContext);
        this.tickets.put(domain, ticket);

        if (null == this.getKernel() || !this.getKernel().hasModule(AuthService.NAME)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DomainInfo domainInfo = this.getDomainInfo(domain);
        if (null == domainInfo || null == domainInfo.getInvitationCode()) {
            // 写入数据
            this.storage.writeDomainInfo(domain, ticket.getLicenceBeginning(), ticket.getLicenceDuration(),
                    ticket.getLicenceLimit(), dialect.getParamAsString("address"));
            // 写入邀请码
            this.storage.updateInvitationCode(domain, Utils.randomNumberString(6));
        }
        else {
            // 更新数据
            this.storage.writeDomainInfo(domain, ticket.getLicenceBeginning(), ticket.getLicenceDuration(),
                    ticket.getLicenceLimit(), dialect.getParamAsString("address"));
        }

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
                        accessPoint.httpEndpoint, accessPoint.httpsEndpoint, iceServers, true);
            }
            else {
                Logger.e(this.getClass(), "#checkIn - No find domain access point");
            }
        }

        List<Contact> contacts = ContactManager.getInstance().getOnlineContactsInDomain(domain);
        for (Contact contact : contacts) {
            // 发送通知
            JSONObject eventData = new JSONObject();
            eventData.put("domain", domain);
            eventData.put("id", contact.getId().longValue());
            ModuleEvent event = new ModuleEvent(NAME, FerryAction.Online.name, eventData);
            this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());
        }
    }

    public void checkOut(ActionDialect dialect, TalkContext talkContext) {
        String domain = dialect.getParamAsString("domain");

        Logger.d(this.getClass(), "#checkOut - " + domain);

        this.tickets.remove(domain);

        List<Contact> contacts = ContactManager.getInstance().getOnlineContactsInDomain(domain);
        for (Contact contact : contacts) {
            // 发送通知
            JSONObject eventData = new JSONObject();
            eventData.put("domain", domain);
            eventData.put("id", contact.getId().longValue());
            ModuleEvent event = new ModuleEvent(NAME, FerryAction.Offline.name, eventData);
            this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());
        }
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

    public AckBundle touchFerryHouse(String domain, long timeout) {
        if (!this.tickets.containsKey(domain)) {
            return null;
        }

        Ticket ticket = this.tickets.get(domain);

        Integer sn = Utils.randomUnsigned();
        ActionDialect actionDialect = new ActionDialect(FerryAction.Ping.name);
        actionDialect.addParam("sn", sn.intValue());
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("timeout", timeout);

        AckBundle bundle = new AckBundle(actionDialect);
        this.ackBundles.put(sn, bundle);

        if (!this.cellet.speak(ticket.talkContext, actionDialect)) {
            this.ackBundles.remove(sn);
            return null;
        }

        synchronized (bundle) {
            try {
                bundle.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.ackBundles.remove(sn);

        return bundle;
    }

    public void notifyAckBundles(ActionDialect actionDialect) {
        if (!actionDialect.containsParam("sn")) {
            return;
        }

        Integer sn = actionDialect.getParamAsInt("sn");
        AckBundle ackBundle = this.ackBundles.remove(sn);
        if (null == ackBundle) {
            return;
        }

        ackBundle.end = System.currentTimeMillis();

        synchronized (ackBundle) {
            ackBundle.notify();
        }
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

    /**
     * 获取指定域名称的访问域。
     *
     * @param domainName
     * @return
     */
    public AuthDomain getAuthDomain(String domainName) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        return authService.getAuthDomain(domainName);
    }

    /**
     * 获取指定域的详细信息。
     *
     * @param domainName
     * @return
     */
    public DomainInfo getDomainInfo(String domainName) {
        return this.storage.readDomainInfo(domainName);
    }

    /**
     * 获取域内指定成员。
     *
     * @param domainName
     * @param contactId
     * @return
     */
    public DomainMember getDomainMember(String domainName, Long contactId) {
        return this.storage.readMember(domainName, contactId);
    }

    /**
     * 列举域内所有成员。
     *
     * @param domainName
     * @return
     */
    public List<DomainMember> listDomainMember(String domainName) {
        return this.storage.queryMembers(domainName);
    }

    /**
     * 列举域内指定状态成员。
     *
     * @param domainName
     * @param state
     * @return
     */
    public List<DomainMember> listDomainMember(String domainName, int state) {
        return this.storage.queryMembers(domainName, state);
    }

    /**
     * 使用邀请码查找指定的域名称。
     *
     * @param invitationCode
     * @return
     */
    public String getDomainNameByCode(String invitationCode) {
        return this.storage.queryDomainName(invitationCode);
    }

    /**
     * 将指定联系人转入为指定域成员。
     *
     * @param contact
     * @param domainMember
     * @param memberList
     * @return 返回域信息。
     */
    public DomainInfo transferIntoDomainMember(Contact contact, DomainMember domainMember, List<DomainMember> memberList) {
        if (this.storage.countDomainMembers(domainMember.getDomain().getName()) == 0) {
            // 更新授权文件的日期
            DomainInfo domainInfo = this.getDomainInfo(domainMember.getDomain().getName());
            domainInfo.setBeginning(System.currentTimeMillis());

            FerryPacket packet = new FerryPacket(FerryPort.ResetLicence);
            packet.setDomain(domainMember.getDomain().getName());
            packet.getDialect().addParam("licence", domainInfo.toLicence());
            this.pushToBoat(domainMember.getDomain().getName(), packet);

            this.storage.writeDomainInfo(domainInfo);
        }

        // 写入新成员
        this.storage.writeMember(domainMember);

        // 复制数据到新的域
        Contact newContact = ContactManager.getInstance().copyContact(contact, domainMember.getDomain().getName());

        FerryPacket ferryPacket = new FerryPacket(FerryPort.TransferIntoMember);
        ferryPacket.setDomain(domainMember.getDomain().getName());
        ferryPacket.getDialect().addParam("member", domainMember.toJSON());
        ferryPacket.getDialect().addParam("contact", newContact.toJSON());
        this.pushToBoat(domainMember.getDomain().getName(), ferryPacket);

        // 创建通讯录
        ContactZone zone = ContactManager.getInstance().createContactZone(newContact,
                ContactManager.DEFAULT_CONTACT_ZONE_NAME,
                ContactManager.DEFAULT_CONTACT_ZONE_NAME, true, null);

        // 向通讯录内添加已加入的成员
        for (DomainMember member : memberList) {
            ContactZoneParticipant participant = new ContactZoneParticipant(member.getContactId(),
                    ContactZoneParticipantType.Contact, System.currentTimeMillis(),
                    newContact.getId(), null,
                    ContactZoneParticipantState.Normal);
            // 强制添加分区参与人
            ContactManager.getInstance().addParticipantToZoneByForce(newContact, zone, participant);
        }

        // 返回域信息
        return this.storage.readDomainInfo(domainMember.getDomain().getName());
    }

    /**
     * 指定成员转出域。
     *
     * @param domainMember
     * @return 返回域信息。
     */
    public DomainInfo transferOutDomainMember(DomainMember domainMember) {
        this.storage.updateMemberState(domainMember.getDomain().getName(),
                domainMember.getContactId(), DomainMember.QUIT);

        FerryPacket ferryPacket = new FerryPacket(FerryPort.TransferOutMember);
        ferryPacket.setDomain(domainMember.getDomain().getName());
        ferryPacket.getDialect().addParam("member", domainMember.toJSON());
        this.pushToBoat(domainMember.getDomain().getName(), ferryPacket);

        // 修改状态
        domainMember.setState(DomainMember.QUIT);

        return this.storage.readDomainInfo(domainMember.getDomain().getName());
    }

    private void setup() {
        AbstractModule messagingModule = this.getKernel().getModule("Messaging");
        if (null != messagingModule) {
            messagingModule.getPluginSystem().register("WriteMessage", new WriteMessagePlugin(this));
        }
    }

    private void teardown() {
    }

    private void pushToContact(Contact contact, String actionName, JSONObject data) {
        for (Device device : contact.getDeviceList()) {
            TalkContext talkContext = device.getTalkContext();
            if (null == talkContext) {
                continue;
            }

            JSONObject payload = new JSONObject();
            try {
                payload.put("code", FerryStateCode.Ok.code);
                payload.put("data", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Packet packet = new Packet(actionName, payload);
            ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                    contact.getId().longValue(), contact.getDomain().getName());
            this.cellet.speak(talkContext, dialect);
        }
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (FerryService.NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {
            ModuleEvent event = new ModuleEvent(jsonObject);
            String eventName = event.getEventName();

            if (FerryAction.Online.name.equals(eventName) ||
                    FerryAction.Offline.name.equals(eventName)) {
                String domainName = event.getData().getString("domain");
                long contactId = event.getData().getLong("id");
                Contact contact = ContactManager.getInstance().getOnlineContact(domainName, contactId);
                if (null != contact) {
                    JSONObject data = new JSONObject();
                    data.put("domain", domainName);
                    pushToContact(contact, eventName, data);
                }
            }
        }
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, JSONObject jsonObject) {
        // Nothing
    }

    @Override
    public void onSubscribeFailed(String topic, Endpoint endpoint) {
        // Nothing
    }

    @Override
    public void onUnsubscribeFailed(String topic, Endpoint endpoint) {
        // Nothing
    }

    /**
     * 应答数据绑定。
     */
    public class AckBundle {

        public final long start;

        public long end;

        public final ActionDialect request;

        public AckBundle(ActionDialect request) {
            this.start = System.currentTimeMillis();
            this.request = request;
        }
    }
}
