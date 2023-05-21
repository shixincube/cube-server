/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cell.util.Clock;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Domain;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.UniqueKey;
import cube.common.action.ContactAction;
import cube.common.entity.*;
import cube.common.state.ContactStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.auth.AuthServicePluginSystem;
import cube.service.contact.plugin.CreateDomainAppPlugin;
import cube.service.contact.plugin.FilterContactNamePlugin;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 联系人管理器。
 */
public class ContactManager extends AbstractModule implements CelletAdapterListener {

    public final static String NAME = "Contact";

    public final static String DEFAULT_CONTACT_ZONE_NAME = "contacts";

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
     * 守护任务。
     */
    private DaemonTask daemon;

    /**
     * 允许管理的最大在线联系人数量。
     */
    private int maxContactNum = 10000;

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
     * 联系人附录。
     */
    protected ConcurrentHashMap<String, ContactAppendix> contactAppendixMap;

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
    private long syncTimeout = 5000;

    /**
     * 联系人事件队列。
     */
    private CelletAdapter contactsAdapter;

    /**
     * 联系人模块的插件系统。
     */
    protected ContactPluginSystem pluginSystem;

    /**
     * 搜索结果映射。
     */
    protected ConcurrentHashMap<String, ContactSearchResult> searchMap;

    /**
     * 统计系统。
     */
    private StatisticsSystem statisticsSystem;

    private List<ContactManagerListener> listeners;

    private ContactManager() {
        super();
        this.listeners = new Vector<>();
    }

    /**
     * 获取管理器实例。
     *
     * @return 返回联系人服务实例。
     */
    public final static ContactManager getInstance() {
        return ContactManager.instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);

        this.daemon = new DaemonTask(this);

        this.onlineTables = new ConcurrentHashMap<>();
        this.tokenContactMap = new ConcurrentHashMap<>();

        this.activeGroupTables = new ConcurrentHashMap<>();

        this.contactAppendixMap = new ConcurrentHashMap<>();

        // 联系人缓存
        SharedMemoryConfig contactConfig = new SharedMemoryConfig("config/contact-cache.properties");
        this.contactCache = new SharedMemory(contactConfig);

        // 群组缓存
        SharedMemoryConfig groupConfig = new SharedMemoryConfig("config/group-cache.properties");
        this.groupCache = new SharedMemory(groupConfig);

        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);

        this.pluginSystem = new ContactPluginSystem();
        // 内置插件
        this.buildInPlugins();

        this.searchMap = new ConcurrentHashMap<>();

        this.statisticsSystem = new StatisticsSystem(this.executor);

        // 异步初始化缓存和存储
        (new Thread() {
            @Override
            public void run() {
                // 启动联系人缓存
                contactCache.start();

                // 启动群组缓存
                groupCache.start();

                // 读取存储配置
                JSONObject config = ConfigUtils.readStorageConfig();
                if (config.has(ContactManager.NAME)) {
                    config = config.getJSONObject(ContactManager.NAME);
                    if (config.getString("type").equalsIgnoreCase("SQLite")) {
                        storage = new ContactStorage(executor, StorageType.SQLite, config);
                    }
                    else {
                        storage = new ContactStorage(executor, StorageType.MySQL, config);
                    }
                }
                else {
                    config.put("file", "storage/ContactService.db");
                    storage = new ContactStorage(executor, StorageType.SQLite, config);
                }
                // 启动存储
                storage.open();

                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                storage.execSelfChecking(authService.getDomainList());

                // 插件注册
                initPlugin(authService);

                // 启动统计系统
                statisticsSystem.start(storage.getStorageType(), storage.getConfig(), authService.getDomainList());

                for (ContactManagerListener listener : listeners) {
                    listener.onStarted(ContactManager.this);
                }

                // 更新状态
                started.set(true);
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!this.isStarted()) {
            return;
        }

        this.storage.close();

        this.onlineTables.clear();
        this.tokenContactMap.clear();

        this.contactCache.stop();
        this.groupCache.stop();

        this.searchMap.clear();

        this.statisticsSystem.stop();

        this.executor.shutdown();

        for (ContactManagerListener listener : this.listeners) {
            listener.onStopped(this);
        }

        this.started.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContactPluginSystem getPluginSystem() {
        return this.pluginSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {
        this.daemon.run();
    }

    private void initPlugin(AuthService authService) {
        (new Thread() {
            @Override
            public void run() {
                AuthServicePluginSystem pluginSystem = authService.getPluginSystem();
                while (null == pluginSystem) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    pluginSystem = authService.getPluginSystem();
                }

                pluginSystem.register(AuthServiceHook.CreateDomainApp,
                        new CreateDomainAppPlugin());
            }
        }).start();
    }

    /**
     * 添加管理器的监听器。
     *
     * @param listener 监听器。
     */
    public void addListener(ContactManagerListener listener) {
        if (this.listeners.contains(listener)) {
            return;
        }
        this.listeners.add(listener);
    }

    /**
     * 移除管理器的监听器。
     *
     * @param listener 监听器。
     */
    public void removeListener(ContactManagerListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * 内置插件。
     */
    private void buildInPlugins() {
        this.pluginSystem.register(ContactHook.SignIn, new FilterContactNamePlugin());
    }

    /**
     * 设置 Cellet 实例。
     * @param cellet 指定 Cellet 。
     */
    public void setCellet(ContactServiceCellet cellet) {
        this.cellet = cellet;
    }

    /**
     * 返回在线联系人数量。
     *
     * @return 返回在线联系人数量。
     */
    public int numOnlineContacts() {
        return this.onlineTables.size();
    }

    /**
     * 返回最大在线联系人数量。
     *
     * @return 返回最大在线联系人数量。
     */
    public int getMaxContactNum() {
        return this.maxContactNum;
    }

    /**
     * 创建联系人。
     *
     * @param contactId 指定联系人 ID 。
     * @param domain 指定域名称。
     * @param contactName 指定联系人名称。
     * @param context 指定联系人上下文数据，可以设置 {@code null} 值。
     * @return 返回联系人实例。
     */
    public Contact createContact(Long contactId, String domain, String contactName, JSONObject context) {
        Contact contact = new Contact(contactId, domain, contactName);
        contact.setContext(context);

        Device device = new Device("Server", "Cube Server");

        // 更新存储
        this.storage.writeContact(contact, device);

        return contact;
    }

    /**
     * 终端签入。
     *
     * @param contact 指定联系人。
     * @param authToken 指定联系人使用的授权令牌。
     * @param activeDevice 指定当前签入的设备。
     * @return 返回联系人实例。
     */
    public Contact signIn(final Contact contact, final AuthToken authToken, final Device activeDevice) {
        if (!this.isStarted()) {
            // 未就绪
            return null;
        }

        // 判断 Domain 名称
        if (!authToken.getDomain().equals(contact.getDomain().getName())) {
            return null;
        }

        Logger.d(this.getClass(), "SignIn contact: " + contact.getId() + " (" + activeDevice.getName() + ") - " +
                authToken.getCode());

        // Hook sign-in
        ContactHook hook = this.pluginSystem.getSignInHook();
        hook.apply(new ContactPluginContext(ContactHook.SignIn, contact, activeDevice));

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
     * 仅使用令牌码签入。
     *
     * @param tokenCode 指定令牌码。
     * @return 返回签入的联系人。
     */
    public Contact signIn(String tokenCode, Device activeDevice) {
        if (!this.isStarted()) {
            // 未就绪
            return null;
        }

        // 获取指定令牌码对应的令牌
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken token = authService.getToken(tokenCode);
        if (null == token) {
            Logger.w(this.getClass(), "#signIn - Can NOT find token code: " + tokenCode);
            return null;
        }

        String domain = token.getDomain();
        Long cid = token.getContactId();

        if (cid.longValue() == 0) {
            // 该令牌没有绑定的联系人 ID
            Logger.w(this.getClass(), "#signIn - Token can NOT bind content: " + tokenCode);
            return null;
        }

        Contact contact = this.getContact(domain, cid);
        contact.addDevice(activeDevice);

        // Hook sign-in
        try {
            ContactHook hook = this.pluginSystem.getSignInHook();
            hook.apply(new ContactPluginContext(ContactHook.SignIn, contact, activeDevice));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#signIn", e);
            return null;
        }

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
        this.follow(contact, token, activeDevice);

        return contact;
    }

    /**
     * 联系人签出。
     *
     * @param contact 指定联系人。
     * @param tokenCode 指定签入时使用的令牌码。
     * @param activeDevice 指定当前签出的活跃设备。
     * @return 返回联系人实例。
     */
    public Contact signOut(final Contact contact, String tokenCode, Device activeDevice) {
        // Hook 调用插件
        ContactHook hook = this.pluginSystem.getSignOutHook();
        hook.apply(new ContactPluginContext(ContactHook.SignOut, contact, activeDevice));

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

        // 放弃管理
        this.repeal(contact, tokenCode, activeDevice);

        return contact;
    }

    /**
     * 客户端在断线后恢复。
     *
     * @param contact 指定联系人。
     * @param tokenCode 指定令牌码。
     * @return 返回联系人实例。操作失败时返回 {@code null} 值。
     */
    public Contact comeback(final Contact contact, final String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

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

            // 调用插件 Hook
            ContactHook hook = this.pluginSystem.getComebackHook();
            hook.apply(new ContactPluginContext(ContactHook.Comeback, onlineContact, tokenDevice.device));

            return onlineContact;
        }

        return null;
    }

    /**
     * 报告指定的联系人的设备断开连接。
     *
     * @param contact 指定联系人。
     */
    public void reportDisconnect(Contact contact) {
        if (null == this.contactsAdapter) {
            return;
        }

        String key = contact.getUniqueKey();
        ModuleEvent event = new ModuleEvent(ContactManager.NAME, ContactAction.Disconnect.name, contact.toJSON());
        this.contactsAdapter.publish(key, event.toJSON());
    }

    /**
     * 获取令牌对应的设备。
     *
     * @param tokenCode 指定设备的令牌码。
     * @return 返回设备实例。
     */
    public Device getDevice(String tokenCode) {
        TokenDevice device = this.tokenContactMap.get(tokenCode);
        if (null == device) {
            return null;
        }

        return device.device;
    }

    /**
     * 创建新的联系人。
     *
     * @param contact
     * @return
     */
    public Contact newContact(Contact contact) {
        this.storage.writeContact(contact);

        String key = UniqueKey.make(contact.getId(), contact.getDomain().getName());
        this.contactCache.applyPut(key, contact.toJSON());

        return contact;
    }

    /**
     * 更新联系人。
     *
     * @param domain 指定域名称。
     * @param contactId 指定联系人 ID 。
     * @param newName 指定联系人的新名称。
     * @param newContext 指定联系人的新上下文数据。
     * @return 返回更新后的联系人实例。
     */
    public Contact updateContact(String domain, Long contactId, String newName, JSONObject newContext) {
        String key = UniqueKey.make(contactId, domain);

        // 更新缓存里的数据
        JSONObject data = this.contactCache.applyGet(key);
        if (null != data) {
            Contact contact = new Contact(data);
            if (null != newName) {
                contact.setName(newName);
            }
            if (null != newContext) {
                contact.setContext(newContext);
            }

            this.contactCache.applyPut(key, contact.toJSON());
        }

        // 更新数据库
        Contact contact = this.storage.readContact(domain, contactId);
        if (null != contact) {
            if (null != newName) {
                contact.setName(newName);
            }
            if (null != newContext) {
                contact.setContext(newContext);
            }

            // 更新数据库
            this.storage.writeContact(contact);

            // 尝试更新在线数据
            ContactTable table = this.onlineTables.get(domain);
            if (null != table) {
                table.update(contact);
            }

            return contact;
        }

        return null;
    }

    /**
     * 复制联系人到指定域。
     *
     * @param source 源联系人。
     * @param destDomain 目标域。
     * @return 返回新的联系人实例。
     */
    public Contact copyContact(Contact source, String destDomain) {
        Contact srcContact = this.getContact(source.getDomain().getName(), source.getId());
        if (null == srcContact) {
            return null;
        }

        ContactAppendix appendix = this.getAppendix(srcContact);

        // 修改域
        srcContact.setDomain(destDomain);
        ContactAppendix srcAppendix = new ContactAppendix(srcContact, appendix.toJSON());

        // 创建新联系人
        Contact newContact = this.createContact(srcContact.getId(), destDomain, srcContact.getName(),
                srcContact.getContext());
        this.updateAppendix(srcAppendix);

        return newContact;
    }

    /**
     * 获取令牌对应的联系人。
     *
     * @param tokenCode 指定令牌码。
     * @return 返回令牌码对应的联系人。
     */
    public Contact getContact(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        if (!this.started.get()) {
            int count = 0;
            while (count < 30 && !this.started.get()) {
                ++count;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!this.started.get()) {
                return null;
            }
        }

        TokenDevice device = this.tokenContactMap.get(tokenCode);
        if (null == device) {
            // 从授权模块查找令牌
            AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
            AuthToken authToken = authService.getToken(tokenCode);
            if (null != authToken) {
                long contactId = authToken.getContactId();
                if (contactId > 0) {
                    return this.getContact(authToken.getDomain(), contactId);
                }
            }

            return null;
        }

        return device.contact;
    }

    /**
     * 获取联系人。
     *
     * @param domain 指定域名称。
     * @param id 指定联系人 ID 。
     * @return 返回联系人实例。
     */
    public Contact getContact(String domain, Long id) {
        if (!this.started.get()) {
            int count = 0;
            while (count < 30 && !this.started.get()) {
                ++count;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!this.started.get()) {
                return null;
            }
        }

        String key = UniqueKey.make(id, domain);
        JSONObject data = this.contactCache.applyGet(key);
        if (null != data) {
            return new Contact(data);
        }

        Contact contact = null;

        // 缓存里没有数据，从数据库读取
        if (null != this.storage) {
            contact = this.storage.readContact(domain, id);
            if (null != contact) {
                return contact;
            }
        }

        contact = new Contact(id, domain, "Cube-" + id);
        return contact;
    }

    /**
     * 获取指定的在线联系人。
     *
     * @param domainName 指定域名称。
     * @param id 指定联系人 ID 。
     * @return 返回指定的在线联系人。
     */
    public Contact getOnlineContact(String domainName, Long id) {
        ContactTable table = this.onlineTables.get(domainName);
        if (null == table) {
            return null;
        }

        return table.get(id);
    }

    /**
     * 获取指定域里的所有在线联系人。
     *
     * @param domainName 指定域名称。
     * @return 返回在线联系人列表。
     */
    public List<Contact> getOnlineContactsInDomain(String domainName) {
        ArrayList<Contact> contacts = new ArrayList<>();

        ContactTable table = this.onlineTables.get(domainName);
        if (null == table) {
            return contacts;
        }

        for (Contact contact : table.getOnlineContacts()) {
            if (contact.getDomain().getName().equals(domainName)) {
                contacts.add(contact);
            }
        }

        return contacts;
    }

    /**
     * 获取所有的在线联系人。
     *
     * @return 返回所有的在线联系人。
     */
    public List<Contact> getAllOnlineContacts() {
        ArrayList<Contact> contacts = new ArrayList<>();

        for (ContactTable table : this.onlineTables.values()) {
            contacts.addAll(table.getOnlineContacts());
        }

        return contacts;
    }

    /**
     * 获取指定域当前的所有联系人总数。
     *
     * @param domain 指定域。
     * @return 返回指定域当前的所有联系人总数。
     */
    protected int countContacts(String domain) {
        return this.storage.countContacts(domain);
    }

    /**
     * 获取指定域的联系人当前签入的令牌。
     *
     * @param domain
     * @param contactId
     * @return
     */
    public AuthToken getAuthToken(String domain, Long contactId) {
        ContactTable table = this.onlineTables.get(domain);
        if (null == table) {
            return null;
        }

        return table.getAuthToken(contactId);
    }

    /**
     * 移除指定联系人的设备。
     *
     * @param contactId
     * @param domainName
     * @param device
     */
    public void removeContactDevice(final Long contactId, final String domainName, final Device device) {
        if (null == this.contactCache) {
            return;
        }

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

                // 调用插件 Hook
                ContactHook hook = this.pluginSystem.getDeviceTimeoutHook();
                hook.apply(new ContactPluginContext(ContactHook.DeviceTimeout, contact, device));
            }
        }
    }

    /**
     * 更新联系人数据。
     *
     * @param domain
     * @param contactId
     * @param name
     * @param context
     * @return
     */
    public Contact modifyContact(String domain, Long contactId, String name, JSONObject context) {
        Contact contact = this.getContact(domain, contactId);
        if (null == contact) {
            return null;
        }

        boolean modified = false;

        if (null != name) {
            ContactHook hook = this.pluginSystem.getModifyContactNameHook();
            ContactPluginContext pluginContext = new ContactPluginContext(ContactHook.ModifyContactName, contact);
            // 设置新名称
            pluginContext.setNewName(name);
            // 调用插件
            hook.apply(pluginContext);
            // 获取新名称
            name = pluginContext.getNewName();

            if (null != name) {
                modified = contact.setName(name);
            }
        }

        if (null != context) {
            ContactHook hook = this.pluginSystem.getModifyContactContextHook();
            ContactPluginContext pluginContext = new ContactPluginContext(ContactHook.ModifyContactContext, contact);
            // 设置新上下文
            pluginContext.setNewContext(context);
            // 调用插件
            hook.apply(pluginContext);
            // 获取新上下文
            context = pluginContext.getNewContext();

            if (null != context) {
                contact.setContext(context);
                modified = true;
            }
        }

        if (modified) {
            // 重置时间戳
            contact.resetTimestamp();

            // 更新存储
            this.storage.writeContact(contact);

            // 更新缓存
            this.contactCache.applyPut(contact.getUniqueKey(), contact.toJSON());
        }

        return contact;
    }

    /**
     * 读取指定时间戳之后的联系人分区数据清单。
     *
     * @param contact
     * @param timestamp
     * @return
     */
    public List<ContactZone> listContactZones(Contact contact, long timestamp, int limit) {
        return this.storage.readContactZoneList(contact, timestamp, limit);
    }

    /**
     * 获取指定联系人指定名称的分区数据。
     *
     * @param contact
     * @param zoneName
     * @return
     */
    public ContactZone getContactZone(Contact contact, String zoneName) {
        ContactZone zone = this.storage.readContactZone(contact.getDomain().getName(), contact.getId(), zoneName);
        if (null == zone && DEFAULT_CONTACT_ZONE_NAME.equals(zoneName)) {
            // 没有创建默认联系人分区，创建
            zone = this.createContactZone(contact, zoneName, DEFAULT_CONTACT_ZONE_NAME, true, null);
        }
        return zone;
    }

    /**
     * 创建新的联系人分区。
     *
     * @param owner
     * @param zoneName
     * @param displayName
     * @param peerMode
     * @param participants
     * @return
     */
    public ContactZone createContactZone(Contact owner, String zoneName, String displayName, boolean peerMode,
                                         List<ContactZoneParticipant> participants) {
        String displayZoneName = (null != displayName) ? displayName : zoneName;

        // 创建实例
        ContactZone zone = new ContactZone(Utils.generateSerialNumber(), owner.getDomain().getName(),
                owner.getId(), zoneName, System.currentTimeMillis(),
                displayZoneName, ContactZoneState.Normal, peerMode);

        if (null != participants) {
            for (ContactZoneParticipant participant : participants) {
                zone.addParticipant(participant);
            }
        }

        // 写入数据库
        this.storage.writeContactZone(zone, null);

        return zone;
    }

    /**
     * 删除指定的联系人分区。
     *
     * @param owner
     * @param zoneName
     */
    public void deleteContactZone(Contact owner, String zoneName) {
        this.storage.deleteContactZone(owner.getDomain().getName(), owner.getId(), zoneName);
    }

    /**
     * 指定分区是否包含指定参与人。
     *
     * @param contact
     * @param zoneName
     * @param participantId
     * @return
     */
    public boolean containsParticipantInZone(Contact contact, String zoneName, Long participantId) {
        return this.storage.hasParticipantInZone(contact.getDomain().getName(), contact.getId(), zoneName, participantId);
    }

    /**
     * 强制添加分区参与人。
     *
     * @param contact
     * @param zone
     * @param participant
     */
    public void addParticipantToZoneByForce(Contact contact, ContactZone zone, ContactZoneParticipant participant) {
        if (zone.peerMode) {
            // 对等模式
            ContactZone peerZone = this.storage.readContactZone(contact.getDomain().getName(),
                    participant.id, zone.name);
            if (null != peerZone) {
                ContactZoneParticipant inviter = new ContactZoneParticipant(contact.getId(),
                        ContactZoneParticipantType.Contact, participant.timestamp,
                        contact.getId(), participant.postscript, participant.state);
                // 更新数据库
                this.storage.addZoneParticipant(peerZone, inviter);
            }
        }

        this.storage.addZoneParticipant(zone, participant);
        zone.addParticipant(participant);
    }

    /**
     * 添加参与人到指定的分区。
     *
     * @param contact
     * @param zoneName
     * @param participant
     * @return
     */
    public ContactZone addParticipantToZone(Contact contact, String zoneName, ContactZoneParticipant participant) {
        ContactZone zone = this.storage.readContactZone(contact.getDomain().getName(), contact.getId(), zoneName);
        if (null == zone) {
            return null;
        }

        // 判断发起人的阻止列表
        List<Long> blockList = this.getBlockList(contact);
        if (blockList.contains(participant.id)) {
            // 被阻止，不允许添加
            return null;
        }

        if (participant.type == ContactZoneParticipantType.Contact) {
            // 判断被添加人的阻止列表
            Contact participantContact = this.getContact(contact.getDomain().getName(), participant.id);
            blockList = this.getBlockList(participantContact);
            if (blockList.contains(contact.getId())) {
                // 被受邀者阻止，不允许添加
                return null;
            }
        }

        // 更新时间戳
        zone.resetTimestamp();

        if (zone.peerMode && participant.type == ContactZoneParticipantType.Contact) {
            // 对等模式
            ContactZone peerZone = this.storage.readContactZone(contact.getDomain().getName(),
                    participant.id, zoneName);
            if (null != peerZone) {
                // 向对端的分区插入邀请人
                ContactZoneParticipant inviter = new ContactZoneParticipant(contact.getId(),
                        ContactZoneParticipantType.Contact, System.currentTimeMillis(),
                        contact.getId(), participant.postscript, participant.state);
                peerZone.addParticipant(inviter);
                // 重置时间戳
                peerZone.resetTimestamp();
                // 更新数据库
                this.storage.addZoneParticipant(peerZone, inviter);

                // 绑定数据
                ContactZoneBundle bundle = new ContactZoneBundle(peerZone, inviter, ContactZoneBundle.ACTION_ADD);

                // 通知对方
                String uKey = UniqueKey.make(participant.id, contact.getDomain().getName());
                ModuleEvent event = new ModuleEvent(ContactManager.NAME, ContactAction.ModifyZone.name, bundle.toJSON());
                this.contactsAdapter.publish(uKey, event.toJSON());
            }
        }

        this.storage.addZoneParticipant(zone, participant);

        zone.addParticipant(participant);
        return zone;
    }

    /**
     * 从指定的分区移除参与人。
     *
     * @param contact
     * @param zoneName
     * @param participant
     * @return
     */
    public ContactZone removeParticipantFromZone(Contact contact, String zoneName, ContactZoneParticipant participant) {
        ContactZone zone = this.storage.readContactZone(contact.getDomain().getName(), contact.getId(), zoneName);
        if (null == zone) {
            return null;
        }

        zone.removeParticipant(participant.id);
        // 更新时间戳
        zone.resetTimestamp();

        if (zone.peerMode && participant.type == ContactZoneParticipantType.Contact) {
            // 对等模式，对等删除
            ContactZone peerZone = this.storage.readContactZone(contact.getDomain().getName(),
                    participant.id, zoneName);
            if (null != peerZone) {
                ContactZoneParticipant inviter = peerZone.getParticipant(contact.getId());
                if (null != inviter) {
                    // 更新时间戳
                    peerZone.resetTimestamp();
                    // 移除
                    this.storage.removeZoneParticipant(peerZone, inviter);

                    // 绑定数据
                    ContactZoneBundle bundle = new ContactZoneBundle(peerZone, inviter, ContactZoneBundle.ACTION_REMOVE);

                    // 通知对方
                    String uKey = UniqueKey.make(participant.id, contact.getDomain().getName());
                    ModuleEvent event = new ModuleEvent(ContactManager.NAME, ContactAction.ModifyZone.name, bundle.toJSON());
                    this.contactsAdapter.publish(uKey, event.toJSON());
                }
                else {
                    Logger.w(ContactManager.class, "#removeParticipantFromZone - Can NOT find inviter : " + participant.id);
                }
            }
        }

        // 移除
        this.storage.removeZoneParticipant(zone, participant);

        return zone;
    }

    /**
     * 修改分区参与人数据。
     *
     * @param contact
     * @param zoneName
     * @param participant
     * @return
     */
    public ContactZoneParticipant modifyZoneParticipant(Contact contact, String zoneName,
                                                        ContactZoneParticipant participant) {
        ContactZone zone = this.storage.readContactZone(contact.getDomain().getName(), contact.getId(), zoneName);
        if (null == zone) {
            return null;
        }

        // 设置时间戳
        long timestamp = System.currentTimeMillis();
        participant.timestamp = timestamp;

        if (zone.peerMode && participant.type == ContactZoneParticipantType.Contact) {
            // 对等模式
            ContactZone peerZone = this.storage.readContactZone(contact.getDomain().getName(), participant.id, zoneName);
            if (null != peerZone) {
                // 修改对端的自己
                ContactZoneParticipant selfInPeerZone = new ContactZoneParticipant(contact.getId(),
                        ContactZoneParticipantType.Contact, timestamp,
                        participant.id, participant.postscript, participant.state);
                this.storage.updateZoneParticipant(peerZone, selfInPeerZone);

                // 绑定数据
                ContactZoneBundle bundle = new ContactZoneBundle(peerZone, selfInPeerZone,
                        ContactZoneBundle.ACTION_UPDATE);

                String uKey = UniqueKey.make(participant.id, contact.getDomain().getName());
                ModuleEvent event = new ModuleEvent(ContactManager.NAME,
                        ContactAction.ModifyZoneParticipant.name, bundle.toCompactJSON());
                this.contactsAdapter.publish(uKey, event.toJSON());
            }
        }

        // 更新自己的 Zone
        this.storage.updateZoneParticipant(zone, participant);

        return participant;
    }

    /**
     * 获取指定联系人所在的所有群。
     *
     * @param domain
     * @param memberId
     * @param beginningLastActive
     * @param endingLastActive
     * @param groupState
     * @return
     */
    public List<Group> listGroupsWithMember(String domain, Long memberId,
                                            long beginningLastActive, long endingLastActive,
                                            GroupState groupState) {
        List<Group> result = this.storage.readGroupsWithMember(domain, memberId,
                beginningLastActive, endingLastActive, groupState.code);
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
        GroupTable table = this.getGroupTable(domainName);
        Group group = table.getGroup(id);
        if (null != group) {
            return group;
        }

        String key = UniqueKey.make(id, domainName);
        JSONObject data = this.groupCache.applyGet(key);
        if (null != data) {
            group = new Group(data);
            table.putGroup(group);
            return group;
        }

        group = this.storage.readGroup(domainName, id);
        if (null != group) {
            table.putGroup(group);
            this.groupCache.applyPut(key, group.toJSON());
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
        Group current = gt.updateGroup(modifiedGroup, true);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Modify group " + modifiedGroup.getId());
        }

        return current;
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

        if (!group.getMembers().contains(group.getOwnerId())) {
            // 创建人不在列表里，加入列表
            group.addMemberId(group.getOwnerId());
        }

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
        for (Long memberId : group.getMembers()) {
            if (memberId.equals(group.getOwnerId())) {
                // 创建群的所有者不进行通知
                continue;
            }

            String uKey = UniqueKey.make(memberId, group.getDomain().getName());
            ModuleEvent event = new ModuleEvent(NAME, ContactAction.CreateGroup.name, group.toJSON());
            this.contactsAdapter.publish(uKey, event.toJSON());
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
    public Group dismissGroup(final Group group) {
        // 获取活跃表
        GroupTable table = this.getGroupTable(group.getDomain().getName());

        // 更新状态
        Group current = table.updateState(group, GroupState.Dismissed);
        if (null == current) {
            // 系统内没有该群组
            return null;
        }

        // 向群里的成员发送事件
        for (Long memberId : current.getMembers()) {
            if (memberId.equals(current.getOwnerId())) {
                // 创建群的所有者不进行通知
                continue;
            }

            String uKey = UniqueKey.make(memberId, group.getDomain().getName());
            ModuleEvent event = new ModuleEvent(NAME, ContactAction.DismissGroup.name, current.toJSON());
            this.contactsAdapter.publish(uKey, event.toJSON());
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Group dismissed : " + current.getUniqueKey()
                    + " - \"" + current.getName() + "\"");
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

        ArrayList<Long> addedList = new ArrayList<>();
        for (Contact contact : memberList) {
            Long addedContactId = group.addMember(contact.getId());
            if (null == addedContactId) {
                Logger.w(this.getClass(), "Add group member : try to add repeated member - " + groupId);
                continue;
            }

            addedList.add(addedContactId);
        }

        GroupTable gt = this.getGroupTable(domain);
        Group current = gt.addGroupMembers(group, addedList, operator);
        if (null == current) {
            Logger.w(this.getClass(), "Add group member : update cache & storage failed - " + groupId);
            return null;
        }

        GroupBundle bundle = new GroupBundle(current, addedList);
        bundle.operatorId = operator.getId();

        // 向群里的成员发送事件
        for (Long memberId : current.getMembers()) {
            String uKey = UniqueKey.make(memberId, group.getDomain().getName());

            ModuleEvent event = new ModuleEvent(NAME, ContactAction.AddGroupMember.name, bundle.toJSON());
            this.contactsAdapter.publish(uKey, event.toJSON());
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
        List<Long> recentMembers = group.getMembers();

        ArrayList<Long> removedList = new ArrayList<>();
        for (Long memberId : memberIdList) {
            if (group.getOwnerId().longValue() == memberId.longValue()) {
                Logger.w(this.getClass(), "Remove group member : try to remove owner - " + groupId);
                continue;
            }

            Long removedContactId = group.removeMember(memberId);
            if (null == removedContactId) {
                Logger.w(this.getClass(), "Remove group member : try to remove non-member - " + groupId);
                continue;
            }

            removedList.add(removedContactId);
        }

        GroupTable gt = this.getGroupTable(domain);
        // 删除群组成员
        Group current = gt.removeGroupMembers(group, removedList, operator);
        if (null == current) {
            Logger.w(this.getClass(), "Remove group member : update cache & storage failed - " + groupId);
            return null;
        }

        GroupBundle bundle = new GroupBundle(current, removedList);
        bundle.operatorId = operator.getId();

        // 向群里的成员发送事件，使用最近列表
        for (Long memberId : recentMembers) {
            String uKey = UniqueKey.make(memberId, group.getDomain().getName());
            ModuleEvent event = new ModuleEvent(NAME, ContactAction.RemoveGroupMember.name, bundle.toJSON());
            this.contactsAdapter.publish(uKey, event.toJSON());
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Remove group member : " + current.getUniqueKey() + " - \"" + current.getName() + "\"");
        }

        return bundle;
    }

    /**
     * 获取指定联系人的附录。
     *
     * @param contact
     * @return
     */
    public ContactAppendix getAppendix(Contact contact) {
        ContactAppendix appendix = this.contactAppendixMap.get(contact.getUniqueKey());
        if (null == appendix) {
            appendix = this.storage.readAppendix(contact);
            if (null == appendix) {
                appendix = new ContactAppendix(contact);
            }

            this.contactAppendixMap.put(contact.getUniqueKey(), appendix);
        }
        return appendix;
    }

    /**
     * 获取指定群组的附录。
     *
     * @param group
     * @return
     */
    public GroupAppendix getAppendix(Group group) {
        String key = GroupAppendix.makeUniqueKey(group);
        JSONObject json = this.groupCache.applyGet(key);
        if (null != json) {
            return new GroupAppendix(group, json);
        }

        GroupAppendix appendix = this.storage.readAppendix(group);
        if (null == appendix) {
            appendix = new GroupAppendix(group);
        }

        this.groupCache.applyPut(key, appendix.toJSON());
        return appendix;
    }

    /**
     * 更新附录。
     *
     * @param appendix
     */
    public void updateAppendix(ContactAppendix appendix) {
        final long timestamp = System.currentTimeMillis();

        // 更新联系人的时间戳
        Contact contact = appendix.getContact();

        // 更新缓存
        this.contactCache.apply(contact.getUniqueKey(), new LockFuture() {
            @Override
            public void acquired(String key) {
                JSONObject data = get();
                if (null == data) {
                    return;
                }

                Entity.updateTimestamp(data, timestamp);
                put(data);
            }
        });

        contact.setTimestamp(timestamp);
        this.storage.updateContactTimestamp(contact);

        this.contactAppendixMap.put(contact.getUniqueKey(), appendix);
        this.storage.writeAppendix(appendix);
    }

    /**
     * 更新附录。
     *
     * @param appendix
     */
    public void updateAppendix(Group group, GroupAppendix appendix, boolean broadcast) {
        this.groupCache.applyPut(appendix.getUniqueKey(), appendix.toJSON());
        this.storage.writeAppendix(appendix);

        // 更新群组的活跃时间
        this.storage.updateGroupActiveTime(group);

        if (broadcast) {
            // 向群组内所有成员广播
            for (Long memberId : group.getMembers()) {
                String uKey = UniqueKey.make(memberId, group.getDomain().getName());
                ModuleEvent event = new ModuleEvent(ContactManager.NAME,
                        ContactAction.GroupAppendixUpdated.name, appendix.packJSON(memberId));
                this.contactsAdapter.publish(uKey, event.toJSON());
            }
        }
    }

    /**
     * 添加阻止的联系人 ID 。
     *
     * @param contact
     * @param blockId
     */
    public List<Long> addBlockList(Contact contact, Long blockId) {
        ContactTable contactTable = this.onlineTables.get(contact.getDomain().getName());
        if (null != contactTable) {
            contactTable.addBlockList(contact, blockId);
        }

        this.storage.writeBlockList(contact.getDomain().getName(), contact.getId(), blockId);

        // 将阻止的联系人从分区里删除
        this.storage.clearParticipantInZones(contact.getDomain().getName(), contact.getId(), blockId);

        return this.storage.readBlockList(contact.getDomain().getName(), contact.getId());
    }

    /**
     * 移除被阻止的联系人 ID 。
     *
     * @param contact
     * @param blockId
     */
    public List<Long> removeBlockList(Contact contact, Long blockId) {
        ContactTable contactTable = this.onlineTables.get(contact.getDomain().getName());
        if (null != contactTable) {
            contactTable.removeBlockList(contact, blockId);
        }

        this.storage.deleteBlockList(contact.getDomain().getName(), contact.getId(), blockId);
        return this.storage.readBlockList(contact.getDomain().getName(), contact.getId());
    }

    /**
     * 获取指定联系人的阻止列表。
     *
     * @param contact
     * @return 返回指定联系人的阻止列表。
     */
    public List<Long> getBlockList(Contact contact) {
        ContactTable contactTable = this.onlineTables.get(contact.getDomain().getName());
        if (null != contactTable) {
            List<Long> list = contactTable.getBlockList(contact);
            if (null == list) {
                list = this.storage.readBlockList(contact.getDomain().getName(), contact.getId());
                contactTable.setBlockList(contact, list);
            }
            return list;
        }

        return this.storage.readBlockList(contact.getDomain().getName(), contact.getId());
    }

    /**
     * 判读指定联系人是是否阻止了目标联系人。
     *
     * @param domain
     * @param contactId
     * @param targetContactId
     * @return
     */
    public boolean hasBlocked(String domain, Long contactId, Long targetContactId) {
        ContactTable contactTable = this.onlineTables.get(domain);
        if (null == contactTable) {
            return this.storage.hasBlocked(domain, contactId, targetContactId);
        }

        List<Long> list = contactTable.getBlockList(contactId);
        if (null == list) {
            list = this.storage.readBlockList(domain, contactId);
            contactTable.setBlockList(contactId, list);
        }

        return list.contains(targetContactId);
    }

    /**
     * 添加联系人的置顶列表。
     *
     * @param contact
     * @param topId
     * @param type
     */
    public void addTopList(Contact contact, Long topId, String type) {
        this.storage.writeTopList(contact.getDomain().getName(), contact.getId(), topId, type);
    }

    /**
     * 移除联系人的置顶列表。
     *
     * @param contact
     * @param topId
     */
    public void removeTopList(Contact contact, Long topId) {
        this.storage.deleteTopList(contact.getDomain().getName(), contact.getId(), topId);
    }

    /**
     * 获取指定联系人的置顶列表。
     *
     * @param contact
     * @return
     */
    public List<JSONObject> getTopList(Contact contact) {
        return this.storage.readTopList(contact.getDomain().getName(), contact.getId());
    }

    /**
     * 对指定关键字进行模糊检索。
     *
     * @param domain
     * @param keyword
     * @return
     */
    public ContactSearchResult searchWithFuzzyRule(String domain, String keyword) {
        ContactSearchResult result = this.searchMap.get(keyword);

        if (null != result) {
            // 检查是否超过5分钟
            if (Clock.currentTimeMillis() - result.getTimestamp() < 300000L) {
                return result;
            }
        }

        result = new ContactSearchResult(keyword);

        List<Contact> contacts = this.storage.searchContacts(domain, keyword);
        if (null != contacts) {
            for (Contact contact : contacts) {
                result.addContact(contact);
            }
        }

        List<Group> groups = this.storage.searchGroups(domain, keyword);
        if (null != groups) {
            for (Group group : groups) {
                result.addGroup(group);
            }
        }

        this.searchMap.put(keyword, result);

        return result;
    }

    /**
     * 使用联系人 ID 检索。
     *
     * @param domain
     * @param contactId
     * @return
     */
    public ContactSearchResult searchWithContactId(String domain, String contactId) {
        ContactSearchResult result = new ContactSearchResult(contactId);

        try {
            Contact contact = this.storage.readContact(domain, Long.parseLong(contactId));
            if (null != contact) {
                result.addContact(contact);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
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
        Contact activeContact = table.add(contact, activeDevice, token);

        // 记录令牌对应关系
        this.tokenContactMap.put(token.getCode(), new TokenDevice(activeContact, activeDevice));

        // 绑定令牌
        token.setContactId(contact.getId());
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        if (!authService.bindToken(token)) {
            Logger.w(ContactManager.class, "#follow - bind token for " + contact.getId() + " failed");
        }

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

    /**
     * 刷新指定的域信息。
     *
     * @param authDomain
     */
    public void refreshAuthDomain(AuthDomain authDomain) {
        List<String> list = new ArrayList<>();
        list.add(authDomain.domainName);
        this.storage.execSelfChecking(list);
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
            if (eventName.equals(ContactAction.CreateGroup.name)
                || eventName.equals(ContactAction.DismissGroup.name)
                || eventName.equals(ContactAction.AddGroupMember.name)
                || eventName.equals(ContactAction.RemoveGroupMember.name)) {
                // 获取在线的联系人
                Contact contact = this.getOnlineContact(domain, id);
                if (null == contact) {
                    // 联系人在本地不在线
                    return;
                }

                // 推送动作给终端设备
                this.pushAction(eventName, contact, event.getData());
            }
            else if (ContactAction.GroupAppendixUpdated.name.equals(eventName)) {
                // 获取在线的联系人
                Contact contact = this.getOnlineContact(domain, id);
                if (null == contact) {
                    // 联系人在本地不在线
                    return;
                }

                // 推送数据
                this.pushAction(eventName, contact, event.getData());
            }
            else if (ContactAction.ModifyZoneParticipant.name.equals(eventName)
                || ContactAction.ModifyZone.name.equals(eventName)) {
                // 获取在线的联系人
                Contact contact = this.getOnlineContact(domain, id);
                if (null == contact) {
                    // 联系人在本地不在线
                    return;
                }

                // 推送数据
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
