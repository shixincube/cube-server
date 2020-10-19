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
import cell.util.json.JSONObject;
import cube.auth.AuthToken;
import cube.common.Domain;
import cube.common.UniqueKey;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Group;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联系人管理器。
 */
public class ContactManager implements CelletAdapterListener {

    private final static ContactManager instance = new ContactManager();

    /**
     * 管理线程。
     */
    private ManagementDaemon daemon;

    /**
     * 在线的联系人表。
     */
    protected ConcurrentHashMap<Domain, ContactTable> onlineTables;

    /**
     * 令牌码对应联系人关系。
     */
    protected ConcurrentHashMap<String, Contact> tokenContactMap;

    /**
     * 联系人数据缓存。
     */
    private SharedMemory contactsCache;

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
    public void startup() {
        this.daemon = new ManagementDaemon(this);

        this.onlineTables = new ConcurrentHashMap<>();
        this.tokenContactMap = new ConcurrentHashMap<>();

        this.storage = new ContactStorage();

        SharedMemoryConfig config = new SharedMemoryConfig("config/contacts.properties");

        this.contactsCache = new SharedMemory(config);
        this.contactsCache.start();

        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);

        this.pluginSystem = new ContactPluginSystem();

        // 启动守护线程
        this.daemon.start();

        // 启动存储
        this.storage.open();
    }

    /**
     * 关闭管理器。
     */
    public void shutdown() {
        this.contactsCache.stop();

        this.daemon.terminate();

        this.storage.close();

        this.onlineTables.clear();
        this.tokenContactMap.clear();
    }

    /**
     * 终端签入。
     * @param contact
     * @param authToken
     * @return
     */
    public Contact signIn(final Contact contact, final AuthToken authToken) {
        // 判断 Domain 名称
        if (!authToken.getDomain().equals(contact.getDomain().getName())) {
            return null;
        }

        // 在该方法里插入 Hook
        ContactHook hook = this.pluginSystem.getSignInHook();
        hook.apply(contact);

        final Object mutex = new Object();
        LockFuture future = this.contactsCache.apply(contact.getUniqueKey(), new LockFuture() {
            @Override
            public void acquired(String key) {
            JSONObject data = get();
            if (null != data) {
                Contact old = new Contact(data);

                // 追加设备
                for (Device device : old.getDeviceList()) {
                    contact.addDevice(device);
                }
            }

            put(contact.toJSON());

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

        // 关注该用户数据
        this.follow(contact, authToken);

        return contact;
    }

    /**
     * 联系人签出。
     *
     * @param contact
     * @param tokenCode
     * @return
     */
    public Contact signOut(final Contact contact, String tokenCode) {
        final Object mutex = new Object();
        LockFuture future = this.contactsCache.apply(contact.getUniqueKey(), new LockFuture() {
            @Override
            public void acquired(String key) {
            JSONObject data = get();
            if (null != data) {
                Contact current = new Contact(data);

                // 删除设备
                current.removeDevice(contact.getCurrentDevice());

                if (current.numDevices() == 0) {
                    remove();
                }
                else {
                    for (Device dev : current.getDeviceList()) {
                        contact.addDevice(dev);
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

        this.repeal(contact, tokenCode);
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
        Contact onlineContact = this.tokenContactMap.get(tokenCode);

        // 如果信息匹配，则返回服务器存储的实体
        if (onlineContact.getDomain().equals(contact.getDomain()) &&
            onlineContact.getId().longValue() == contact.getId().longValue()) {
            return onlineContact;
        }

        return null;
    }

    /**
     * 获取联系人。
     *
     * @param id
     * @param domain
     * @return
     */
    public Contact getContact(Long id, String domain) {
        String key = UniqueKey.make(id, domain);
        JSONObject data = this.contactsCache.applyGet(key);
        if (null == data) {
            // 缓存里没有数据，从数据库读取
            data = this.storage.queryContact(domain, id);
            if (null == data) {
                return null;
            }
        }

        Contact contact = new Contact(data);
        return contact;
    }

    /**
     * 获取群组。
     *
     * @param id
     * @param domain
     * @return
     */
    public Group getGroup(Long id, Domain domain) {
        return this.getGroup(id, domain.getName());
    }

    /**
     * 获取群组。
     *
     * @param id
     * @param domainName
     * @return
     */
    public Group getGroup(Long id, String domainName) {
        String key = UniqueKey.make(id, domainName);
        JSONObject data = this.contactsCache.applyGet(key);
        if (null == data) {
            data = this.storage.queryGroup(domainName, id);
            if (null == data) {
                return null;
            }
        }

        Group group = new Group(data);
        return group;
    }

    public JSONObject getContactData(Long id, String domain) {
        String key = UniqueKey.make(id, domain);
        JSONObject data = this.contactsCache.applyGet(key);
        if (null == data) {
            Contact contact = new Contact(id, domain, "Cube-" + id.toString());
            data = contact.toJSON();
        }
        return data;
    }

    public Contact getOnlineContact(Domain domain, Long id) {
        ContactTable table = this.onlineTables.get(domain);
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
        this.contactsCache.apply(key, new LockFuture() {
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

        ContactTable table = this.onlineTables.get(new Domain(domainName));
        if (null != table) {
            Contact contact = table.get(contactId);
            if (null != contact) {
                contact.removeDevice(device);
            }
        }
    }

    /**
     * 跟踪该联系人数据。
     *
     * @param contact
     * @param token
     */
    private synchronized void follow(Contact contact, AuthToken token) {
        // 订阅该用户事件
        this.contactsAdapter.subscribe(contact.getUniqueKey());

        ContactTable table = this.onlineTables.get(contact.getDomain());
        if (null == table) {
            table = new ContactTable(new Domain(contact.getDomain().getName().toString()));
            this.onlineTables.put(table.getDomain(), table);
        }
        // 记录联系人的通信上下文
        table.add(contact);

        // 记录令牌对应关系
        this.tokenContactMap.put(token.getCode().toString(), contact);
    }

    /**
     * 放弃管理该联系人数据。
     *
     * @param contact
     */
    private synchronized void repeal(Contact contact, String token) {
        ContactTable table = this.onlineTables.get(contact.getDomain());

        if (contact.numDevices() == 1) {
            // 退订该用户事件
            this.contactsAdapter.unsubscribe(contact.getUniqueKey());

            if (null != table) {
                table.remove(contact);
            }
        }
        else {
            if (null != table) {
                table.update(contact);
            }
        }

        this.tokenContactMap.remove(token);
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, Primitive primitive) {

    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {

    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, Primitive primitive) {

    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, JSONObject jsonObject) {

    }

    @Override
    public void onSubscribeFailed(String topic, Endpoint endpoint) {

    }

    @Override
    public void onUnsubscribeFailed(String topic, Endpoint endpoint) {

    }
}
