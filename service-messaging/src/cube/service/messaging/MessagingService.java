/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
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
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.UniqueKey;
import cube.common.action.MessagingAction;
import cube.common.entity.*;
import cube.common.state.MessagingStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactManager;
import cube.service.fileprocessor.FileProcessorService;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
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
    private long recallLimited = 3L * 60 * 1000;

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
        // 读取存储配置
        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(MessagingService.NAME)) {
            config = config.getJSONObject(MessagingService.NAME);
            if (config.getString("type").equalsIgnoreCase("SQLite")) {
                this.storage = new MessagingStorage(this.executor, StorageType.SQLite, config);
            } else {
                this.storage = new MessagingStorage(this.executor, StorageType.MySQL, config);
            }
        } else {
            config.put("file", "storage/MessagingService.db");
            this.storage = new MessagingStorage(this.executor, StorageType.SQLite, config);
        }

        (new Thread() {
            @Override
            public void run() {
                // 打开存储器
                storage.open();

                // 存储进行自校验
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                storage.execSelfChecking(authService.getDomainList());
            }
        }).start();
    }

    private void initPlugin() {
        (new Thread() {
            @Override
            public void run() {
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                PluginSystem<?> pluginSystem = authService.getPluginSystem();
                while (null == pluginSystem) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    pluginSystem = authService.getPluginSystem();
                }

                pluginSystem.register(AuthServiceHook.CreateDomainApp,
                        new ServicePlugin(MessagingService.this));
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

        this.initPlugin();
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
    public PluginSystem<?> getPluginSystem() {
        return this.pluginSystem;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {
        long DAY30 = 30L * 24 * 60 * 60 * 1000;
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
     * {@inheritDoc}
     */
    @Override
    public Object notify(Object event) {
        JSONObject data = (event instanceof JSONObject) ? (JSONObject) event : null;
        if (null != data && data.has("action")) {
            // 动作
            String action = data.getString("action");

            if (MessagingAction.Push.name.equals(action)) {
                Message message = new Message(data.getJSONObject("message"));
                Device device = new Device(data.getJSONObject("device"));
                PushResult result = this.pushMessage(message, device);
                return result.toJSON();
            }
            else if (MessagingAction.Pull.name.equals(action)) {
                String domain = data.getString("domain");
                long beginning = data.getLong("beginning");
                long ending = data.getLong("ending");
                if (data.has("contactId")) {
                    Long contactId = data.getLong("contactId");
                    List<Message> list = this.pullMessage(domain, contactId, beginning, ending);
                    JSONArray array = new JSONArray();

                    for (Message message : list) {
                        array.put(message.toJSON());
                    }

                    data.put("list", array);
                    return data;
                }
            }
            else if (MessagingAction.Read.name.equals(action)) {
                String domain = data.getString("domain");
                Long receiverId = data.getLong("to");
                Long senderId = data.getLong("from");
                JSONArray idArray = data.getJSONArray("list");
                List<Long> messageIds = new ArrayList<>(idArray.length());
                for (int i = 0; i < idArray.length(); ++i) {
                    messageIds.add(idArray.getLong(i));
                }
                // 标记消息已读
                List<Message> messageList = this.markReadMessagesByContact(domain, receiverId, senderId, messageIds);
                // 将标记的消息返回
                JSONArray messageArray = new JSONArray();
                for (Message message : messageList) {
                    messageArray.put(message.toCompactJSON());
                }
                data.put("messages", messageArray);
                return data;
            }
        }

        return null;
    }

    /**
     * 将指定消息实体进行推送处理。
     *
     * @param message
     * @param sourceDevice
     * @return
     */
    public PushResult pushMessage(Message message, Device sourceDevice) {
        // 记录时间戳
        message.setRemoteTimestamp(System.currentTimeMillis());

        // 设置消息的来源设备
        message.setSourceDevice(sourceDevice);

        // 处理附件
        if (!this.processAttachment(message)) {
            // 更新状态
            message.setState(MessageState.Fault);
            return new PushResult(message, MessagingStateCode.AttachmentError);
        }

        // 依照作用域进行投送
        if (message.getScope() == MessageScope.Unlimited) {
            // 一般作用域

            if (message.getTo().longValue() > 0) {
                // 检查发送目标是否被发件人阻止
                boolean blocked = ContactManager.getInstance().hasBlocked(message.getDomain().getName(),
                        message.getFrom(), message.getTo());
                if (blocked) {
                    // 发件人阻止了目标联系人
                    message.setState(MessageState.SendBlocked);
                    return new PushResult(message, MessagingStateCode.BeBlocked);
                }

                // 检查是否被 To 阻止
                blocked = ContactManager.getInstance().hasBlocked(message.getDomain().getName(),
                        message.getTo(), message.getFrom());
                if (blocked) {
                    // 设置状态
                    message.setState(MessageState.ReceiveBlocked);
                    return new PushResult(message, MessagingStateCode.BeBlocked);
                }

                // 更新状态
                message.setState(MessageState.Sent);

                // Hook PrePush
                MessagingHook hook = this.pluginSystem.getPrePushHook();
                hook.apply(new MessagingPluginContext(message));

                String fromKey = UniqueKey.make(message.getFrom(), message.getDomain());
                String toKey = UniqueKey.make(message.getTo(), message.getDomain());

                // 创建副本
                Message[] copies = this.makeMessageCopies(message);
                Message fromCopy = copies[0];
                Message toCopy = copies[1];

                // 将消息写入 TO 缓存
                this.messageCache.add(toKey, toCopy.toJSON(), toCopy.getRemoteTimestamp());
                // 将消息写入 FROM 缓存
                this.messageCache.add(fromKey, fromCopy.toJSON(), fromCopy.getRemoteTimestamp());

                // 发布消息事件给 TO
                ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Push.name, toCopy.toJSON());
                this.contactsAdapter.publish(toKey, event.toJSON());

                // 发布消息事件给 FROM
                event = new ModuleEvent(MessagingService.NAME, MessagingAction.Push.name, fromCopy.toJSON());
                this.contactsAdapter.publish(fromKey, event.toJSON());

                // TO 副本写入存储
                this.storage.write(toCopy);
                // FROM 副本写入存储
                this.storage.write(fromCopy);

                // 触发 Hook
                MessagingHook writeHook = this.pluginSystem.getWriteMessageHook();
                writeHook.apply(new MessagingPluginContext(toCopy));
                writeHook.apply(new MessagingPluginContext(fromCopy));

                // 在内存里记录状态
                this.messageStateMap.put(new MessageKey(toCopy.getOwner(), toCopy.getId()),
                        new MessageStateBundle(toCopy.getId(), toCopy.getOwner(), MessageState.Sent));
                this.messageStateMap.put(new MessageKey(fromCopy.getOwner(), fromCopy.getId()),
                        new MessageStateBundle(fromCopy.getId(), fromCopy.getOwner(), MessageState.Sent));

                // 更新会话
                this.updateConversations(fromCopy, toCopy);

                // Hook PostPush
                hook = this.pluginSystem.getPostPushHook();
                hook.apply(new MessagingPluginContext(message));

                return new PushResult(message, MessagingStateCode.Ok);
            }
            else if (message.getSource().longValue() > 0) {
                // 进行消息的群组管理
                Group group = ContactManager.getInstance().getGroup(message.getSource(), message.getDomain().getName());
                if (null != group) {
                    if (group.getState() == GroupState.Normal) {
                        // 群组状态正常

                        // 更新状态
                        message.setState(MessageState.Sent);

                        // Hook PrePush
                        MessagingHook hook = this.pluginSystem.getPrePushHook();
                        hook.apply(new MessagingPluginContext(message));

                        Long senderId = message.getFrom();
                        List<Long> list = group.getMembers();
                        for (Long contactId : list) {
                            if (contactId.longValue() == senderId.longValue()) {
                                // 跳过发件人
                                continue;
                            }

                            // 创建 TO 副本
                            Message copy = new Message(message);
                            // 更新 To 数据
                            copy.setTo(contactId);
                            // 设置 Owner
                            copy.setOwner(contactId);

                            // 将消息写入缓存
                            // 写入 TO
                            String toKey = UniqueKey.make(contactId, message.getDomain());
                            this.messageCache.add(toKey, copy.toJSON(), copy.getRemoteTimestamp());

                            // 发布给 TO
                            ModuleEvent event = new ModuleEvent(MessagingService.NAME,
                                    MessagingAction.Push.name, copy.toJSON());
                            this.contactsAdapter.publish(toKey, event.toJSON());

                            // 写入存储
                            this.storage.write(copy);

                            // Hook
                            MessagingHook writeHook = this.pluginSystem.getWriteMessageHook();
                            writeHook.apply(new MessagingPluginContext(copy));

                            // 在内存里记录状态
                            this.messageStateMap.put(new MessageKey(contactId, copy.getId()),
                                    new MessageStateBundle(copy.getId(), contactId, MessageState.Sent));

                            // 更新会话
                            this.updateConversation(group, copy);
                        }

                        // 创建 FROM 副本
                        Message copy = new Message(message);
                        copy.setOwner(message.getFrom());

                        // 写入 FROM
                        String fromKey = UniqueKey.make(copy.getFrom(), message.getDomain());
                        this.messageCache.add(fromKey, copy.toJSON(), copy.getRemoteTimestamp());

                        // 发布给 FROM
                        ModuleEvent event = new ModuleEvent(MessagingService.NAME,
                                MessagingAction.Push.name, copy.toJSON());
                        this.contactsAdapter.publish(fromKey, event.toJSON());

                        // 写入存储
                        this.storage.write(copy);

                        // Hook
                        MessagingHook writeHook = this.pluginSystem.getWriteMessageHook();
                        writeHook.apply(new MessagingPluginContext(copy));

                        // 在内存里记录状态
                        this.messageStateMap.put(new MessageKey(copy.getOwner(), copy.getId()),
                                new MessageStateBundle(copy.getId(), copy.getOwner(), MessageState.Sent));

                        // 更新会话
                        this.updateConversation(group, copy);

                        // 更新群组活跃时间
                        ContactManager.getInstance().updateGroupActiveTime(group, message.getRemoteTimestamp());

                        // Hook PostPush
                        hook = this.pluginSystem.getPostPushHook();
                        hook.apply(new MessagingPluginContext(message));

                        return new PushResult(message, MessagingStateCode.Ok);
                    }
                    else {
                        // 群组状态不正常，设置为故障状态
                        message.setState(MessageState.Fault);
                        return new PushResult(message, MessagingStateCode.GroupError);
                    }
                }
                else {
                    // 找不到群组，设置为故障状态
                    message.setState(MessageState.Fault);
                    return new PushResult(message, MessagingStateCode.NoGroup);
                }
            }
            else {
                // 设置为故障状态
                message.setState(MessageState.Fault);
                return new PushResult(message, MessagingStateCode.DataStructureError);
            }
        }
        else {
            // 仅自己可见

            // 更新状态
            message.setState(MessageState.Sent);

            String fromKey = UniqueKey.make(message.getFrom(), message.getDomain());

            // 创建副本
            Message[] copies = this.makeMessageCopies(message);
            Message fromCopy = copies[0];

            // 将消息写入 FROM 缓存
            this.messageCache.add(fromKey, fromCopy.toJSON(), fromCopy.getRemoteTimestamp());

            // 发布消息事件给 FROM
            ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Push.name, fromCopy.toJSON());
            this.contactsAdapter.publish(fromKey, event.toJSON());

            // FROM 副本写入存储
            this.storage.write(fromCopy);

            // Hook
            MessagingHook writeHook = this.pluginSystem.getWriteMessageHook();
            writeHook.apply(new MessagingPluginContext(fromCopy));

            this.messageStateMap.put(new MessageKey(fromCopy.getOwner(), fromCopy.getId()),
                    new MessageStateBundle(fromCopy.getId(), fromCopy.getOwner(), MessageState.Sent));

            return new PushResult(message, MessagingStateCode.Ok);
        }
    }

    /**
     * 拉取指定时间戳到当前时间段的所有消息内容。
     *
     * @param domain
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
        if (null != list) {
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
        }

        // 如果缓存里没有数据，从存储里读取
        if (result.isEmpty()) {
            List<Message> messageList = this.storage.readOrderByTime(domain, contactId, beginningTime, endingTime);
            result.addAll(messageList);
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

        // 从存储器里读取出该 ID 的所有消息
        List<Message> msgList = this.storage.read(domain, messageId);
        for (Message msg : msgList) {
            // 更新内存里的数据
            MessageKey messageKey = new MessageKey(msg.getOwner(), messageId);
            MessageStateBundle stateBundle = this.messageStateMap.get(messageKey);
            if (null != stateBundle) {
                stateBundle.state = MessageState.Recalled;
            }

            String copyKey = UniqueKey.make(msg.getOwner(), domain);
            // 发布 Recall 动作
            ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingAction.Recall.name, msg.toCompactJSON());
            this.contactsAdapter.publish(copyKey, event.toJSON());
        }

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
        MessageKey messageKey = new MessageKey(contactId, messageId);

        MessageStateBundle stateBundle = this.messageStateMap.get(messageKey);
        if (null != stateBundle) {
            stateBundle.state = MessageState.Deleted;
        }

        this.storage.writeMessageState(domain, contactId, messageId, MessageState.Deleted);
    }

    /**
     * 获取紧凑结构的消息。
     *
     * @param domain
     * @param contactId
     * @param messageId
     * @return
     */
    public Message getCompactMessage(String domain, Long contactId, Long messageId) {
        return this.storage.readCompact(domain, contactId, messageId);
    }

    /**
     * 标记消息已读。将 TO 的消息副本标记为已读状态。
     * 修改之后将该状态通知给消息的发送人。
     *
     * @param domain
     * @param contactId
     * @param messageId
     * @return
     */
    public Message markReadMessage(String domain, Long contactId, Long messageId) {
        Message message = this.storage.readCompact(domain, contactId, messageId);
        if (null == message) {
            // 没有找到消息
            return null;
        }

        // 修改状态
        MessageKey key = new MessageKey(contactId, messageId);
        MessageStateBundle stateBundle = this.messageStateMap.get(key);
        if (null != stateBundle) {
            stateBundle.state = MessageState.Read;
        }

        // 修改状态
        message.setState(MessageState.Read);

        // 更新存储
        this.storage.writeMessageState(domain, contactId, messageId, MessageState.Read);

        if (!message.isFromGroup() && message.getFrom().longValue() != contactId.longValue()) {
            // 获取发件人侧的消息
            Message senderMessage = this.storage.readCompact(domain, message.getFrom(), messageId);
            if (null != senderMessage && senderMessage.getState() == MessageState.Sent) {
                // 修改状态
                key = new MessageKey(message.getFrom(), messageId);
                stateBundle = this.messageStateMap.get(key);
                if (null != stateBundle) {
                    stateBundle.state = MessageState.Read;
                }

                // 更新存储
                this.storage.writeMessageState(domain, message.getFrom(), messageId, MessageState.Read);

                // 修改状态
                senderMessage.setState(MessageState.Read);

                // 通知发件人
                String fromKey = UniqueKey.make(message.getFrom(), domain);
                ModuleEvent event = new ModuleEvent(MessagingService.NAME,
                        MessagingAction.Read.name, senderMessage.toCompactJSON());
                this.contactsAdapter.publish(fromKey, event.toJSON());
            }
        }

        return message;
    }

    /**
     * 标记消息为已读。
     *
     * @param domain
     * @param contactId
     * @param fromId
     * @param messageIdList
     * @return
     */
    public List<Message> markReadMessagesByContact(String domain,
                                                   Long contactId, Long fromId, List<Long> messageIdList) {
        for (Long messageId : messageIdList) {
            // 修改状态
            MessageKey key = new MessageKey(contactId, messageId);
            MessageStateBundle stateBundle = this.messageStateMap.get(key);
            if (null != stateBundle) {
                stateBundle.state = MessageState.Read;
            }
        }

        // 更新存储
        this.storage.writeMessagesState(domain, contactId, messageIdList, MessageState.Read);

        // 将发件人的消息也标记为已读
        List<Long> validIdList = this.storage.writeMessagesState(domain,
                fromId, messageIdList, MessageState.Sent, MessageState.Read);

        List<Message> messageList = new ArrayList<>(validIdList.size());
        for (Long messageId : validIdList) {
            // 更新状态
            MessageKey key = new MessageKey(fromId, messageId);
            MessageStateBundle stateBundle = this.messageStateMap.get(key);
            if (null != stateBundle && stateBundle.state == MessageState.Sent) {
                stateBundle.state = MessageState.Read;
            }

            // 读取数据
            Message message = this.storage.readCompact(domain, fromId, messageId);
            if (null != message) {
                messageList.add(message);
            }
        }

        // 通知发件人
        Contact sender = ContactManager.getInstance().getContact(domain, fromId);
        notifyContactMessageRead(sender, messageList);
        return messageList;
    }

    private void notifyContactMessageRead(Contact contact, List<Message> messageList) {
        String uniqueKey = UniqueKey.make(contact.getId(), contact.getDomain().getName());
        for (Message message : messageList) {
            ModuleEvent event = new ModuleEvent(MessagingService.NAME,
                    MessagingAction.Read.name, message.toCompactJSON());
            this.contactsAdapter.publish(uniqueKey, event.toJSON());
        }
    }

    /**
     * 标记消息为已读。
     *
     * @param domain
     * @param contactId
     * @param groupId
     * @param messageIdList
     */
    public void markReadMessagesByGroup(String domain, Long contactId, Long groupId, List<Long> messageIdList) {
        for (Long messageId : messageIdList) {
            // 修改状态
            MessageKey key = new MessageKey(contactId, messageId);
            MessageStateBundle stateBundle = this.messageStateMap.get(key);
            if (null != stateBundle) {
                stateBundle.state = MessageState.Read;
            }
        }

        // 更新存储
        this.storage.writeMessagesState(domain, contactId, messageIdList, MessageState.Read);
    }

    /**
     * 查询指定群组在指定时间范围内的消息。
     *
     * @param group
     * @param beginningTime
     * @param endingTime
     * @return
     */
    public List<Message> queryGroupMessages(Group group, long beginningTime, long endingTime) {
        List<Message> list = this.storage.readWithGroupOrderByTime(group.getDomain().getName(),
                group.getId(), beginningTime, endingTime);

        // 按照时间升序排序
        Collections.sort(list);

        for (Message message : list) {
            message.setOwner(message.getFrom());
        }

        return list;
    }

    /**
     * 获取指定联系人的最近会话清单。
     *
     * @param contact
     * @return 没有找到数据时返回 {@code null} 值。
     */
    public List<Conversation> getRecentConversations(Contact contact) {
        List<Conversation> list = this.storage.readConversationByDescendingOrder(contact);
        if (null == list) {
            return null;
        }

        // 将 Important 置顶
        list.sort(new Comparator<Conversation>() {
            @Override
            public int compare(Conversation c1, Conversation c2) {
                if (c1.getState().code == c2.getState().code) {
                    // 相同状态，判断时间戳
                    if (c1.getTimestamp() < c2.getTimestamp()) {
                        return 1;
                    }
                    else if (c1.getTimestamp() > c2.getTimestamp()) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                }
                else {
                    if (c1.getState() == ConversationState.Normal && c2.getState() == ConversationState.Important) {
                        // 交换顺序，把 c2 排到前面
                        return 1;
                    }
                    else if (c1.getState() == ConversationState.Important && c2.getState() == ConversationState.Normal) {
                        return -1;
                    }
                }

                return 0;
            }
        });

        // 判断会话是否需要读取未读信息
        for (Conversation conversation : list) {
            if (conversation.getRemindType() == ConversationRemindType.Normal
                    || conversation.getRemindType() == ConversationRemindType.Closed) {
                int unread = this.storage.countUnread(conversation);
                conversation.setUnreadCount(unread);
            }
        }

        return list;
    }

    /**
     * 更新指定的会话。
     *
     * @param conversation 指定待更新会话。
     */
    public void updateConversation(Conversation conversation) {
        conversation.resetTimestamp();
        this.storage.updateConversation(conversation);
    }

    /**
     * 制作消息的 From 和 To 的两个副本。
     *
     * @param message
     * @return
     */
    private Message[] makeMessageCopies(Message message) {
        Message[] result = new Message[2];

        Message fromCopy = new Message(message);
        fromCopy.setOwner(message.getFrom());
        result[0] = fromCopy;

        Message toCopy = new Message(message);
        toCopy.setOwner(message.getTo());
        result[1] = toCopy;

        return result;
    }

    private boolean notifyMessage(MessagingAction action, TalkContext talkContext, Long contactId, Message message) {
        if (null == talkContext) {
            return false;
        }

        JSONObject payload = new JSONObject();
        payload.put("code", MessagingStateCode.Ok.code);
        payload.put("data", message.toJSON());

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
            Logger.d(this.getClass(), "Process attachment : " + message.getFrom() + " - "
                    + message.getAttachment().getFileCode(0));
        }

        FileStorageService fileStorageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);

        String domainName = message.getDomain().getName();

        for (int i = 0, length = fileAttachment.numFiles(); i < length; ++i) {
            int count = 12;
            while (!fileStorageService.existsFileData(fileAttachment.getFileCode(i))) {
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

//        if (Logger.isDebugLevel()) {
//            Logger.d(this.getClass(), "Process attachment [File Exists] : " + message.getFrom() + " - "
//                    + message.getAttachment().getFileCode());
//        }

            // 获取文件标签
            FileLabel fileLabel = fileStorageService.getFile(domainName, fileAttachment.getFileCode(i));
            if (null == fileLabel) {
                Logger.e(this.getClass(), "Can NOT find file label : " + fileAttachment.getFileLabel(i));
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
                } else {
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
            fileAttachment.addFileLabel(fileLabel);

//        if (Logger.isDebugLevel()) {
//            Logger.d(this.getClass(), "Process attachment [Set file label] : " + message.getFrom() + " - "
//                    + message.getAttachment().getFileCode());
//        }

            // 是否生成缩略图
            boolean genThumb = fileAttachment.isImageType(i);
            if (genThumb && this.getKernel().hasModule(FileProcessorService.NAME)) {
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "Make thumb : " + message.getFrom() + " - "
                            + fileAttachment.getFileCode(i));
                }

                int quality = 60;

                FileProcessorService processor = (FileProcessorService) this.getKernel().getModule(
                        FileProcessorService.NAME);

                // 生成缩略图
                FileThumbnail thumbnail = processor.makeThumbnail(domainName, fileLabel, quality);

                // 将缩略图作为文件标签的上下文数据
                if (null != thumbnail) {
                    fileLabel.setContext(thumbnail.toJSON());
                }
            }
        }

        return true;
    }

    private void updateConversations(Message fromCopy, Message toCopy) {
        this.storage.updateConversation(fromCopy.getDomain().getName(), fromCopy.getOwner(),
                fromCopy.getTo(), fromCopy.getId(),
                fromCopy.getRemoteTimestamp(), ConversationType.Contact,
                ConversationState.Normal, ConversationRemindType.Normal);

        this.storage.updateConversation(toCopy.getDomain().getName(), toCopy.getOwner(),
                toCopy.getFrom(), toCopy.getId(),
                toCopy.getRemoteTimestamp(), ConversationType.Contact,
                ConversationState.Normal, ConversationRemindType.Normal);
    }

    private void updateConversation(Group group, Message message) {
        this.storage.updateConversation(message.getDomain().getName(), message.getOwner(),
                group.getId(), message.getId(),
                message.getRemoteTimestamp(), ConversationType.Group,
                ConversationState.Normal, ConversationRemindType.Normal);
    }

    protected void refreshDomain(AuthDomain authDomain) {
        List<String> list = new ArrayList<>();
        list.add(authDomain.domainName);
        this.storage.execSelfChecking(list);
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (MessagingService.NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {
            // 消息模块

            ModuleEvent event = new ModuleEvent(jsonObject);
            if (event.getEventName().equals(MessagingAction.Push.name)) {
                Message message = new Message(event.getData());

                Long ownerId = message.getOwner();

                // 判断是否是推送给 TO 的消息还是推送给 FROM
                if (ownerId.longValue() == message.getTo().longValue()) {
                    // 将消息发送给目标设备
                    Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain().getName(),
                            message.getTo());
                    if (null != contact) {
                        for (Device device : contact.getDeviceList()) {
                            TalkContext talkContext = device.getTalkContext();
                            if (notifyMessage(MessagingAction.Notify, talkContext, ownerId, message)) {
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
                else if (ownerId.longValue() == message.getFrom().longValue()) {
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
                            if (notifyMessage(MessagingAction.Notify, talkContext, ownerId, message)) {
                                Logger.d(this.getClass(), "Notify message to other device: '"
                                        + message.getFrom() + "' -> '" + message.getTo() + "'");
                            }
                        }
                    }
                }
            }
            else if (event.getEventName().equals(MessagingAction.Read.name)) {
                Message message = new Message(event.getData());

                // 获取发件人
                Long fromId = message.getFrom();

                Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain().getName(), fromId);
                if (null != contact) {
                    for (Device device : contact.getDeviceList()) {
                        if (notifyMessage(MessagingAction.Read, device.getTalkContext(), fromId, message)) {
                            Logger.d(this.getClass(), "Mark read message : '"
                                    + message.getTo() + "' mark - from '" + fromId + "'");
                        }
                    }
                }
            }
            else if (event.getEventName().equals(MessagingAction.Recall.name)) {
                Message message = new Message(event.getData());

                Long ownerId = message.getOwner();

                Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain().getName(), ownerId);
                if (null != contact) {
                    for (Device device : contact.getDeviceList()) {
                        if (notifyMessage(MessagingAction.Recall, device.getTalkContext(), ownerId, message)) {
                            Logger.d(this.getClass(), "Recall message : '" + message.getId()
                                    + "' - '" + ownerId + "'");
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
