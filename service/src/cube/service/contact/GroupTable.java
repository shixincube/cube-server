/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cell.adapter.extra.memory.SharedMemory;
import cube.common.Domain;
import cube.common.UniqueKey;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.GroupState;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于暂存活跃群组的表格。
 */
public class GroupTable {

    private Domain domain;

    private SharedMemory cache;

    private ContactStorage storage;

    protected ConcurrentHashMap<Long, Group> groups;

    /**
     * 暂存群组的活跃时间，之后由守护任务同步到集群和存储。
     */
    protected ConcurrentHashMap<Long, AtomicLong> groupActiveTimeMap;

    public GroupTable(Domain domain, SharedMemory cache, ContactStorage storage) {
        this.domain = domain;
        this.cache = cache;
        this.storage = storage;
        this.groups = new ConcurrentHashMap<>();
        this.groupActiveTimeMap = new ConcurrentHashMap<>();
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
     * 更新群组。对群组进行比较，如果群组数据发生变化，则更新。
     *
     * @param group
     * @param force 是否强制更新，跳过比较。
     * @return
     */
    public Group updateGroup(Group group, boolean force) {
        Group current = get(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        boolean modified = false;

        if (!force) {
            if (!current.getName().equals(group.getName())) {
                modified = true;
                current.setName(group.getName());
            }

            if (!current.getOwnerId().equals(group.getOwnerId())) {
                modified = true;
                current.setOwnerId(group.getOwnerId());
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
        }
        else {
            modified = true;
            current.setName(group.getName());
            current.setOwnerId(group.getOwnerId());
            current.setContext(group.getContext());
        }

        if (modified) {
            current.setLastActiveTime(System.currentTimeMillis());
            if (current != group) {
                group.setLastActiveTime(current.getLastActiveTime());
            }

            this.cache.applyPut(current.getUniqueKey(), current.toJSON());
            this.storage.updateGroupWithoutMember(current);
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
        Group current = get(group);
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
    public void updateActiveTime(Group group, long timestamp) {
        AtomicLong activeTime = this.groupActiveTimeMap.get(group.getId());
        if (null == activeTime) {
            this.groupActiveTimeMap.put(group.getId(), new AtomicLong(timestamp));
            return;
        }

        if (activeTime.get() < timestamp) {
            activeTime.set(timestamp);
        }

        group.setLastActiveTime(activeTime.get());
    }

    protected void submitActiveTime() {
        Iterator<Map.Entry<Long, AtomicLong>> iter = this.groupActiveTimeMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, AtomicLong> entry = iter.next();
            Long groupId = entry.getKey();
            long timestamp = entry.getValue().get();

            Group current = this.get(groupId);
            if (null == current) {
                // 群组不存在，跳过
                iter.remove();
                continue;
            }

            if (current.getLastActiveTime() >= timestamp) {
                // 时间戳过期
                entry.getValue().set(current.getLastActiveTime());
                continue;
            }

            // 更新
            current.setLastActiveTime(timestamp);

            this.cache.applyPut(current.getUniqueKey(), current.toJSON());
            this.storage.updateGroupActiveTime(current);
        }
    }

    /**
     * 添加群组成员。
     *
     * @param group
     * @param addedContactList
     * @param operator
     * @return
     */
    public Group addGroupMembers(Group group, List<Long> addedContactList, Contact operator) {
        Group current = get(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        // 添加成员
        for (Long memberId : addedContactList) {
            current.addMember(memberId);
        }

        // 更新时间戳
        current.setLastActiveTime(System.currentTimeMillis());
        if (current != group) {
            group.setLastActiveTime(current.getLastActiveTime());
        }

        // 更新缓存
        this.cache.applyPut(current.getUniqueKey(), current.toJSON());

        this.storage.addGroupMembers(current, addedContactList, operator.getId(), new Runnable() {
            @Override
            public void run() {
                storage.updateGroupActiveTime(current);
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
    public Group removeGroupMembers(Group group, List<Long> removedContactList, Contact operator) {
        Group current = get(group);
        if (null == current) {
            this.groups.remove(group.getId());
            return null;
        }

        // 删除成员
        for (Long memberId : removedContactList) {
            current.removeMember(memberId);
        }

        // 更新时间戳
        current.setLastActiveTime(System.currentTimeMillis());
        if (current != group) {
            group.setLastActiveTime(current.getLastActiveTime());
        }

        // 更新缓存
        this.cache.applyPut(current.getUniqueKey(), current.toJSON());

        this.storage.removeGroupMembers(current, removedContactList, operator.getId(), new Runnable() {
            @Override
            public void run() {
                storage.updateGroupActiveTime(current);
            }
        });

        this.groups.put(current.getId(), current);
        return current;
    }

    /**
     *
     *
     * @param group
     * @return
     */
    private Group get(Group group) {
        return this.get(group.getId());
    }

    private Group get(Long groupId) {
        Group result = null;

        // 从集群里获取
        JSONObject data = this.cache.applyGet(UniqueKey.make(groupId, this.domain));
        if (null == data) {
            // 缓存里没有，从存储里读取
            result = this.storage.readGroup(this.domain.getName(), groupId);
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
