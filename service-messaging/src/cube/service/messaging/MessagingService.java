/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.service.messaging;

import cell.adapter.CelletAdapter;
import cell.adapter.CelletAdapterFactory;
import cell.adapter.CelletAdapterListener;
import cell.adapter.extra.timeseries.SeriesItem;
import cell.adapter.extra.timeseries.SeriesMemory;
import cell.adapter.extra.timeseries.SeriesMemoryConfig;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.UniqueKey;
import cube.common.action.MessagingAction;
import cube.common.entity.*;
import cube.common.state.MessagingStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.storage.StorageType;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 消息管理器。
 */
public final class MessagingService extends AbstractModule implements CelletAdapterListener {

    public final static String NAME = "Messaging";

    private final static String HIDDEN_DIR = "_CubeMessaging_";

    private MessagingServiceCellet cellet;

    /**
     * 多线程执行器。
     */
    private ExecutorService executor;

    /**
     * 消息缓存器。
     */
    private SeriesMemory messageCache;

    /**
     * 消息状态映射。
     */
    private ConcurrentHashMap<MessageKey, MessageStateBundle> messageStateMap;

    /**
     * 消息存储。
     */
    private MessagingStorage storage;

    /**
     * 联系人事件适配器。
     */
    private CelletAdapter contactsAdapter;

    /**
     * 召回消息的时间限制。
     */
    private long recallLimited = 2L * 60L * 1000L;

    /**
     * 消息模块的插件系统。
     */
    private MessagingPluginSystem pluginSystem;

    /**
     * 构造函数。
     *
     * @param cellet
     */
    public MessagingService(MessagingServiceCellet cellet) {
        this.cellet = cellet;
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(32);
        this.messageStateMap = new ConcurrentHashMap<>();
    }

    private void initMessageCache() {
        String filepath = "config/messaging-series-memory.properties";
        File file = new File(filepath);
        if (!file.exists()) {
            filepath = "messaging-series-memory.properties";
        }

        SeriesMemoryConfig config = new SeriesMemoryConfig(filepath);
        this.messageCache = new SeriesMemory(config);

        this.messageCache.start();

        int count = 10;
        Logger.i(this.getClass(), "Waiting for message cache ready");
        while (!this.messageCache.isReady()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            --count;
            if (count == 0) {
                break;
            }
        }
    }

    private void initMessageStorage() {
        JSONObject config = new JSONObject();
        try {
            config.put("file", "storage/MessagingService.db");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.storage = new MessagingStorage(this.executor, StorageType.SQLite, config);

        this.storage.open();

        (new Thread() {
            @Override
            public void run() {
                // 存储进行自校验
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                storage.execSelfChecking(authService.getDomainList());
            }
        }).start();
    }

    /**
     * 启动服务。
     */
    @Override
    public void start() {
        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);

        this.pluginSystem = new MessagingPluginSystem();

        this.initMessageCache();

        this.initMessageStorage();
    }

    /**
     * 停止服务。
     */
    @Override
    public void stop() {
        this.messageCache.stop();

        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }

        this.storage.close();
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
        long DAY30 = 30 * 24 * 60 * 60 * 1000L;
        long now = System.currentTimeMillis();
        long delta = now - DAY30;

        Iterator<MessageStateBundle> msbiter = this.messageStateMap.values().iterator();
        while (msbiter.hasNext()) {
            MessageStateBundle messageStateBundle = msbiter.next();
            if (messageStateBundle.timestamp < delta) {
                msbiter.remove();
            }
        }
    }

    /**
     * 将指定消息实体进行推送处理。
     * 
     * @param message
     * @param sourceDevice
     * @return
     */
    public Message pushMessage(Message message, Device sourceDevice) {
        // 记录时间戳
        message.setRemoteTimestamp(System.currentTimeMillis());

        // 设置消息的来源设备
        message.setSourceDevice(sourceDevice);

        // 处理附件
        if (!this.processAttachment(message)) {
            // 更新状态
            message.setState(MessageState.Fault);
            return null;
        }

        // 更新状态
        message.setState(MessageState.Sent);

        // Hook PrePush
        MessagingHook hook = this.pluginSystem.getPrePushHook();
        hook.apply(message);

        if (message.getTo().longValue() > 0) {
            String toKey = UniqueKey.make(message.getTo(), message.getDomain());
            String fromKey = UniqueKey.make(message.getFrom(), message.getDomain());

            // 将消息写入缓存
            // 写入 TO
            this.messageCache.add(toKey, message.toJSON(), message.getRemoteTimestamp());
            // 写入 FROM
            this.messageCache.add(fromKey, message.toJSON(), message.getRemoteTimestamp());

            // 发布消息事件
            ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Push.name, message.toJSON());
            // 发布给 TO
            this.contactsAdapter.publish(toKey, event.toJSON());
            // 发布给 FROM
            this.contactsAdapter.publish(fromKey, event.toJSON());

            // 写入存储
            this.storage.write(message);

            // 在内存里记录状态
            this.messageStateMap.put(new MessageKey(message.getTo(), message.getId()),
                    new MessageStateBundle(message.getId(), message.getTo(), MessageState.Sent));
            this.messageStateMap.put(new MessageKey(message.getFrom(), message.getId()),
                    new MessageStateBundle(message.getId(), message.getFrom(), MessageState.Sent));
        }
        else if (message.getSource().longValue() > 0) {
            // 进行消息的群组管理
            Group group = ContactManager.getInstance().getGroup(message.getSource(), message.getDomain().getName());
            if (null != group) {
                if (group.getState() == GroupState.Normal) {
                    Long senderId = message.getFrom();
                    List<Contact> list = group.getMembers();
                    for (Contact contact : list) {
                        if (contact.getId().longValue() == senderId.longValue()) {
                            // 跳过发件人
                            continue;
                        }

                        // 创建副本
                        Message copy = new Message(message);
                        // 更新 To 数据
                        copy.setTo(contact.getId());

                        // 将消息写入缓存
                        // 写入 TO
                        String toKey = UniqueKey.make(contact.getId(), contact.getDomain());
                        this.messageCache.add(toKey, copy.toJSON(), copy.getRemoteTimestamp());

                        // 发布给 TO
                        ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Push.name, copy.toJSON());
                        this.contactsAdapter.publish(toKey, event.toJSON());

                        // 写入存储
                        this.storage.write(copy);

                        // 在内存里记录状态
                        this.messageStateMap.put(new MessageKey(contact.getId(), copy.getId()),
                                new MessageStateBundle(copy.getId(), copy.getTo(), MessageState.Sent));
                    }

                    // 写入 FROM
                    String fromKey = UniqueKey.make(message.getFrom(), message.getDomain());
                    this.messageCache.add(fromKey, message.toJSON(), message.getRemoteTimestamp());

                    // 发布给 FROM
                    ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Push.name, message.toJSON());
                    this.contactsAdapter.publish(fromKey, event.toJSON());

                    // 写入存储
                    this.storage.write(message);

                    // 在内存里记录状态
                    this.messageStateMap.put(new MessageKey(message.getFrom(), message.getId()),
                            new MessageStateBundle(message.getId(), message.getFrom(), MessageState.Sent));

                    // 更新群组活跃时间
                    ContactManager.getInstance().updateGroupActiveTime(group, message.getRemoteTimestamp());
                }
                else {
                    // 群组状态不正常，设置为故障状态
                    message.setState(MessageState.Fault);
                }
            }
            else {
                // 找不到群组，设置为故障状态
                message.setState(MessageState.Fault);
            }
        }
        else {
            // 设置为故障状态
            message.setState(MessageState.Fault);
        }

        // Hook PostPush
        hook = this.pluginSystem.getPostPushHook();
        hook.apply(message);

        return message;
    }

    /**
     * 拉取指定时间戳到当前时间段的所有消息内容。
     *
     * @param contactId
     * @param beginningTime
     * @param endingTime
     * @return
     */
    public List<Message> pullMessage(String domain, Long contactId, long beginningTime, long endingTime) {
        LinkedList<Message> result = new LinkedList<>();

        String key = UniqueKey.make(contactId, domain);

        // 从缓存里读取数据
        List<SeriesItem> list = this.messageCache.query(key, beginningTime, endingTime);
        for (SeriesItem item : list) {
            Message message = new Message(item.data);
            MessageState state = message.getState();

            MessageKey messageKey = new MessageKey(contactId, message.getId());

            MessageStateBundle msb = this.messageStateMap.get(messageKey);
            if (null != msb) {
                state = msb.state;
            }
            else {
                MessageState ms = this.storage.readMessageState(domain, contactId, message.getId());
                if (null != ms) {
                    this.messageStateMap.put(messageKey, new MessageStateBundle(message.getId(), contactId, ms));
                    state = ms;
                }
            }

            if (state == MessageState.Sent || state == MessageState.Read) {
                // 重置状态
                message.setState(state);
                result.add(message);
            }
        }

        // 如果缓存里没有数据，从存储里读取
        if (result.isEmpty()) {
            List<Message> messageList1 = this.storage.readWithToOrderByTime(domain, contactId, beginningTime, endingTime);
            List<Message> messageList2 = this.storage.readWithFromOrderByTime(domain, contactId, beginningTime, endingTime);
            result.addAll(messageList1);
            result.addAll(messageList2);
        }

        // 按照时间戳升序排序
        Collections.sort(result);

        return result;
    }

    /**
     * 撤回消息。
     *
     * @param domain
     * @param fromId
     * @param messageId
     * @return
     */
    public boolean recallMessage(String domain, Long fromId, Long messageId) {
        long now = System.currentTimeMillis();
        String key = UniqueKey.make(fromId, domain);

        // 查询时限内的消息
        List<SeriesItem> list = this.messageCache.query(key, now - this.recallLimited, now);
        if (list.isEmpty()) {
            // 没有找到消息
            return false;
        }

        Message message = null;
        for (SeriesItem item : list) {
            Message msg = new Message(item.data);
            if (msg.getFrom().longValue() == fromId.longValue()
                    && msg.getId().longValue() == messageId.longValue()) {
                // 找到指定的消息
                message = msg;
                break;
            }
        }

        if (null == message) {
            // 没有找到消息
            return false;
        }

        // 更新消息状态
        message.setState(MessageState.Recalled);

        // 修改该 ID 的所有消息的状态
        this.storage.writeMessageState(domain, messageId, MessageState.Recalled);

        // 更新内存里的数据
        MessageKey messageKey = new MessageKey(fromId, messageId);
        MessageStateBundle stateBundle = this.messageStateMap.get(messageKey);
        if (null != stateBundle) {
            stateBundle.state = MessageState.Recalled;
        }

        // 从存储器里读取出该 ID 的所有消息
        List<Message> msgList = this.storage.read(domain, messageId);
        for (Message msg : msgList) {
            if (msg.getTo().longValue() == 0) {
                continue;
            }

            // 更新内存里的数据
            messageKey = new MessageKey(msg.getTo(), messageId);
            stateBundle = this.messageStateMap.get(messageKey);
            if (null != stateBundle) {
                stateBundle.state = MessageState.Recalled;
            }

            String toKey = UniqueKey.make(msg.getTo(), domain);
            // 发布给 TO Recall 动作
            ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Recall.name, msg.toCompactJSON());
            this.contactsAdapter.publish(toKey, event.toJSON());
        }

        String fromKey = UniqueKey.make(fromId, domain);
        // 发布给 FROM Recall 动作
        ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Recall.name, message.toCompactJSON());
        this.contactsAdapter.publish(fromKey, event.toJSON());

        return true;
    }

    /**
     * 删除消息。
     *
     * @param domain
     * @param contactId
     * @param messageId
     */
    public void deleteMessage(String domain, Long contactId, Long messageId) {
        String key = UniqueKey.make(contactId, domain);

        MessageStateBundle stateBundle = this.messageStateMap.get(key);
        if (null != stateBundle) {
            stateBundle.state = MessageState.Deleted;
        }

        this.storage.writeMessageState(domain, contactId, messageId, MessageState.Deleted);
    }

    private boolean notifyMessage(MessagingAction action, TalkContext talkContext, Long contactId, Message message) {
        if (null == talkContext) {
            return false;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("code", MessagingStateCode.Ok.code);
            payload.put("data", message.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet packet = new Packet(action.name, payload);
        ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                contactId.longValue(), message.getDomain().getName());
        this.cellet.speak(talkContext, dialect);
        return true;
    }

    /**
     * 处理消息的附件。
     *
     * @param message
     * @return 返回是否处理成功。
     */
    private boolean processAttachment(Message message) {
        FileAttachment fileAttachment = message.getAttachment();
        if (null == fileAttachment) {
            return true;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Process attachment : " + message.getFrom() + " -> "
                    + message.getAttachment().getFileCode());
        }

        FileStorageService fileStorageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);

        String domainName = message.getDomain().getName();

        int count = 12;
        while (!fileStorageService.existsFile(domainName, fileAttachment.getFileCode())) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            --count;
            if (count <= 0) {
                break;
            }
        }

        // 获取文件标签
        FileLabel fileLabel = fileStorageService.getFile(domainName, fileAttachment.getFileCode());
        if (null == fileLabel) {
            Logger.e(this.getClass(), "Can NOT find file label : " + fileAttachment.getFileLabel());
            return false;
        }

        FileHierarchy fileHierarchy = null;
        if (message.getSource().longValue() > 0) {
            // 存入群组的隐藏目录里

            fileHierarchy = fileStorageService.getFileHierarchy(domainName, message.getSource());
            Directory root = fileHierarchy.getRoot();

            // 将文件存到隐藏目录里
            Directory dir = null;
            if (!root.existsDirectory(HIDDEN_DIR)) {
                dir = root.createDirectory(HIDDEN_DIR);
                dir.setHidden(true);
            }
            else {
                dir = root.getDirectory(HIDDEN_DIR);
            }

            // 添加文件
            dir.addFile(fileLabel);
        }
        else {
            // 存入发件人的隐藏目录里

            fileHierarchy = fileStorageService.getFileHierarchy(domainName, message.getFrom());
            Directory root = fileHierarchy.getRoot();

            Directory dir = root.getDirectory(HIDDEN_DIR);
            if (null == dir) {
                dir = root.createDirectory(HIDDEN_DIR);
                dir.setHidden(true);
            }

            // 添加文件
            dir.addFile(fileLabel);
        }

        // 向消息附件追加文件标签
        fileAttachment.setFileLabel(fileLabel);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Process attachment end : " + message.getFrom() + " -> "
                    + fileAttachment.getFileLabel().getFileCode());
        }

        return true;
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (MessagingService.NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {
            // 消息模块

            // 取主键的 ID
            Long id = UniqueKey.extractId(topic);
            if (null == id) {
                return;
            }

            ModuleEvent event = new ModuleEvent(jsonObject);
            if (event.getEventName().equals(MessagingAction.Push.name)) {
                Message message = new Message(event.getData());

                // 判断是否是推送给 TO 的消息还是推送给 FROM
                if (id.longValue() == message.getTo().longValue()) {
                    // 将消息发送给目标设备
                    Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain().getName(), message.getTo());
                    if (null != contact) {
                        for (Device device : contact.getDeviceList()) {
                            TalkContext talkContext = device.getTalkContext();
                            if (notifyMessage(MessagingAction.Notify, talkContext, id, message)) {
                                Logger.d(this.getClass(), "Notify message: '" + message.getFrom()
                                        + "' -> '" + message.getTo() + "'");
                            }
                            else {
                                Logger.w(this.getClass(), "Notify message error: '" + message.getFrom()
                                        + "' -> '" + message.getTo() + "'");
                            }
                        }
                    }
                }
                else if (id.longValue() == message.getFrom().longValue()) {
                    // 将消息发送给源联系人的其他设备
                    Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain().getName(),
                            message.getFrom());
                    if (null != contact) {
                        for (Device device : contact.getDeviceList()) {
                            if (device.equals(message.getSourceDevice())) {
                                // 跳过源设备
                                continue;
                            }

                            TalkContext talkContext = device.getTalkContext();
                            if (notifyMessage(MessagingAction.Notify, talkContext, id, message)) {
                                Logger.d(this.getClass(), "Notify message to other device: '"
                                        + message.getFrom() + "' -> '" + message.getTo() + "'");
                            }
                        }
                    }
                }
            }
            else if (event.getEventName().equals(MessagingAction.Recall.name)) {
                Message message = new Message(event.getData());
                Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain().getName(), id);
                if (null != contact) {
                    for (Device device : contact.getDeviceList()) {
                        if (notifyMessage(MessagingAction.Recall, device.getTalkContext(), id, message)) {
                            Logger.d(this.getClass(), "Recall message : '" + message.getId() + "' - '" + id + "'");
                        }
                    }
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
}
