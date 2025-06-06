/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.auth.AuthToken;
import cube.common.Domain;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联系人表。
 */
public class ContactTable {

    private Domain domain;

    private ConcurrentHashMap<Long, Contact> onlineContacts;

    private ConcurrentHashMap<Long, AuthToken> contactTokenMap;

    /**
     * 联系人的阻止列表。
     */
    private ConcurrentHashMap<Long, List<Long>> contactBlockLists;

    public ContactTable(Domain domain) {
        this.domain = domain;
        this.onlineContacts = new ConcurrentHashMap<>();
        this.contactTokenMap = new ConcurrentHashMap<>();
        this.contactBlockLists = new ConcurrentHashMap<>();
    }

    public Domain getDomain() {
        return this.domain;
    }

    /**
     * 返回指定 ID 的联系人。
     *
     * @param id
     * @return
     */
    public Contact get(Long id) {
        return this.onlineContacts.get(id);
    }

    /**
     * 更新联系人。
     *
     * @param contact
     * @param device
     * @return
     */
    public Contact add(Contact contact, Device device, AuthToken authToken) {
        Contact current = this.onlineContacts.get(contact.getId());
        if (null == current) {
            current = contact;
            this.onlineContacts.put(contact.getId(), contact);
        }
        else {
            current.setName(contact.getName());

            JSONObject context = contact.getContext();
            if (null != context) {
                current.setContext(context);
            }

            current.resetTimestamp();

            current.addDevice(device);
        }

        this.contactTokenMap.put(current.getId(), authToken);

        return current;
    }

    /**
     * 移除联系人的设备。
     *
     * @param contact
     * @param device
     */
    public void remove(Contact contact, Device device) {
        Contact current = this.onlineContacts.get(contact.getId());
        if (null != current) {
            current.removeDevice(device);
            current.resetTimestamp();
        }
    }

    /**
     * 获取联系人的令牌。
     *
     * @param contactId
     * @return
     */
    public AuthToken getAuthToken(Long contactId) {
        return this.contactTokenMap.get(contactId);
    }

    /**
     * 获取在线联系人列表。
     *
     * @return
     */
    public List<Contact> getOnlineContacts() {
        return new ArrayList<>(this.onlineContacts.values());
    }

    /**
     * 移除联系人。
     *
     * @param contact
     */
    public void remove(Contact contact) {
        this.onlineContacts.remove(contact.getId());
        this.contactBlockLists.remove(contact.getId());
        this.contactTokenMap.remove(contact.getId());
    }

    /**
     * 更新联系人。
     *
     * @param contact
     * @return
     */
    public boolean update(Contact contact) {
        Contact current = this.onlineContacts.get(contact.getId());
        if (null == current) {
            return false;
        }

        current.setName(contact.getName());
        current.setContext(contact.getContext());
        return true;
    }

    public List<Long> getBlockList(Contact contact) {
        return this.contactBlockLists.get(contact.getId());
    }

    public List<Long> getBlockList(Long contactId) {
        return this.contactBlockLists.get(contactId);
    }

    public void setBlockList(Contact contact, List<Long> blockList) {
        this.contactBlockLists.put(contact.getId(), blockList);
    }

    public void setBlockList(Long contactId, List<Long> blockList) {
        this.contactBlockLists.put(contactId, blockList);
    }

    public void addBlockList(Contact contact, Long blockId) {
        List<Long> list = this.contactBlockLists.get(contact.getId());
        if (null != list) {
            list.add(blockId);
        }
    }

    public void removeBlockList(Contact contact, Long blockId) {
        List<Long> list = this.contactBlockLists.get(contact.getId());
        if (null != list) {
            list.remove(blockId);
        }
    }
}
