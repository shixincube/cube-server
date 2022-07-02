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
import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.UniqueKey;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.ferry.*;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.ferry.plugin.*;
import cube.service.ferry.tenet.CleanupTenet;
import cube.service.ferry.tenet.Tenet;
import cube.storage.StorageType;
import cube.util.CodeUtils;
import cube.util.ConfigUtils;
import cube.util.FileUtils;
import cube.vision.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private Object pushMutex = new Object();
    private boolean pushing = false;

    private Queue<StreamBundle> streamQueue;

    private ExecutorService executor;

    private Map<Integer, AckBundle> ackBundles;

    private Map<String, BoxReport> boxReportMap;

    public FerryService(FerryCellet cellet) {
        super();
        this.cellet = cellet;
        this.tickets = new ConcurrentHashMap<>();
        this.pushQueue = new ConcurrentLinkedQueue<>();
        this.streamQueue = new ConcurrentLinkedQueue<>();
        this.ackBundles = new ConcurrentHashMap<>();
        this.boxReportMap = new ConcurrentHashMap<>();
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

        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);

        (new Thread() {
            @Override
            public void run() {
                contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
                contactsAdapter.addListener(FerryService.this);

                storage.open();
                storage.execSelfChecking(null);

                TenetManager.getInstance().start(FerryService.this, storage, contactsAdapter);
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

        if (null != this.executor) {
            this.executor.shutdown();
            this.executor = null;
        }

        TenetManager.getInstance().stop();

        this.boxReportMap.clear();
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
        // Nothing
    }

    /**
     * 创建访问点。
     *
     * @param domainName
     * @param mainPoint
     * @param httpPoint
     * @param httpsPoint
     * @param iceServer
     */
    public void createAccessPoint(String domainName, Endpoint mainPoint, Endpoint httpPoint,
                                  Endpoint httpsPoint, IceServer iceServer) {
        this.storage.writeAccessPoint(mainPoint, httpPoint, httpsPoint, iceServer, domainName);
    }

    /**
     * 更新访问点。
     *
     * @param domainName
     * @param mainPoint
     * @param httpPoint
     * @param httpsPoint
     */
    public void updateAccessPoint(String domainName, Endpoint mainPoint, Endpoint httpPoint,
                                  Endpoint httpsPoint) {
        this.storage.updateAccessPoint(domainName, mainPoint, httpPoint, httpsPoint);
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
        if (null == domainInfo) {
            // 创建访问二维码
            FileLabel codeFile = this.makeQRCodeFile(domain);

            // 写入数据
            this.storage.writeDomainInfo(domain, ticket.getLicenceBeginning(), ticket.getLicenceDuration(),
                    ticket.getLicenceLimit(), codeFile, DomainInfo.STATE_NORMAL, FerryHouseFlag.STANDARD,
                    dialect.getParamAsString("address"));
            // 写入邀请码
            this.storage.updateInvitationCode(domain, Utils.randomNumberString(6));
        }
        else {
            // 更新数据
            this.storage.writeDomainInfo(domain, ticket.getLicenceBeginning(), ticket.getLicenceDuration(),
                    ticket.getLicenceLimit(), domainInfo.getQRCodeFileLabel(),
                    domainInfo.getState(), domainInfo.getFlag(),
                    dialect.getParamAsString("address"));
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

        if (!ContactManager.getInstance().isStarted()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int count = 3;
        while (!ContactManager.getInstance().isStarted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            --count;
            if (count <= 0) {
                break;
            }
        }

        try {
            List<Contact> contacts = ContactManager.getInstance().getOnlineContactsInDomain(domain);
            for (Contact contact : contacts) {
                // 发送通知
                JSONObject eventData = new JSONObject();
                eventData.put("domain", domain);
                eventData.put("id", contact.getId().longValue());
                ModuleEvent event = new ModuleEvent(NAME, FerryAction.Online.name, eventData);
                this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());
            }
        } catch (Exception e) {
            Logger.w(getClass(), "#checkIn", e);
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

    private FileLabel makeQRCodeFile(String domainName) {
        String string = "https://box.shixincube.com/box/" + domainName;
        File qrFile = new File("storage/tmp/ferry_qrcode_" + domainName + ".jpg");
        // 生成二维码文件
        if (CodeUtils.generateQRCode(qrFile, string, 400, 400, new Color("#000000"))) {
            AbstractModule fileStorage = this.getKernel().getModule("FileStorage");

            // 生成文件码
            String fileCode = FileUtils.makeFileCode(0L, domainName, qrFile.getName());

            // 生成文件标签
            FileLabel fileLabel = FileUtils.makeFileLabel(domainName, fileCode, 0L, qrFile);
            // 10 年期
            fileLabel.setExpiryTime(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000);

            JSONObject notifyData = new JSONObject();
            notifyData.put("action", FileStorageAction.SaveFile.name);
            notifyData.put("path", qrFile.getAbsolutePath());
            notifyData.put("fileLabel", fileLabel.toJSON());
            try {
                JSONObject result = (JSONObject) fileStorage.notify(notifyData);
                if (null != result) {
                    // 删除文件
                    qrFile.delete();

                    return new FileLabel(result);
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#makeQRCodeFile", e);
            }
        }

        if (qrFile.exists()) {
            qrFile.delete();
        }

        return null;
    }

    /**
     * 触发指定的信条。
     *
     * @param dialect
     */
    public void triggerTenet(ActionDialect dialect) {
        String domain = dialect.getParamAsString("domain");
        String port = dialect.getParamAsString("port");

        Tenet tenet = null;

        if (FerryPort.Cleanup.equals(port)) {
            long timestamp = dialect.getParamAsLong("timestamp");
            tenet = new CleanupTenet(domain, timestamp);
            // TODO 操作 Messaging 服务
            // TODO 操作 FileStorage 服务
        }

        if (null != tenet) {
            // 触发信条
            TenetManager.getInstance().triggerTenet(tenet);
        }
        else {
            Logger.e(this.getClass(), "Unknown tenet port: " + port + "@" + domain);
        }
    }

    public void processSynchronize(ActionDialect dialect, TalkContext talkContext) {
        String domainName = dialect.getParamAsString("domain");

        List<DomainMember> memberList = this.storage.queryMembers(domainName);

        JSONArray memberArray = new JSONArray();
        JSONArray contactArray = new JSONArray();

        for (DomainMember member : memberList) {
            Contact contact = ContactManager.getInstance().getContact(domainName, member.getContactId());

            memberArray.put(member.toJSON());
            contactArray.put(contact.toJSON());
        }

        JSONObject data = new JSONObject();
        data.put("members", memberArray);
        data.put("contacts", contactArray);

        ActionDialect response = new ActionDialect(FerryAction.Synchronize.name);
        response.addParam("domain", domainName);
        response.addParam("data", data);
        this.cellet.speak(talkContext, response);
    }

    /**
     * 判断指定域是否在线。
     *
     * @param domain 指定域名称。
     * @return 返回是否在线。
     */
    public boolean isOnlineDomain(String domain) {
        if (this.tickets.containsKey(domain)) {
            return true;
        }

        DomainInfo domainInfo = this.getDomainInfo(domain);
        int flag = domainInfo.getFlag();
        if (FerryHouseFlag.isAllowVirtualMode(flag)) {
            VirtualTicket virtualTicket = new VirtualTicket(domainInfo);
            this.tickets.put(domain, virtualTicket);
            return true;
        }

        return false;
    }

    /**
     * 向 House 进行潜伏期探测。
     *
     * @param domain
     * @param member
     * @param timeout
     * @return
     */
    public synchronized MembershipAckBundle touchFerryHouse(String domain, Contact member, long timeout) {
        // 判断成员是否是该域成员
        if (!this.storage.isDomainMember(domain, member)) {
            Logger.i(this.getClass(), "#touchFerryHouse - Not member: " + domain + " - " + member.getId());
            return new MembershipAckBundle(false);
        }

        if (!this.tickets.containsKey(domain)) {
            // House 不在线，判断是否是可虚拟化
            DomainInfo domainInfo = this.getDomainInfo(domain);
            int flag = domainInfo.getFlag();
            if (FerryHouseFlag.isAllowVirtualMode(flag)) {
                // 允许虚拟模式
                VirtualTicket virtualTicket = new VirtualTicket(domainInfo);
                this.tickets.put(domain, virtualTicket);

                MembershipAckBundle result = new MembershipAckBundle(true);
                result.end = System.currentTimeMillis() + 1;
                return result;
            }

            return new MembershipAckBundle(true);
        }

        Ticket ticket = this.tickets.get(domain);

        if (ticket.sessionId == 0) {
            MembershipAckBundle result = new MembershipAckBundle(true);
            result.end = System.currentTimeMillis() + 1;
            return result;
        }

        Integer sn = Utils.randomUnsigned();
        ActionDialect actionDialect = new ActionDialect(FerryAction.Ping.name);
        actionDialect.addParam("sn", sn.intValue());
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("timeout", timeout);

        MembershipAckBundle bundle = new MembershipAckBundle(actionDialect);
        this.ackBundles.put(sn, bundle);

        if (!this.cellet.speak(ticket.talkContext, actionDialect)) {
            this.ackBundles.remove(sn);
            return bundle;
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

    protected void notifyAckBundles(ActionDialect actionDialect) {
        if (!actionDialect.containsParam("sn")) {
            return;
        }

        Integer sn = actionDialect.getParamAsInt("sn");
        AckBundle ackBundle = this.ackBundles.remove(sn);
        if (null == ackBundle) {
            return;
        }

        ackBundle.end = System.currentTimeMillis();
        ackBundle.response = actionDialect;

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
            if (Logger.isDebugLevel()) {
                Logger.w(this.getClass(), "#pushToBoat - Can NOT find domain talk context: " + domain);
            }
            return;
        }

        // 设置 Domain
        packet.setDomain(domain);

        if (this.tickets.get(domain).sessionId == 0) {
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#pushToBoat - Ticket is virtual: " + domain);
            }
            return;
        }

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
                    if (null != ticket && null != ticket.talkContext) {
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
     * 推送流给 Boat 。
     *
     * @param domain
     * @param streamName
     * @param inputStream
     */
    public void pushToBoat(String domain, String streamName, InputStream inputStream) {
        if (!this.tickets.containsKey(domain)) {
            if (Logger.isDebugLevel()) {
                Logger.w(this.getClass(), "#pushToBoat(stream) - Can NOT find domain talk context: " + domain);
            }
            return;
        }

        Ticket ticket = this.tickets.get(domain);

        if (ticket.sessionId == 0) {
            // 虚拟 House
            try {
                inputStream.close();
            } catch (IOException e) {
                Logger.w(this.getClass(), "#pushToBoat(stream)", e);
            }
            return;
        }

        TalkContext talkContext = ticket.talkContext;

        StreamBundle streamBundle = new StreamBundle(talkContext, streamName, inputStream);
        // 添加到队列
        this.streamQueue.offer(streamBundle);

        // 发送 Mark
        ActionDialect markAction = new ActionDialect(FerryAction.MarkFile.name);
        markAction.addParam("code", streamName);
        markAction.addParam("domain", domain);
        this.cellet.speak(talkContext, markAction);

        this.executor.execute(() -> {
            StreamBundle bundle = streamQueue.poll();

            while (null != bundle) {
                PrimitiveOutputStream outputStream = cellet.speakStream(bundle.talkContext, bundle.streamName);
                InputStream is = bundle.inputStream;

                byte[] bytes = new byte[256];
                int length = 0;
                try {
                    while ((length = inputStream.read(bytes)) > 0) {
                        outputStream.write(bytes, 0, length);
                    }
                } catch (IOException e) {
                    Logger.w(this.getClass(), "#pushToBoat(stream)", e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Nothing
                    }

                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }

                bundle = streamQueue.poll();
            }
        });
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
        List<DomainMember> memberList = this.storage.queryMembers(domainName);
        return this.checkRole(memberList);
    }

    /**
     * 列举域内指定状态成员。
     *
     * @param domainName
     * @param state
     * @return
     */
    public List<DomainMember> listDomainMember(String domainName, int state) {
        List<DomainMember> memberList = this.storage.queryMembers(domainName, state);
        return this.checkRole(memberList);
    }

    /**
     * 检查角色。
     *
     * @param memberList
     * @return
     */
    private List<DomainMember> checkRole(List<DomainMember> memberList) {
        int adminCount = 0;
        DomainMember first = null;
        long timestamp = System.currentTimeMillis();
        for (DomainMember member : memberList) {
            if (member.getRole() == Role.Administrator) {
                ++adminCount;
            }

            if (member.getJoinTime() < timestamp) {
                first = member;
                timestamp = member.getJoinTime();
            }
        }

        if (adminCount == 1) {
            return memberList;
        }

        for (DomainMember member : memberList) {
            if (first.getContactId().equals(member.getContactId())) {
                first.setRole(Role.Administrator);
                this.storage.updateMemberRole(first);
            }
            else {
                if (member.getRole() == Role.Administrator) {
                    member.setRole(Role.Member);
                    this.storage.updateMemberRole(member);
                }
            }
        }

        return memberList;
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

    /**
     * 取出信条。
     *
     * @param domainName
     * @param contactId
     * @return
     */
    public List<JSONObject> takeOutTenets(String domainName, Long contactId) {
        return this.storage.readAndDeleteTenets(domainName, contactId);
    }

    /**
     *
     * @param domainName
     * @return
     */
    public BoxReport getBoxReport(String domainName) {
        BoxReport report = this.boxReportMap.get(domainName);
        if (null != report && System.currentTimeMillis() - report.getTimestamp() < 60 * 60 * 1000) {
            return report;
        }

        // 向指定 House 获取报告
        Ticket ticket = this.tickets.get(domainName);
        if (null == ticket) {
            Logger.i(this.getClass(), "#getBoxReport - Domain is offline: " + domainName);
            return report;
        }

        if (ticket instanceof VirtualTicket) {
            // 虚拟 House
            VirtualTicket virtualTicket = (VirtualTicket) ticket;
            return virtualTicket.getBoxReport(this);
        }

        Integer sn = Utils.randomUnsigned();

        ActionDialect actionDialect = new ActionDialect(FerryAction.Report.name);
        actionDialect.addParam("sn", sn.intValue());
        actionDialect.addParam("domain", domainName);
        this.cellet.speak(ticket.talkContext, actionDialect);

        AckBundle bundle = new AckBundle(actionDialect);
        this.ackBundles.put(sn, bundle);

        if (!this.cellet.speak(ticket.talkContext, actionDialect)) {
            this.ackBundles.remove(sn);
            return report;
        }

        synchronized (bundle) {
            try {
                bundle.wait(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.ackBundles.remove(sn);

        if (null == bundle.response) {
            Logger.i(this.getClass(), "#getBoxReport - House NO ack: " + domainName);
            return report;
        }

        JSONObject reportJSON = bundle.response.getParamAsJson("report");
        if (!reportJSON.has("domain")) {
            Logger.i(this.getClass(), "#getBoxReport - House NO report: " + domainName);
            return report;
        }

        // 序列化报告
        report = new BoxReport(reportJSON);

        // 更新缓存
        this.boxReportMap.put(domainName, report);

        return report;
    }

    private void setup() {
        AbstractModule messagingModule = this.getKernel().getModule("Messaging");
        if (null != messagingModule) {
            int count = 100;
            while (null == messagingModule.getPluginSystem()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (--count <= 0) {
                    break;
                }
            }

            if (null == messagingModule.getPluginSystem()) {
                Logger.e(this.getClass(), "#setup - Can NOT get messaging service plugin system");
                System.exit(1);
                return;
            }

            messagingModule.getPluginSystem().register("WriteMessage", new WriteMessagePlugin(this));
            messagingModule.getPluginSystem().register("UpdateMessage", new UpdateMessagePlugin(this));
            messagingModule.getPluginSystem().register("DeleteMessage", new DeleteMessagePlugin(this));
            messagingModule.getPluginSystem().register("BurnMessage", new BurnMessagePlugin(this));
        }

        AbstractModule fileStorageModule = this.getKernel().getModule("FileStorage");
        if (null != fileStorageModule) {
            fileStorageModule.getPluginSystem().register("SaveFile", new SaveFilePlugin(this));
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
    public Object notify(Object data) {
        if (data instanceof JSONObject) {
            JSONObject jsonData = (JSONObject) data;
            String action = jsonData.getString("action");

            if (action.equals("createAccessPoint")) {
                String domainName = jsonData.getString("domain");
                Endpoint mainPoint = new Endpoint(jsonData.getJSONObject("main"));
                Endpoint httpPoint = new Endpoint(jsonData.getJSONObject("http"));
                Endpoint httpsPoint = new Endpoint(jsonData.getJSONObject("https"));
                IceServer iceServer = new IceServer(jsonData.getJSONObject("iceServer"));
                this.createAccessPoint(domainName, mainPoint, httpPoint, httpsPoint, iceServer);
            }
            else if (action.equals("updateAccessPoint")) {
                String domainName = jsonData.getString("domain");
                Endpoint mainPoint = new Endpoint(jsonData.getJSONObject("main"));
                Endpoint httpPoint = new Endpoint(jsonData.getJSONObject("http"));
                Endpoint httpsPoint = new Endpoint(jsonData.getJSONObject("https"));
                this.updateAccessPoint(domainName, mainPoint, httpPoint, httpsPoint);
            }
        }

        return null;
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (FerryService.NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {
            ModuleEvent event = new ModuleEvent(jsonObject);
            String eventName = event.getEventName();

            if (FerryAction.Tenet.name.equals(eventName)) {
                Object[] key = UniqueKey.extract(topic);
                if (null == key) {
                    return;
                }

                // 取主键的 ID
                Long id = (Long) key[0];
                // 取主键的域
                String domain = (String) key[1];

                Contact contact = ContactManager.getInstance().getOnlineContact(domain, id);
                if (null != contact) {
                    // 推送给终端
                    this.pushToContact(contact, FerryAction.Tenet.name, event.getData());

                    // 从数据库删除
                    try {
                        this.storage.deleteTenets(domain, id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (FerryAction.Online.name.equals(eventName) ||
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

        public long end = 0;

        public final ActionDialect request;

        public ActionDialect response;

        public AckBundle(ActionDialect request) {
            this.start = System.currentTimeMillis();
            this.request = request;
        }
    }

    /**
     * 成员关系绑定。
     */
    public class MembershipAckBundle extends AckBundle {

        public final boolean membership;

        public MembershipAckBundle(boolean membership) {
            super(null);
            this.membership = membership;
        }

        public MembershipAckBundle(ActionDialect request) {
            super(request);
            this.membership = true;
        }
    }

    public class StreamBundle {

        public final TalkContext talkContext;

        public final String streamName;

        public final InputStream inputStream;

        public StreamBundle(TalkContext talkContext, String streamName, InputStream inputStream) {
            this.talkContext = talkContext;
            this.streamName = streamName;
            this.inputStream = inputStream;
        }
    }
}
