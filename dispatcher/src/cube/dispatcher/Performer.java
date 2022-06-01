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

package cube.dispatcher;

import cell.api.*;
import cell.core.cellet.Cellet;
import cell.core.net.Endpoint;
import cell.core.talk.*;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.StateCode;
import cube.common.UniqueKey;
import cube.common.action.ClientAction;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.AuthStateCode;
import cube.dispatcher.util.Tickable;
import cube.util.FileUtils;
import cube.util.HttpClientFactory;
import cube.util.HttpConfig;
import cube.util.HttpServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performer 接入层连接器。
 */
public class Performer implements TalkListener, Tickable {

    private final String performerKey = "_performer";

    private final String directorKey = "_director";

    protected TalkService talkService;

    protected CelletService celletService;

    /**
     * 节点名称。
     */
    private String nodeName;

    /**
     * HTTP 服务。
     */
    private HttpServer httpServer;

    /**
     * 服务主机列表。
     */
    private List<Director> directorList;

    /**
     * Cellet 对应的 Director 列表。
     */
    private HashMap<String, List<Director>> celletDirectorMap;

    /**
     * 会话上下文对应的服务主机。
     */
    protected HashMap<TalkContext, Director> talkDirectorMap;

    /**
     * 执行机监听器。
     */
    private ConcurrentHashMap<String, PerformerListener> listenerMap;

    /**
     * 在线的联系人。
     */
    private ConcurrentHashMap<String, Contact> onlineContacts;

    /**
     * 令牌对应的设备。
     */
    private ConcurrentHashMap<String, Device> tokenDeviceMap;

    /**
     * 有效的令牌。
     */
    private ConcurrentHashMap<String, AuthToken> validAuthTokenMap;

    /**
     * 数据传输记录。
     */
    protected ConcurrentHashMap<Long, Transmission> transmissionMap;

    /**
     * 阻塞块记录。
     */
    private ConcurrentHashMap<Long, Block> blockMap;

    /**
     * 阻塞超时时长。
     */
    private long blockTimeout = 10000;

    /**
     * 定时回调清单。
     */
    private List<Tickable> tickableList;

    /**
     * 构造函数。
     *
     * @param nucleus 当前 Cell 的内核实例。
     */
    public Performer(Nucleus nucleus) {
        this.talkService = nucleus.getTalkService();
        this.celletService = nucleus.getCelletService();
        this.directorList = new ArrayList<>();
        this.celletDirectorMap = new HashMap<>();
        this.talkDirectorMap = new HashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
        this.onlineContacts = new ConcurrentHashMap<>();
        this.tokenDeviceMap = new ConcurrentHashMap<>();
        this.validAuthTokenMap = new ConcurrentHashMap<>();
        this.transmissionMap = new ConcurrentHashMap<>();
        this.blockMap = new ConcurrentHashMap<>();
        this.tickableList = new ArrayList<>();
    }

    /**
     * 设置节点名。
     *
     * @param nodeName 指定节点名。
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * 获取节点名。
     *
     * @return 返回节点名。
     */
    public String getNodeName() {
        return this.nodeName;
    }

    /**
     * 返回 HTTP 服务器实例。
     *
     * @return 返回 HTTP 服务器实例。
     */
    public HttpServer getHttpServer() {
        return this.httpServer;
    }

    /**
     * 配置 HTTP 服务器。
     *
     * @param config 指定配置信息。
     */
    public void configHttpServer(HttpConfig config) {
        if (null == this.httpServer) {
            this.httpServer = new HttpServer();
        }

        if (config.httpsPort > 0) {
            try {
                this.httpServer.setKeystorePath(config.keystore);
            } catch (FileNotFoundException e) {
                Logger.w(this.getClass(), "install", e);
            }
            this.httpServer.setKeystorePassword(config.storePassword, config.managerPassword);
        }

        this.httpServer.setPort(config.httpPort, config.httpsPort);
    }

    /**
     * 添加 Director 节点。
     * @param address 导演机的地址。
     * @param port 导演机的端口。
     * @param scope 该导演机的配置范围。
     * @return 返回导演机节点。
     */
    public Director addDirector(String address, int port, Scope scope) {
        Endpoint endpoint = new Endpoint(address, port);
        Director director = new Director(endpoint, scope);

        if (this.directorList.contains(director)) {
            return null;
        }

        this.directorList.add(director);

        for (String celletName : scope.cellets) {
            List<Director> list = this.celletDirectorMap.get(celletName);
            if (null == list) {
                list = new ArrayList<>();
                this.celletDirectorMap.put(celletName, list);
            }
            list.add(director);
        }

        return director;
    }

    /**
     * 选择节点。
     *
     * @param talkContext 会话上下文。
     * @param celletName Cellet 名称。
     * @return 返回被选中的导演机。
     */
    private synchronized Director selectDirector(TalkContext talkContext, String celletName) {
        Director director = this.talkDirectorMap.get(talkContext);
        if (null != director) {
            return director;
        }

        List<Director> directors = this.celletDirectorMap.get(celletName);
        if (null == directors) {
            director = this.directorList.get(0);
            this.talkDirectorMap.put(talkContext, director);
            return director;
        }

        int totalWeight = directors.get(0).getSection(celletName).totalWeight;
        int anchor = Utils.randomInt(0, totalWeight - 1);
        for (int i = 0, size = directors.size(); i < size; ++i) {
            Director cur = directors.get(i);
            Director.Section section = cur.getSection(celletName);
            if (section.contains(anchor)) {
                director = cur;
                break;
            }
        }

        if (null != director) {
            this.talkDirectorMap.put(talkContext, director);
            return director;
        }

        Logger.w(this.getClass(), "Can NOT find director : " + celletName);
        return this.directorList.get(0);
    }

    /**
     * 选择节点。
     *
     * @param tokenCode 令牌码。
     * @param celletName Cellet 名称。
     * @return 返回被选中的导演机。
     */
    private synchronized Director selectDirector(String tokenCode, String celletName) {
        // 获取令牌对应的设备
        Device device = this.tokenDeviceMap.get(tokenCode);
        if (null == device) {
            Logger.i(this.getClass(), "#selectDirector Can NOT find device by token: " + tokenCode);
            return null;
        }

        return this.selectDirector(device.getTalkContext(), celletName);
    }

    /**
     * 选择节点。随机方式。
     *
     * @return
     */
    private synchronized Director selectDirector() {
        return this.directorList.get(Utils.randomInt(0, this.directorList.size() - 1));
    }

    /**
     * 更新联系人。
     *
     * @param contact 指定联系人。
     * @param device 指定当前设备。
     */
    public void updateContact(Contact contact, Device device) {
        // 记录 Token 对应的设备
        this.tokenDeviceMap.put(device.getToken(), device);

        Contact current = this.onlineContacts.get(contact.getUniqueKey());
        if (null == current) {
            this.onlineContacts.put(contact.getUniqueKey(), contact);
        }
        else {
            current.setName(contact.getName());

            JSONObject ctx = contact.getContext();
            if (null != ctx) {
                current.setContext(ctx);
            }

            for (Device dev : contact.getDeviceList()) {
                current.addDevice(dev);
            }
        }
    }

    /**
     * 从在线列表移除联系人。
     *
     * @param contact
     * @param device
     */
    public void removeContact(Contact contact, Device device) {
        Contact current = this.onlineContacts.get(contact.getUniqueKey());
        if (null == current) {
            return;
        }

        current.removeDevice(device);

        if (current.numDevices() == 0) {
            // 已经没有设备连接，从在线列表删除
            this.onlineContacts.remove(contact.getUniqueKey());

            Logger.i(this.getClass(), "Remove online contact: " + contact.getId());
        }
    }

    protected Map<String, Contact> getOnlineContacts() {
        return this.onlineContacts;
    }

    public void setListener(String celletName, PerformerListener listener) {
        this.listenerMap.put(celletName, listener);
    }

    public void removeTalkContext(TalkContext context) {
        this.talkDirectorMap.remove(context);

        // 删除记录
        Iterator<Transmission> iter = this.transmissionMap.values().iterator();
        while (iter.hasNext()) {
            Transmission transmission = iter.next();
            if (transmission.source == context) {
                iter.remove();
            }
        }

        Iterator<Device> diter = this.tokenDeviceMap.values().iterator();
        while (diter.hasNext()) {
            Device device = diter.next();
            if (device.getTalkContext() == context) {
                diter.remove();
            }
        }
    }

    /**
     * 启动执行机，并对路由权重和范围进行初始化。
     */
    public void start(List<String> cellets) {
        /* FIXME 20220521 不按照 Cellet 添加监听器
        for (String cellet : cellets) {
            this.talkService.setListener(cellet, this);
            Logger.i(this.getClass(), "Set cellet '" + cellet + "' listener");
        }*/
        // 添加全局监听
        this.talkService.addListener(this);

        Iterator<Map.Entry<String, List<Director>>> iter = this.celletDirectorMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<Director>> e = iter.next();
            String celletName = e.getKey();
            List<Director> directors = e.getValue();

            // 计算 Cellet 对应的每个连接的权重
            int totalWeight = 0;
            int cursor = 0;

            for (Director director : directors) {
                // 累加权重
                totalWeight += director.scope.weight;

                Director.Section section = director.getSection(celletName);
                section.begin = cursor;
                section.end = cursor + director.scope.weight - 1;

                // 更新游标
                cursor += section.end + 1;
            }

            for (Director director : directors) {
                Director.Section section = director.getSection(celletName);
                section.totalWeight = totalWeight;
            }
        }

        // 启动所有 Director
        for (Director director : this.directorList) {
            Endpoint ep = director.endpoint;
            Speakable speakable = this.talkService.call(ep.getHost(), ep.getPort());
            director.speaker = speakable;
        }

        // 启动 HTTP 服务器
        this.httpServer.start();
    }

    public void stop() {
        for (Block block : this.blockMap.values()) {
            synchronized (block) {
                block.notify();
            }
        }
        this.blockMap.clear();

        // 停止 HTTP 服务器
        this.httpServer.stop();

        for (Director director : this.directorList) {
            Endpoint ep = director.endpoint;
            this.talkService.hangup(ep.getHost(), ep.getPort(), false);
        }

        HttpClientFactory.getInstance().close();
    }

    /**
     * 获取指定令牌对应的会话上下文。
     *
     * @param tokenCode
     * @return
     */
    public TalkContext getTalkContext(String tokenCode) {
        Device device = this.tokenDeviceMap.get(tokenCode);
        if (null == device) {
            return null;
        }

        return device.getTalkContext();
    }

    /**
     * 查询联系人。
     *
     * @param talkContext 指定会话上下文。
     * @return
     */
    public Contact queryContact(TalkContext talkContext) {
        Iterator<Contact> iter = this.onlineContacts.values().iterator();
        while (iter.hasNext()) {
            Contact contact = iter.next();
            Device device = contact.getDevice(talkContext);
            if (null != device) {
                Contact result = new Contact(contact.getId(), contact.getDomain(), contact.getName());
                result.addDevice(device);
                return result;
            }
        }
        return null;
    }

    /**
     * 校验令牌码是否有效。
     *
     * @param tokenCode
     * @return
     */
    public AuthToken verifyToken(String tokenCode) {
        AuthToken authToken = this.validAuthTokenMap.get(tokenCode);
        if (null != authToken) {
            return authToken;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.GetAuthToken.name);
        actionDialect.addParam("tokenCode", tokenCode);

        ActionDialect response = this.syncTransmit("Client", actionDialect);
        if (response.getParamAsInt("code") != AuthStateCode.Ok.code) {
            return null;
        }

        authToken = new AuthToken(response.getParamAsJson("token"));
        this.validAuthTokenMap.put(tokenCode, authToken);
        return authToken;
    }

    /**
     * 向服务单元发送数据，不等待应答。
     * @param talkContext
     * @param celletName
     * @param actionDialect
     */
    public void transmit(TalkContext talkContext, String celletName, ActionDialect actionDialect) {
        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();

        Director director = this.selectDirector(talkContext, celletName);
        if (null == director) {
            Logger.e(this.getClass(), "Can not connect '" + celletName + "'");
            return;
        }

        // 添加 P-KEY 记录
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        director.speaker.speak(celletName, actionDialect);
    }

    /**
     * 向服务单元发送数据，不等待应答。
     *
     * @param talkContext
     * @param cellet
     * @param actionDialect
     */
    public void transmit(TalkContext talkContext, Cellet cellet, ActionDialect actionDialect) {
        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();

        Director director = this.selectDirector(talkContext, cellet.getName());
        if (null == director) {
            Logger.e(this.getClass(), "Can not connect '" + cellet.getName() + "'");
            return;
        }

        // 增加 P-KEY 记录
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        // 绑定关系
        Transmission trans = new Transmission(sn, cellet, talkContext);
        this.transmissionMap.put(trans.sn, trans);

        director.speaker.speak(cellet.getName(), actionDialect);
    }

    /**
     * 向服务单元发送数据，该发送不做任何记录。
     *
     * @param tokenCode
     * @param celletName
     * @param actionDialect
     */
    public void transmit(String tokenCode, String celletName, ActionDialect actionDialect) {
        Device device = this.tokenDeviceMap.get(tokenCode);
        if (null == device) {
            Logger.w(this.getClass(), "#transmit : Can NOT find token device, token: " + tokenCode);
            return;
        }

        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();
        
        // 增加 P-KEY 记录
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        Director director = this.selectDirector(device.getTalkContext(), celletName);

        director.speaker.speak(celletName, actionDialect);
    }

    /**
     * 向服务单元发送流。
     *
     * @param tokenCode
     * @param celletName
     * @param streamName
     * @param inputStream
     */
    public void transmit(String tokenCode, String celletName, String streamName, InputStream inputStream) {
        Device device = this.tokenDeviceMap.get(tokenCode);
        if (null == device) {
            Logger.w(this.getClass(), "#transmit : Can NOT find token device, token: " + tokenCode);
            return;
        }

        Director director = this.selectDirector(device.getTalkContext(), celletName);

        long total = 0;
        // 发送数据
        PrimitiveOutputStream stream = director.speaker.speakStream(celletName, streamName);
        try {
            byte[] buf = new byte[64 * 1024];
            int length = 0;
            while ((length = inputStream.read(buf)) > 0) {
                stream.write(buf, 0, length);
                total += length;
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "Writing primitive output stream", e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // Nothing
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                // Nothing
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#transmit stream '" + streamName + "' (" + total + " bytes) to " + tokenCode);
        }
    }

    /**
     * 向服务单元发送流。
     *
     * @param celletName
     * @param streamName
     * @param inputStream
     * @return 返回流的 MD5 码和 SHA1 码。
     */
    public String[] transmit(String celletName, String streamName, InputStream inputStream) {
        // 选择 Director
        Director director = this.selectDirector();

        MessageDigest md5 = null;
        MessageDigest sha1 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        long total = 0;
        // 发送数据
        PrimitiveOutputStream stream = director.speaker.speakStream(celletName, streamName);
        try {
            byte[] buf = new byte[64 * 1024];
            int length = 0;
            while ((length = inputStream.read(buf)) > 0) {
                // 发送数据
                stream.write(buf, 0, length);

                // 计算散列
                md5.update(buf, 0, length);
                sha1.update(buf, 0, length);

                total += length;
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "Writing primitive output stream", e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // Nothing
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                // Nothing
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#transmit stream '" + streamName + "' (" + total + " bytes)");
        }

        byte[] hashMD5 = md5.digest();
        byte[] hashSHA1 = sha1.digest();
        String md5Code = FileUtils.bytesToHexString(hashMD5);
        String sha1Code = FileUtils.bytesToHexString(hashSHA1);
        return new String[] { md5Code, sha1Code };
    }

    /**
     * 向服务单元发送数据，并阻塞当前线程直到应答或超时。
     *
     * @param talkContext
     * @param celletName
     * @param actionDialect
     * @return
     */
    public ActionDialect syncTransmit(TalkContext talkContext, String celletName, ActionDialect actionDialect) {
        Director director = this.selectDirector(talkContext, celletName);
        if (null == director) {
            Logger.e(this.getClass(), "Can not connect '" + celletName + "'");
            return null;
        }

        return this.syncTransmit(director, celletName, actionDialect, this.blockTimeout);
    }

    /**
     * 向服务单元发送数据，并阻塞当前线程直到应答或超时。
     *
     * @param tokenCode
     * @param celletName
     * @param actionDialect
     * @return
     */
    public ActionDialect syncTransmit(String tokenCode, String celletName, ActionDialect actionDialect) {
        Device device = this.tokenDeviceMap.get(tokenCode);
        if (null == device) {
            Logger.i(this.getClass(), "#syncTransmit Can NOT find device by token: " + tokenCode);
            return null;
        }

        return this.syncTransmit(device.getTalkContext(), celletName, actionDialect);
    }

    /**
     * 向服务单元发送数据，并阻塞当前线程直到应答或超时。
     *
     * @param celletName
     * @param actionDialect
     * @return
     */
    public ActionDialect syncTransmit(String celletName, ActionDialect actionDialect) {
        return this.syncTransmit(this.selectDirector(), celletName, actionDialect, this.blockTimeout);
    }

    /**
     * 向服务单元发送数据，并阻塞当前线程直到应答或超时。
     *
     * @param celletName
     * @param actionDialect
     * @param timeout
     * @return
     */
    public ActionDialect syncTransmit(String celletName, ActionDialect actionDialect, long timeout) {
        return this.syncTransmit(this.selectDirector(), celletName, actionDialect, timeout);
    }

    private ActionDialect syncTransmit(Director director, String celletName, ActionDialect actionDialect, long timeout) {
        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();

        // 添加 Performer 信息
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        Block block = new Block(sn);
        this.blockMap.put(block.sn, block);

        if (!director.speaker.speak(celletName, actionDialect)) {
            this.blockMap.remove(block.sn);
            return null;
        }

        synchronized (block) {
            try {
                block.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.blockMap.remove(block.sn);

        if (null != block.dialect) {
            // 删除不需要返回的参数 P-KEY
            block.dialect.removeParam(this.performerKey);
            return block.dialect;
        }
        else {
            Logger.e(this.getClass(), "Service timeout '" + celletName + "'");
            return null;
        }
    }

    private JSONObject createPerformer(long sn) {
        JSONObject json = new JSONObject();
        try {
            json.put("sn", sn);
            json.put("ts", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private ActionDialect processResponse(ActionDialect response) {
        response.addParam("state", StateCode.makeState(StateCode.OK, "OK"));
        return response;
    }

    public void addTickable(Tickable tickable) {
        if (!this.tickableList.contains(tickable)) {
            return;
        }

        this.tickableList.add(tickable);
    }

    /**
     * Tick 回调。
     *
     * @param now
     */
    @Override
    public void onTick(long now) {
        for (Tickable tickable : this.tickableList) {
            tickable.onTick(now);
        }
    }

    @Override
    public void onListened(Speakable speakable, String celletName, Primitive primitive) {
        try {
            ActionDialect actionDialect = new ActionDialect(primitive);
            if (actionDialect.containsParam(this.performerKey)) {
                JSONObject performer = actionDialect.getParamAsJson(this.performerKey);
                Long sn = performer.getLong("sn");
                Block block = this.blockMap.remove(sn);
                if (null != block) {
                    block.dialect = actionDialect;
                    // 唤醒阻塞的线程
                    synchronized (block) {
                        block.notify();
                    }
                }
                else {
                    Transmission transmission = this.transmissionMap.get(sn);
                    if (null != transmission) {
                        // 移除 P-KEY
                        actionDialect.removeParam(this.performerKey);
                        // 向客户端发送数据
                        transmission.cellet.speak(transmission.source,
                                this.processResponse(actionDialect));
                    }
                }
            }
            else if (actionDialect.containsParam(this.directorKey)) {
                JSONObject director = actionDialect.getParamAsJson(this.directorKey);
                // 移除 D-KEY
                actionDialect.removeParam(this.directorKey);

                Long id = director.getLong("id");
                String domain = director.getString("domain");
                Device device = director.has("device") ? new Device(director.getJSONObject("device")) : null;

                // 联系人 KEY
                String key = UniqueKey.make(id, domain);

                Contact contact = this.onlineContacts.get(key);
                Cellet cellet = this.celletService.getCellet(celletName);

//                Logger.d(this.getClass(), "#" + celletName + " - " + contact.getId() + " - " + actionDialect.getName()
//                        + " - " + actionDialect.getParamAsJson("data").toString());

                if (null != contact && null != cellet) {
                    if (null == device) {
                        // 没有指定设备，进行广播
                        for (Device dev : contact.getDeviceList()) {
                            if (dev.isOnline()) {
                                cellet.speak(dev.getTalkContext(), this.processResponse(actionDialect));
                            }
                        }
                    }
                    else {
                        // 指定设备
                        Device dev = contact.getDevice(device);
                        if (null != dev && dev.isOnline()) {
                            cellet.speak(dev.getTalkContext(), this.processResponse(actionDialect));
                        }
                    }
                }
                else {
                    Logger.w(this.getClass(), "Can NOT find online contact or cellet : " + key + " | " + celletName);
                }
            }
            else {
                PerformerListener listener = this.listenerMap.get(celletName);
                if (null != listener) {
                    listener.onReceived(celletName, primitive);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speakable) {
        Logger.i(this.getClass(), "Contacted " + speakable.getRemoteAddress().getHostString() + ":" +
                speakable.getRemoteAddress().getPort());
    }

    @Override
    public void onQuitted(Speakable speakable) {
        Logger.i(this.getClass(), "Quitted " + speakable.getRemoteAddress().getHostString() + ":" +
                speakable.getRemoteAddress().getPort());
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {

    }


    /**
     * 阻塞信息描述。
     */
    public class Block {

        public Long sn;

        public ActionDialect dialect;

        public Block(Long sn) {
            this.sn = sn;
        }
    }

    /**
     * 用于记录客户端经过网关通信后对应的由服务节点回送的数据的映射关系。
     */
    public class Transmission {

        public Long sn;

        public Cellet cellet;

        public TalkContext source;

        public long timestamp;

        public Transmission(Long sn, Cellet cellet, TalkContext source) {
            this.sn = sn;
            this.cellet = cellet;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
