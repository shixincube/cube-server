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

package cube.service.contact;

import cell.adapter.CelletAdapter;
import cell.adapter.CelletAdapterFactory;
import cell.adapter.CelletAdapterListener;
import cell.adapter.extra.memory.LockFuture;
import cell.adapter.extra.memory.SharedMemory;
import cell.adapter.extra.memory.SharedMemoryConfig;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Domain;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.UniqueKey;
import cube.common.action.ContactActions;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Group;
import cube.common.entity.GroupState;
import cube.common.state.ContactStateCode;
import cube.core.Kernel;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.storage.StorageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 联系人管理器。
 */
public class ContactManager implements CelletAdapterListener {

    public final static String NAME = "Contact";

    private final static ContactManager instance = new ContactManager();

    /**
     * Cellet 实例。
     */
    private ContactServiceCellet cellet;

    /**
     * 多线程执行器。
     */
    private ExecutorService executor;

    /**
     * 管理线程。
     */
    private ManagementDaemon daemon;

    /**
     * 在线的联系人表。
     */
    protected ConcurrentHashMap<String, ContactTable> onlineTables;

    /**
     * 令牌码对应联系人和设备关系。
     */
    protected ConcurrentHashMap<String, TokenDevice> tokenContactMap;

    /**
     * 处于活跃状态的群组。
     */
    protected ConcurrentHashMap<String, GroupTable> activeGroupTables;

    /**
     * 联系人数据缓存。
     */
    private SharedMemory contactCache;

    /**
     * 群组数据缓存。
     */
    private SharedMemory groupCache;

    /**
     * 联系人存储。
     */
    private ContactStorage storage;

    /**
     * 同步超时时长。
     */
    private long syncTimeout = 5000L;

    /**
     * 联系人事件队列。
     */
    private CelletAdapter contactsAdapter;

    /**
     * 联系人模块的插件系统。
     */
    protected ContactPluginSystem pluginSystem;

    private ContactManager() {
    }

    public final static ContactManager getInstance() {
        return ContactManager.instance;
    }

    /**
     * 启动管理器。
     */
    public void startup(Kernel kernel) {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);

        this.daemon = new ManagementDaemon(this);

        this.onlineTables = new ConcurrentHashMap<>();
        this.tokenContactMap = new ConcurrentHashMap<>();

        this.activeGroupTables = new ConcurrentHashMap<>();

        // 联系人缓存
        SharedMemoryConfig contactConfig = new SharedMemoryConfig("config/contact-cache.properties");
        this.contactCache = new SharedMemory(contactConfig);

        // 群组缓存
        SharedMemoryConfig groupConfig = new SharedMemoryConfig("config/group-cache.properties");
        this.groupCache = new SharedMemory(groupConfig);

        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);

        this.pluginSystem = new ContactPluginSystem();

        // 启动守护线程
        this.daemon.start();

        // 异步初始化缓存和存储
        (new Thread() {
            @Override
            public void run() {
                // 启动联系人缓存
                contactCache.start();

                // 启动群组缓存
                groupCache.start();

                JSONObject config = new JSONObject();
                try {
                    config.put("file", "storage/ContactService.db");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                storage = new ContactStorage(executor, StorageType.SQLite, config);
                storage.open();

                AuthService authService = (AuthService) kernel.getModule(AuthService.NAME);
                storage.execSelfChecking(authService.getDomainList());
            }
        }).start();
    }

    /**
     * 关闭管理器。
     */
    public void shutdown() {
        this.daemon.terminate();

        this.storage.close();

        this.onlineTables.clear();
        this.tokenContactMap.clear();

        this.contactCache.stop();
        this.groupCache.stop();

        this.executor.shutdown();
    }

    public void setCellet(ContactServiceCellet cellet) {
        this.cellet = cellet;
    }

    /**
     * 终端签入。
     *
     * @param contact
     * @param authToken
     * @param activeDevice
     * @return
     */
    public Contact signIn(final Contact contact, final AuthToken authToken, final Device activeDevice) {
        // 判断 Domain 名称
        if (!authToken.getDomain().equals(contact.getDomain().getName())) {
            return null;
        }

        Logger.d(this.getClass(), "SignIn contact: " + contact.getId() + " (" + activeDevice.getName() + ") - " +
                authToken.getCode());

        // 在该方法里插入 Hook
        ContactHook hook = this.pluginSystem.getSignInHook();
        hook.apply(contact);

        this.contactCache.apply(contact.getUniqueKey(), new LockFuture() {
            @Override
            public void acquired(String key) {
            JSONObject data = get();
            if (null != data) {
                Contact cached = new Contact(data);

                // 追加设备
                for (Device device : cached.getDeviceList()) {
                    if (!contact.hasDevice(device)) {
                        contact.addDevice(device);
                    }
                }
            }

            put(contact.toJSON());
            }
        });

        // 更新存储
        this.storage.writeContact(contact, activeDevice);

        // 关注该用户数据
        this.follow(contact, authToken, activeDevice);

        return contact;
    }

    /**
     * 联系人签出。
     *
     * @param contact
     * @param tokenCode
     * @param activeDevice
     * @return
     */
    public Contact signOut(final Contact contact, String tokenCode, Device activeDevice) {
        final Object mutex = new Object();
        LockFuture future = this.contactCache.apply(contact.getUniqueKey(), new LockFuture() {
            @Override
            public void acquired(String key) {
            JSONObject data = get();
            if (null != data) {
                Contact current = new Contact(data);

                // 删除设备
                current.removeDevice(activeDevice);

                if (current.numDevices() == 0) {
                    remove();
                }
                else {
                    for (Device dev : current.getDeviceList()) {
                        if (!contact.hasDevice(dev)) {
                            contact.addDevice(dev);
                        }
                    }

                    put(current.toJSON());
                }
            }

            synchronized (mutex) {
                mutex.notify();
            }
            }
        });

        if (null != future) {
            synchronized (mutex) {
                try {
                    mutex.wait(this.syncTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        this.repeal(contact, tokenCode, activeDevice);
        return contact;
    }

    /**
     * 客户端在断线后恢复。
     *
     * @param contact
     * @param tokenCode
     * @return
     */
    public Contact comeback(final Contact contact, final String tokenCode) {
        TokenDevice tokenDevice = this.tokenContactMap.get(tokenCode);
        if (null == tokenDevice) {
            return null;
        }

        Contact onlineContact = tokenDevice.contact;

        // 如果信息匹配，则返回服务器存储的实体
        if (onlineContact.getDomain().equals(contact.getDomain()) &&
            onlineContact.getId().longValue() == contact.getId().longValue()) {
            // 重置时间戳
            onlineContact.resetTimestamp();
            return onlineContact;
        }

        return null;
    }

    /**
     * 获取令牌对应的设备。
     *
     * @param tokenCode
     * @return
     */
    public Device getDevice(String tokenCode) {
        TokenDevice device = this.tokenContactMap.get(tokenCode);
        if (null == device) {
            return null;
        }

        return device.device;
    }

    /**
     * 获取令牌对应的联系人。
     *
     * @param tokenCode
     * @return
     */
    public Contact getContact(String tokenCode) {
        TokenDevice device = this.tokenContactMap.get(tokenCode);
        if (null == device) {
            return null;
        }

        return device.contact;
    }

    /**
     * 获取联系人。
     *
     * @param domain
     * @param id
     * @return
     */
    public Contact getContact(String domain, Long id) {
        String key = UniqueKey.make(id, domain);
        JSONObject data = this.contactCache.applyGet(key);
        if (null != data) {
            return new Contact(data);
        }

        // 缓存里没有数据，从数据库读取
        Contact contact = this.storage.readContact(domain, id);
        if (null != contact) {
            return contact;
        }

        contact = new Contact(id, domain, "Cube-" + id);
        return contact;
    }

    /**
     * 获取指定的在线联系人。
     *
     * @param domainName
     * @param id
     * @return
     */
    public Contact getOnlineContact(String domainName, Long id) {
        ContactTable table = this.onlineTables.get(domainName);
        if (null == table) {
            return null;
        }

        return table.get(id);
    }

    /**
     * 移除指定联系人的设备。
     *
     * @param contactId
     * @param domainName
     * @param device
     */
    public void removeContactDevice(final Long contactId, final String domainName, final Device device) {
        String key = UniqueKey.make(contactId, domainName);
        this.contactCache.apply(key, new LockFuture() {
            @Override
            public void acquired(String key) {
                JSONObject data = get();
                if (null == data) {
                    return;
                }

                Contact contact = new Contact(data);
                contact.removeDevice(device);

                put(contact.toJSON());
            }
        });

        ContactTable table = this.onlineTables.get(domainName);
        if (null != table) {
            Contact contact = table.get(contactId);
            if (null != contact) {
                contact.removeDevice(device);
            }
        }
    }

    /**
     * 获取列表里联系人。
     *
     * @param domain
     * @param idList
     * @return
     */
    public List<Contact> getContactList(String domain, List<Long> idList) {
        List<Contact> result = new ArrayList<>(idList.size());
        return result;
    }

    /**
     * 获取指定联系人所在的所有群。
     *
     * @param domain
     * @param memberId
     * @param beginningLastActive
     * @param endingLastActive
     * @return
     */
    public List<Group> listGroupsWithMember(String domain, Long memberId, long beginningLastActive, long endingLastActive) {
        List<Group> result = this.storage.readGroupsWithMember(domain, memberId, beginningLastActive, endingLastActive);
        Collections.sort(result);
        return result;
    }

    /**
     * 获取群组。
     *
     * @param id
     * @param domainName
     * @return
     */
    public Group getGroup(Long id, String domainName) {
        GroupTable table = this.activeGroupTables.get(domainName);
        if (null != table) {
            Group group = table.getGroup(id);
            if (null != group) {
                return group;
            }
        }

        String key = UniqueKey.make(id, domainName);
        JSONObject data = this.groupCache.applyGet(key);
        if (null != data) {
            Group group = new Group(data);
            if (null != table) {
                table.putGroup(group);
            }
            return group;
        }

        Group group = this.storage.readGroup(domainName, id);
        if (null != group) {
            if (null != table) {
                table.putGroup(group);
            }
            return group;
        }

        return null;
    }

    /**
     * 修改群组信息。
     *
     * @param modifiedGroup
     * @return
     */
    public Group modifyGroup(Group modifiedGroup) {
        GroupTable gt = this.getGroupTable(modifiedGroup.getDomain().getName());
        Group current = gt.updateGroup(modifiedGroup);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Modify group " + modifiedGroup.getId());
        }

        return current;
    }

    /**
     * 修改群成员信息。
     *
     * @param group
     * @param member
     * @return
     */
    public GroupBundle modifyGroupMember(Group group, Contact member) {
        GroupTable gt = this.getGroupTable(group.getDomain().getName());
        Contact current = gt.updateGroupMember(group, member);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Modify group member " + group.getId() + " - " + member.getId());
        }

        GroupBundle bundle = new GroupBundle(group, current);
        return bundle;
    }

    /**
     * 更新群组的活跃时间。
     *
     * @param group
     * @param timestamp
     */
    public void updateGroupActiveTime(Group group, long timestamp) {
        GroupTable table = this.getGroupTable(group.getDomain().getName());

        // 更新活跃时间
        table.updateActiveTime(group, timestamp);
    }

    /**
     * 创建群组。
     *
     * @param group
     * @return
     */
    public Group createGroup(Group group) {
        long now = System.currentTimeMillis();

        // 重新生成 ID
        group.resetId(Utils.generateSerialNumber());
        // 重置时间戳
        group.setCreationTime(now);
        group.setLastActiveTime(now);

        // 写入存储
        this.storage.writeGroup(group);

        // 写入缓存
        this.groupCache.applyPut(group.getUniqueKey(), group.toJSON());

        // 写入活跃表
        GroupTable table = this.activeGroupTables.get(group.getDomain().getName());
        if (null == table) {
            table = new GroupTable(group.getDomain(), this.groupCache, this.storage);
            this.activeGroupTables.put(table.getDomain().getName(), table);
        }
        table.putGroup(group);

        // 向群成员发送事件
        for (Contact member : group.getMembers()) {
            if (member.equals(group.getOwner())) {
                // 创建群的所有者不进行通知
                continue;
            }

            ModuleEvent event = new ModuleEvent(NAME, ContactActions.CreateGroup.name, group.toJSON());
            this.contactsAdapter.publish(member.getUniqueKey(), event.toJSON());
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Group created : " + group.getUniqueKey() + " - \"" + group.getName() + "\"");
        }

        return group;
    }

    /**
     * 解散群。
     *
     * @param group
     * @return
     */
    public Group dissolveGroup(final Group group) {
        // 获取活跃表
        GroupTable table = this.getGroupTable(group.getDomain().getName());

        // 更新状态
        Group current = table.updateState(group, GroupState.Dismissed);
        if (null == current) {
            // 系统内没有该群组
            return null;
        }

        // 向群里的成员发送事件
        for (Contact member : current.getMembers()) {
            if (member.equals(current.getOwner())) {
                // 创建群的所有者不进行通知
                continue;
            }

            ModuleEvent event = new ModuleEvent(NAME, ContactActions.DissolveGroup.name, current.toJSON());
            this.contactsAdapter.publish(member.getUniqueKey(), event.toJSON());
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Group dissolved : " + current.getUniqueKey() + " - \"" + current.getName() + "\"");
        }

        return current;
    }

    /**
     * 添加群组成员。
     *
     * @param domain
     * @param groupId
     * @param memberIdList
     * @param operator
     * @return
     */
    public GroupBundle addGroupMembersById(String domain, Long groupId, List<Long> memberIdList, Contact operator) {
        ArrayList<Contact> memberList = new ArrayList<>(memberIdList.size());
        for (Long id : memberIdList) {
            Contact contact = this.getOnlineContact(domain, id);
            if (null == contact) {
                contact = this.getContact(domain, id);
            }

            memberList.add(contact);
        }

        return this.addGroupMembers(domain, groupId, memberList, operator);
    }

    /**
     * 添加群组成员。
     *
     * @param domain
     * @param groupId
     * @param memberList
     * @param operator
     * @return
     */
    public GroupBundle addGroupMembers(String domain, Long groupId, List<Contact> memberList, Contact operator) {
        Group group = this.getGroup(groupId, domain);
        if (null == group) {
            Logger.w(this.getClass(), "Add group member : can not find group " + groupId);
            return null;
        }

        ArrayList<Contact> addedList = new ArrayList<>();
        for (Contact contact : memberList) {
            Contact addedContact = group.addMember(contact);
            if (null == addedContact) {
                Logger.w(this.getClass(), "Add group member : try to add duplicative member - " + groupId);
                continue;
            }

            addedList.add(addedContact);
        }

        GroupTable gt = this.getGroupTable(domain);
        Group current = gt.addGroupMembers(group, addedList, operator);
        if (null == current) {
            Logger.w(this.getClass(), "Add group member : update cache & storage failed - " + groupId);
            return null;
        }

        GroupBundle bundle = new GroupBundle(current, addedList);
        bundle.operator = operator;

        // 向群里的成员发送事件
        for (Contact member : current.getMembers()) {
            ModuleEvent event = new ModuleEvent(NAME, ContactActions.AddGroupMember.name, bundle.toJSON());
            this.contactsAdapter.publish(member.getUniqueKey(), event.toJSON());
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Add group member : " + current.getUniqueKey() + " - \"" + current.getName() + "\"");
        }

        return bundle;
    }

    /**
     * 移除群组成员。
     *
     * @param domain
     * @param groupId
     * @param memberIdList
     * @param operator
     * @return
     */
    public GroupBundle removeGroupMembers(String domain, Long groupId, List<Long> memberIdList, Contact operator) {
        Group group = this.getGroup(groupId, domain);
        if (null == group) {
            Logger.w(this.getClass(), "Remove group member : can not find group " + groupId);
            return null;
        }

        // 最近的成员列表
        List<Contact> recentMembers = group.getMembers();

        ArrayList<Contact> removedList = new ArrayList<>();
        for (Long memberId : memberIdList) {
            if (group.getOwner().getId().longValue() == memberId.longValue()) {
                Logger.w(this.getClass(), "Remove group member : try to remove owner - " + groupId);
                continue;
            }

            Contact removedContact = group.removeMember(memberId);
            if (null == removedContact) {
                Logger.w(this.getClass(), "Remove group member : try to remove non-member - " + groupId);
                continue;
            }

            removedList.add(removedContact);
        }

        GroupTable gt = this.getGroupTable(domain);
        Group current = gt.removeGroupMembers(group, removedList, operator);
        if (null == current) {
            Logger.w(this.getClass(), "Remove group member : update cache & storage failed - " + groupId);
            return null;
        }

        GroupBundle bundle = new GroupBundle(current, removedList);
        bundle.operator = operator;

        // 向群里的成员发送事件，使用最近列表
        for (Contact member : recentMembers) {
            ModuleEvent event = new ModuleEvent(NAME, ContactActions.RemoveGroupMember.name, bundle.toJSON());
            this.contactsAdapter.publish(member.getUniqueKey(), event.toJSON());
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Remove group member : " + current.getUniqueKey() + " - \"" + current.getName() + "\"");
        }

        return bundle;
    }

    /**
     * 跟踪该联系人数据。
     *
     * @param contact
     * @param token
     * @param activeDevice
     */
    private synchronized void follow(Contact contact, AuthToken token, Device activeDevice) {
        // 订阅该用户事件
        this.contactsAdapter.subscribe(contact.getUniqueKey());

        ContactTable table = this.onlineTables.get(contact.getDomain().getName());
        if (null == table) {
            table = new ContactTable(new Domain(contact.getDomain().getName().toString()));
            this.onlineTables.put(table.getDomain().getName(), table);
        }
        // 记录联系人的通信上下文
        Contact activeContact = table.add(contact, activeDevice);

        // 记录令牌对应关系
        this.tokenContactMap.put(token.getCode().toString(), new TokenDevice(activeContact, activeDevice));

        // 建立群组缓存
        GroupTable groupTable = this.activeGroupTables.get(contact.getDomain().getName());
        if (null == groupTable) {
            Domain domain = new Domain(contact.getDomain().getName().toString());
            this.activeGroupTables.put(domain.getName(), new GroupTable(domain, this.groupCache, this.storage));
        }
    }

    /**
     * 放弃管理该联系人数据。
     *
     * @param contact
     * @param token
     * @param activeDevice
     */
    private synchronized void repeal(Contact contact, String token, Device activeDevice) {
        ContactTable table = this.onlineTables.get(contact.getDomain().getName());

        if (contact.numDevices() == 1) {
            // 退订该用户事件
            this.contactsAdapter.unsubscribe(contact.getUniqueKey());

            if (null != table) {
                table.remove(contact);
            }
        }
        else {
            if (null != table) {
                table.remove(contact, activeDevice);
            }
        }

        this.tokenContactMap.remove(token);
    }

    private synchronized GroupTable getGroupTable(final String domain) {
        GroupTable table = this.activeGroupTables.get(domain);
        if (null == table) {
            table = new GroupTable(new Domain(domain), this.groupCache, this.storage);
            this.activeGroupTables.put(table.getDomain().getName(), table);
        }
        return table;
    }

    /**
     * 向在线设备推送动作数据。
     *
     * @param actionName
     * @param contact
     * @param data
     */
    private void pushAction(String actionName, Contact contact, JSONObject data) {
        for (Device device : contact.getDeviceList()) {
            TalkContext talkContext = device.getTalkContext();
            if (null == talkContext) {
                continue;
            }

            JSONObject payload = new JSONObject();
            try {
                payload.put("code", ContactStateCode.Ok.code);
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
        if (NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {
            // 提取主键的信息
            Object[] key = UniqueKey.extract(topic);
            if (null == key) {
                return;
            }

            // 取主键的 ID
            Long id = (Long) key[0];
            // 取主键的域
            String domain = (String) key[1];

            ModuleEvent event = new ModuleEvent(jsonObject);
            String eventName = event.getEventName();
            if (eventName.equals(ContactActions.CreateGroup.name)
                || eventName.equals(ContactActions.DissolveGroup.name)
                || eventName.equals(ContactActions.AddGroupMember.name)
                || eventName.equals(ContactActions.RemoveGroupMember.name)) {
                // 获取在线的联系人信息
                Contact contact = this.getOnlineContact(domain, id);
                if (null == contact) {
                    return;
                }

                // 推送动作给终端设备
                this.pushAction(eventName, contact, event.getData());
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
