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

import cell.adapter.extra.memory.SharedMemory;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.GroupState;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于暂存活跃群组的表格。
 */
public class GroupTable {

    private Domain domain;

    private SharedMemory cache;

    private ContactStorage storage;

    protected ConcurrentHashMap<Long, Group> groups;

    public GroupTable(Domain domain, SharedMemory cache, ContactStorage storage) {
        this.domain = domain;
        this.cache = cache;
        this.storage = storage;
        this.groups = new ConcurrentHashMap<>();
    }

    public Domain getDomain() {
        return this.domain;
    }

    public Group getGroup(Long id) {
        return this.groups.get(id);
    }

    public void putGroup(Group group) {
        this.groups.put(group.getId(), group);
    }

    /**
     * 更新群组。对群组进行比较，如果群组数据发生变化，则成为脏数据，由管理器进行更新
     * @param group
     * @return
     */
    public Group updateGroup(Group group) {
        Group current = refresh(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        boolean modified = false;

        if (!current.getName().equals(group.getName())) {
            modified = true;
            current.setName(group.getName());
        }

        if (!current.getOwner().equals(group.getOwner())) {
            modified = true;
            current.setOwner(group.getOwner());
        }

        if (current.getLastActiveTime() != group.getLastActiveTime()) {
            modified = true;
            current.setLastActiveTime(group.getLastActiveTime());
        }

        JSONObject context = group.getContext();
        if (null != context) {
            if (null == current.getContext()) {
                modified = true;
                current.setContext(context);
            }
            else {
                JSONObject currentCtx = current.getContext();
                if (!currentCtx.toString().equals(context.toString())) {
                    modified = true;
                    current.setContext(context);
                }
            }
        }

        if (modified) {
            this.cache.applyPut(current.getUniqueKey(), current.toJSON());
            this.storage.writeGroup(current);
        }

        this.groups.put(current.getId(), current);

        return current;
    }

    /**
     * 更新状态。
     *
     * @param group
     * @param state
     * @return
     */
    public Group updateState(Group group, GroupState state) {
        Group current = refresh(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        boolean modified = false;
        if (current.getState() != state) {
            modified = true;
        }

        if (modified) {
            current.setLastActiveTime(System.currentTimeMillis());
            current.setState(state);

            if (group != current) {
                group.setLastActiveTime(current.getLastActiveTime());
                group.setState(current.getState());
            }

            this.cache.applyPut(current.getUniqueKey(), current.toJSON());
            this.storage.updateGroupState(current, false);
        }

        this.groups.put(current.getId(), current);

        return current;
    }

    /**
     *
     * @param group
     * @param timestamp
     * @return
     */
    public Group updateActiveTime(Group group, long timestamp) {
        Group current = refresh(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        boolean modified = false;
        if (current.getLastActiveTime() != timestamp) {
            modified = true;
        }

        if (modified) {
            current.setLastActiveTime(timestamp);

            if (group != current) {
                group.setLastActiveTime(timestamp);
            }

            this.cache.applyPut(current.getUniqueKey(), current.toJSON());
            this.storage.updateGroupActiveName(current);
        }

        this.groups.put(current.getId(), current);

        return current;
    }

    /**
     * 添加群组成员。
     *
     * @param group
     * @param addedContactList
     * @param operator
     * @return
     */
    public Group addGroupMembers(Group group, List<Contact> addedContactList, Contact operator) {
        Group current = refresh(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        // 添加成员
        for (Contact member : addedContactList) {
            current.addMember(member);
        }

        // 更新时间戳
        current.setLastActiveTime(System.currentTimeMillis());

        // 更新缓存
        this.cache.applyPut(current.getUniqueKey(), current.toJSON());

        this.storage.addGroupMembers(current, addedContactList, operator.getId(), new Runnable() {
            @Override
            public void run() {
                storage.updateGroupActiveName(current);
            }
        });

        this.groups.put(current.getId(), current);
        return current;
    }

    /**
     * 移除群组成员。
     *
     * @param group
     * @param removedContactList
     * @param operator
     * @return
     */
    public Group removeGroupMembers(Group group, List<Contact> removedContactList, Contact operator) {
        Group current = refresh(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        // 删除成员
        for (Contact member : removedContactList) {
            current.removeMember(member);
        }

        // 更新时间戳
        current.setLastActiveTime(System.currentTimeMillis());

        // 更新缓存
        this.cache.applyPut(current.getUniqueKey(), current.toJSON());

        this.storage.removeGroupMembers(current, removedContactList, operator.getId(), new Runnable() {
            @Override
            public void run() {
                storage.updateGroupActiveName(current);
            }
        });

        this.groups.put(current.getId(), current);
        return current;
    }

    /**
     *
     * @param group
     * @return
     */
    private Group refresh(Group group) {
        Group result = null;

        // 从集群里获取
        JSONObject data = this.cache.applyGet(group.getUniqueKey());
        if (null == data) {
            // 缓存里没有，从存储里读取
            result = this.storage.readGroup(group.getDomain().getName(), group.getId());
            if (null == result) {
                // 存储里没有
                return null;
            }

            this.cache.applyPut(result.getUniqueKey(), result.toJSON());
        }
        else {
            result = new Group(data);
        }

        return result;
    }
}
