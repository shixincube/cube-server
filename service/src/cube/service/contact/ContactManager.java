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
     * 在线的联系人列表。
     */
    protected ConcurrentHashMap<Long, Contact> onlineContacts;

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

        this.onlineContacts = new ConcurrentHashMap<>();

        this.storage = new ContactStorage();

        SharedMemoryConfig config = new SharedMemoryConfig("contacts.properties");

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
    }

    /**
     * 进行 Self 申请。
     * @param contact
     * @return
     */
    public Contact setSelf(final Contact contact) {
        // 在该方法里插入 Hook
        ContactHook hook = this.pluginSystem.getSelfHook();
        hook.apply(contact);

        final Object mutex = new Object();
        LockFuture future = this.contactsCache.apply(contact.getId().toString(), new LockFuture() {
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

        Contact self = contact;

        // 关注该用户数据
        this.follow(self);

        return self;
    }

    /**
     * 获取联系人。
     * @param id
     * @return
     */
    public Contact getContact(Long id) {
        JSONObject data = this.contactsCache.applyGet(id.toString());
        if (null == data) {
            // 缓存里没有数据，从数据库读取
            data = this.storage.queryContact(id);
            if (null == data) {
                return null;
            }
        }

        Contact contact = new Contact(data);
        return contact;
    }

    /**
     *
     * @param id
     * @return
     */
    public Group getGroup(Long id) {
        JSONObject data = this.contactsCache.applyGet(id.toString());
        if (null == data) {
            return null;
        }

        Group group = new Group(data);
        return group;
    }

    public JSONObject getContactData(Long id) {
        JSONObject data = this.contactsCache.applyGet(id.toString());
        if (null == data) {
            Contact contact = new Contact(id, "Cube" + id.toString());
            data = contact.toJSON();
        }
        return data;
    }

    public Contact getOnlineContact(Long id) {
        return this.onlineContacts.get(id);
    }

    /**
     * 移除指定联系人的设备。
     *
     * @param contactId
     * @param device
     */
    public void removeContactDevice(final Long contactId, final Device device) {
        this.contactsCache.apply(contactId.toString(), new LockFuture() {
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

        Contact contact = this.onlineContacts.get(contactId);
        if (null != contact) {
            contact.removeDevice(device);
        }
    }

    private void follow(Contact contact) {
        // 订阅该用户事件
        this.contactsAdapter.subscribe(Long.toString(contact.getId()));

        // 记录联系人的通信上下文
        this.onlineContacts.put(contact.getId(), contact);
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
