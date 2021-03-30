/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 联系人存储。
 */
public class ContactStorage implements Storagable {

    private final String version = "1.0";

    private final String contactTablePrefix = "contact_";

    private final String contactZoneTablePrefix = "contact_zone_";

    private final String groupTablePrefix = "group_";

    private final String groupMemberTablePrefix = "group_member_";

    private final String appendixTablePrefix = "appendix_";

    /**
     * 联系人字段描述。
     */
    private final StorageField[] contactFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("context", LiteralBase.STRING),
            new StorageField("recent_device_name", LiteralBase.STRING),
            new StorageField("recent_device_platform", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    /**
     * 联系人分区字段描述。
     */
    private final StorageField[] contactZoneFields = new StorageField[] {
            new StorageField("owner", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    /**
     * 群组字段描述。
     */
    private final StorageField[] groupFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("tag", LiteralBase.STRING),
            new StorageField("owner", LiteralBase.LONG),
            new StorageField("creation_time", LiteralBase.LONG),
            new StorageField("last_active", LiteralBase.LONG),
            new StorageField("state", LiteralBase.INT),
            new StorageField("context", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    /**
     * 群成员字段描述。
     */
    private final StorageField[] groupMembersFields = new StorageField[] {
            new StorageField("group", LiteralBase.LONG),
            new StorageField("contact_id", LiteralBase.LONG),
            new StorageField("contact_name", LiteralBase.STRING),
            new StorageField("contact_context", LiteralBase.STRING),
            new StorageField("adding_time", LiteralBase.LONG),
            new StorageField("adding_operator", LiteralBase.LONG),
            new StorageField("removing_time", LiteralBase.LONG),
            new StorageField("removing_operator", LiteralBase.LONG)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    /**
     * 附件表字段描述。
     */
    private final StorageField[] appendixFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("appendix", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> contactTableNameMap;
    private Map<String, String> contactZoneTableNameMap;
    private Map<String, String> groupTableNameMap;
    private Map<String, String> groupMemberTableNameMap;
    private Map<String, String> appendixTableNameMap;

    public ContactStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.contactTableNameMap = new HashMap<>();
        this.contactZoneTableNameMap = new HashMap<>();
        this.groupTableNameMap = new HashMap<>();
        this.groupMemberTableNameMap = new HashMap<>();
        this.appendixTableNameMap = new HashMap<>();
    }

    public ContactStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "ContactStorage", config);
        this.contactTableNameMap = new HashMap<>();
        this.contactZoneTableNameMap = new HashMap<>();
        this.groupTableNameMap = new HashMap<>();
        this.groupMemberTableNameMap = new HashMap<>();
        this.appendixTableNameMap = new HashMap<>();
    }

    @Override
    public void open() {
        this.storage.open();
    }

    @Override
    public void close() {
        this.storage.close();
    }

    @Override
    public void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            this.checkContactTable(domain);
            this.checkContactZoneTable(domain);
            this.checkGroupTable(domain);
            this.checkGroupMemberTable(domain);
            this.checkAppendixTable(domain);
        }
    }

    /**
     * 写入联系人数据。
     *
     * @param contact
     */
    public void writeContact(final Contact contact) {
        this.writeContact(contact, null, null);
    }

    /**
     * 写入联系人数据。
     *
     * @param contact
     * @param device
     */
    public void writeContact(final Contact contact, final Device device) {
        this.writeContact(contact, device, null);
    }

    /**
     * 写入联系人数据。
     *
     * @param contact
     * @param device
     * @param completed
     */
    public void writeContact(final Contact contact, final Device device, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String domain = contact.getDomain().getName();

                String table = contactTableNameMap.get(domain);

                synchronized (storage) {
                    List<StorageField[]> list = storage.executeQuery(table,
                            new StorageField[] { new StorageField("sn", LiteralBase.LONG) },
                            new Conditional[] {
                                    Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, contact.getId()))
                            });

                    if (list.isEmpty()) {
                        // 没有数据，插入新数据
                        storage.executeInsert(table, new StorageField[] {
                                new StorageField("id", LiteralBase.LONG, contact.getId()),
                                new StorageField("name", LiteralBase.STRING, contact.getName()),
                                new StorageField("context", LiteralBase.STRING,
                                        (null != contact.getContext()) ? contact.getContext().toString() : null),
                                new StorageField("recent_device_name", LiteralBase.STRING,
                                        (null != device) ? device.getName() : null),
                                new StorageField("recent_device_platform", LiteralBase.STRING,
                                        (null != device) ? device.getPlatform() : null)
                        });
                    }
                    else {
                        // 更新数据
                        storage.executeUpdate(table, new StorageField[] {
                                new StorageField("name", LiteralBase.STRING, contact.getName()),
                                new StorageField("context", LiteralBase.STRING,
                                        (null != contact.getContext()) ? contact.getContext().toString() : null),
                                new StorageField("recent_device_name", LiteralBase.STRING,
                                        (null != device) ? device.getName() : null),
                                new StorageField("recent_device_platform", LiteralBase.STRING,
                                        (null != device) ? device.getPlatform() : null)
                        }, new Conditional[] {
                                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, contact.getId()))
                        });
                    }
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    /**
     * 读取联系人数据。
     *
     * @param domain
     * @param id
     * @return
     */
    public Contact readContact(String domain, Long id) {
        List<StorageField[]> result = null;

        String table = this.contactTableNameMap.get(domain);
        result = this.storage.executeQuery(table, this.contactFields,
                new Conditional[] { Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, id)) });

        if (result.isEmpty()) {
            return null;
        }

        StorageField[] data = result.get(0);

        Contact contact = null;

        try {
            Long contactId = data[0].getLong();
            String name = data[1].getString();
            JSONObject context = data[2].isNullValue() ? null : new JSONObject(data[2].getString());
            String deviceName = data[3].isNullValue() ? null : data[3].getString();
            String devicePlatform = data[4].isNullValue() ? null : data[4].getString();

            contact = new Contact(contactId, domain, name);
            if (null != context) {
                contact.setContext(context);
            }
            if (null != deviceName && null != devicePlatform) {
                Device device = new Device(deviceName, devicePlatform);
                contact.addDevice(device);
            }
        } catch (Exception e) {
            // Nothing;
        }

        return contact;
    }

    /**
     * 增加 Zone 记录。
     *
     * @param domain
     * @param owner
     * @param name
     * @param contactId
     */
    public void addContactZone(String domain, long owner, String name, Long contactId) {
        final String table = this.contactZoneTableNameMap.get(domain);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> list = storage.executeQuery(table, new StorageField[] {
                        new StorageField("contact", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("owner", LiteralBase.LONG, owner),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("name", LiteralBase.STRING, name),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact", LiteralBase.LONG, contactId.longValue()),
                });
                if (!list.isEmpty()) {
                    return;
                }

                storage.executeInsert(table, new StorageField[] {
                        new StorageField("owner", LiteralBase.LONG, owner),
                        new StorageField("name", LiteralBase.STRING, name),
                        new StorageField("contact", LiteralBase.LONG, contactId.longValue()),
                        new StorageField("state", LiteralBase.INT, 0),
                        new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
                });
            }
        });
    }

    /**
     * 移除 Zone 记录。
     *
     * @param domain
     * @param owner
     * @param name
     * @param contactId
     */
    public void removeContactZone(String domain, long owner, String name, Long contactId) {
        final String table = this.contactZoneTableNameMap.get(domain);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeDelete(table, new Conditional[] {
                        Conditional.createEqualTo("owner", LiteralBase.LONG, owner),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("name", LiteralBase.STRING, name),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact", LiteralBase.LONG, contactId.longValue())
                });
            }
        });
    }

    /**
     * 读取 Zone 记录。
     *
     * @param domain
     * @param owner
     * @param name
     * @return
     */
    public ContactZone readContactZone(String domain, long owner, String name) {
        ContactZone zone = new ContactZone(domain, owner, name);

        String table = this.contactZoneTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.contactZoneFields, new Conditional[] {
                Conditional.createEqualTo("owner", LiteralBase.LONG, owner),
                Conditional.createAnd(),
                Conditional.createEqualTo("name", LiteralBase.STRING, name),
        });
        if (result.isEmpty()) {
            return zone;
        }

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);
            zone.contacts.add(map.get("contact").getLong());
        }

        return zone;
    }

    /**
     * 写入群组数据。
     *
     * @param group
     */
    public void writeGroup(final Group group) {
        this.writeGroup(group, true, null);
    }

    /**
     * 写入群组数据。
     *
     * @param group
     * @param completed
     */
    public void writeGroup(final Group group, final Runnable completed) {
        this.writeGroup(group, true, completed);
    }

    /**
     * 写入群组数据。
     *
     * @param group
     * @param writeMembers
     * @param completed
     */
    public void writeGroup(final Group group, final boolean writeMembers, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String domain = group.getDomain().getName();

                String groupTable = groupTableNameMap.get(domain);
                String groupMemberTable = groupMemberTableNameMap.get(domain);

                Long groupId = group.getId();

                synchronized (storage) {
                    List<StorageField[]> list = storage.executeQuery(groupTable,
                            groupFields,
                            new Conditional[] { Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, groupId)) });

                    if (list.isEmpty()) {
                        // 执行插入
                        boolean successful = storage.executeInsert(groupTable, new StorageField[] {
                                new StorageField("id", LiteralBase.LONG, groupId),
                                new StorageField("name", LiteralBase.STRING, group.getName()),
                                new StorageField("tag", LiteralBase.STRING, group.getTag()),
                                new StorageField("owner", LiteralBase.LONG, group.getOwner().getId()),
                                new StorageField("creation_time", LiteralBase.LONG, group.getCreationTime()),
                                new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                                new StorageField("state", LiteralBase.INT, group.getState().getCode()),
                                new StorageField("context", LiteralBase.STRING,
                                        (null == group.getContext()) ? null : group.getContext().toString())
                        });

                        if (successful) {
                            // 插入成员数据
                            List<StorageField[]> data = new ArrayList<>(group.numMembers());
                            for (Contact contact : group.getMembers()) {
                                StorageField[] fields = new StorageField[] {
                                        new StorageField("group", LiteralBase.LONG, groupId),
                                        new StorageField("contact_id", LiteralBase.LONG, contact.getId()),
                                        new StorageField("contact_name", LiteralBase.STRING, contact.getName()),
                                        new StorageField("contact_context", LiteralBase.STRING,
                                                (null == contact.getContext()) ? null : contact.getContext().toString()),
                                        new StorageField("adding_time", LiteralBase.LONG, group.getLastActiveTime()),
                                        new StorageField("adding_operator", LiteralBase.LONG, group.getOwner().getId())
                                };
                                data.add(fields);
                            }

                            // 插入
                            if (!storage.executeInsert(groupMemberTable, data)) {
                                Logger.e(this.getClass(), "Write group member error : " + group.toJSON().toString());
                            }
                        }
                        else {
                            Logger.e(this.getClass(), "Write new group error : " + group.toJSON().toString());
                        }
                    }
                    else {
                        // 执行更新
                        storage.executeUpdate(groupTable, new StorageField[] {
                                new StorageField("name", LiteralBase.STRING, group.getName()),
                                new StorageField("owner", LiteralBase.LONG, group.getOwner().getId()),
                                new StorageField("creation_time", LiteralBase.LONG, group.getCreationTime()),
                                new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                                new StorageField("state", LiteralBase.INT, group.getState().getCode()),
                                new StorageField("context", LiteralBase.STRING,
                                        (null == group.getContext()) ? null : group.getContext().toString())
                        }, new Conditional[] {
                                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, groupId))
                        });

                        if (writeMembers) {
                            // 更新成员列表
                            for (Contact contact : group.getMembers()) {
                                // 查询成员
                                List<StorageField[]> cr = storage.executeQuery(groupMemberTable, new StorageField[] {
                                        new StorageField("sn", LiteralBase.LONG)
                                }, new Conditional[] {
                                        Conditional.createEqualTo("group", LiteralBase.LONG, groupId),
                                        Conditional.createAnd(),
                                        Conditional.createEqualTo("contact_id", LiteralBase.LONG, contact.getId())
                                });

                                if (cr.isEmpty()) {
                                    // 没有该成员数据，进行插入
                                    // 成员数据字段
                                    StorageField[] fields = new StorageField[] {
                                            new StorageField("group", LiteralBase.LONG, groupId),
                                            new StorageField("contact_id", LiteralBase.LONG, contact.getId()),
                                            new StorageField("contact_name", LiteralBase.STRING, contact.getName()),
                                            new StorageField("contact_context", LiteralBase.STRING,
                                                    (null == contact.getContext()) ? null : contact.getContext().toString()),
                                            new StorageField("adding_time", LiteralBase.LONG, group.getLastActiveTime()),
                                            new StorageField("adding_operator", LiteralBase.LONG, group.getOwner().getId())
                                    };
                                    storage.executeInsert(groupMemberTable, fields);
                                }
                                else {
                                    // 有该成员数据，进行更新
                                    // 成员数据字段
                                    StorageField[] fields = new StorageField[] {
                                            new StorageField("contact_name", LiteralBase.STRING, contact.getName()),
                                            new StorageField("contact_context", LiteralBase.STRING,
                                                    (null == contact.getContext()) ? null : contact.getContext().toString())
                                    };
                                    storage.executeUpdate(groupMemberTable, fields, new Conditional[] {
                                            Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, groupId)),
                                            Conditional.createAnd(),
                                            Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, contact.getId()))
                                    });
                                }
                            }
                        }
                    }
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    /**
     * 读取群组数据。
     *
     * @param domain
     * @param groupId
     * @return
     */
    public Group readGroup(String domain, Long groupId) {
        String groupTable = this.groupTableNameMap.get(domain);
        String groupMemberTable = this.groupMemberTableNameMap.get(domain);

        StorageField[] memberFields = this.createGroupMemberFields(groupMemberTable);

        List<StorageField[]> groupResult = null;
        List<StorageField[]> groupMemberResult = null;

        // 查询群组信息
        groupResult = this.storage.executeQuery(groupTable, this.groupFields,
                new Conditional[] { Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, groupId)) });

        // 查询群组成员
        groupMemberResult = this.storage.executeQuery(new String[] { groupTable, groupMemberTable },
                memberFields, new Conditional[] {
                        Conditional.createEqualTo(new StorageField(groupTable, "id", LiteralBase.LONG, groupId)),
                        Conditional.createAnd(),
                        Conditional.createEqualTo(new StorageField(groupTable, "id", LiteralBase.LONG),
                                new StorageField(groupMemberTable, "group", LiteralBase.LONG)),
                        Conditional.createAnd(),
                        // 没有被移除的成员
                        Conditional.createEqualTo(
                                new StorageField(groupMemberTable, "removing_time", LiteralBase.LONG, 0L))
                });

        if (groupResult.isEmpty() || groupMemberResult.isEmpty()) {
            return null;
        }

        Group group = null;

        try {
            StorageField[] groupFields = groupResult.get(0);
            Map<String, StorageField> groupMap = StorageFields.get(groupFields);
            Long ownerId = groupMap.get("owner").getLong();
            Contact owner = null;

            ArrayList<Contact> members = new ArrayList<>(groupMemberResult.size());
            for (StorageField[] fields : groupMemberResult) {
                Map<String, StorageField> map = StorageFields.get(fields);
                Long id = map.get("contact_id").getLong();
                String name = map.get("contact_name").getString();
                String context = map.get("contact_context").isNullValue() ? null : map.get("contact_context").getString();

                Contact contact = new Contact(id, domain, name);
                if (null != context) {
                    contact.setContext(new JSONObject(context));
                }
                members.add(contact);

                if (ownerId.equals(id)) {
                    owner = contact;
                }
            }

            group = new Group(groupMap.get("id").getLong(), domain, groupMap.get("name").getString(),
                    owner, groupMap.get("creation_time").getLong());
            group.setTag(groupMap.get("tag").getString());
            group.setLastActiveTime(groupMap.get("last_active").getLong());
            group.setState(GroupState.parse(groupMap.get("state").getInt()));

            // 添加成员
            for (Contact member : members) {
                group.addMember(member);
            }

            // 是否有上下文数据
            if (!groupMap.get("context").isNullValue()) {
                JSONObject context = new JSONObject(groupMap.get("context").getString());
                group.setContext(context);
            }
        } catch (JSONException e) {
            Logger.e(this.getClass(), "JSON format error", e);
            return null;
        }

        return group;
    }

    /**
     * 读取包含指定成员的所有群组。
     *
     * @param domain
     * @param memberId
     * @param beginningLastActive
     * @param endingLastActive
     * @return
     */
    public List<Group> readGroupsWithMember(String domain, Long memberId, long beginningLastActive, long endingLastActive) {
        List<Group> result = new ArrayList<>();

        String groupTable = this.groupTableNameMap.get(domain);
        String groupMemberTable = this.groupMemberTableNameMap.get(domain);

        // 查询有该成员的群信息
        StorageField[] groupFields = new StorageField[] {
                new StorageField(groupTable, "id", LiteralBase.LONG)
        };
        // 查询群组成员
        List<StorageField[]> groupsResult = this.storage.executeQuery(new String[] { groupTable, groupMemberTable },
                groupFields, new Conditional[] {
                        // 群 ID 相等
                        Conditional.createEqualTo(new StorageField(groupTable, "id", LiteralBase.LONG),
                                new StorageField(groupMemberTable, "group", LiteralBase.LONG)),
                        Conditional.createAnd(),
                        // 成员 ID 相等
                        Conditional.createEqualTo(new StorageField(groupMemberTable, "contact_id", LiteralBase.LONG, memberId)),
                        Conditional.createAnd(),
                        // 群成员没有被移除
                        Conditional.createEqualTo(new StorageField(groupMemberTable, "removing_time", LiteralBase.LONG, 0L)),
                        Conditional.createAnd(),
                        // 活跃时间戳大于等于
                        Conditional.createGreaterThanEqual(new StorageField(groupTable, "last_active", LiteralBase.LONG, beginningLastActive)),
                        Conditional.createAnd(),
                        // 活跃时间戳小于
                        Conditional.createLessThan(new StorageField(groupTable, "last_active", LiteralBase.LONG, endingLastActive))
                });

        ArrayList<Long> ids = new ArrayList<>(groupsResult.size());
        for (StorageField[] fields : groupsResult) {
            Long groupId = fields[0].getLong();

            if (ids.contains(groupId)) {
                continue;
            }
            ids.add(groupId);

            Group group = this.readGroup(domain, groupId);
            result.add(group);
        }

        ids.clear();
        ids = null;

        return result;
    }

    /**
     * 更新群组数据，但是不更新群组成员数据。
     *
     * @param group
     */
    public void updateGroupWithoutMember(final Group group) {
        String domain = group.getDomain().getName();
        String groupTable = this.groupTableNameMap.get(domain);
        String groupMemberTable = this.groupMemberTableNameMap.get(domain);

        Contact owner = group.getOwner();

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (storage) {
                    storage.executeUpdate(groupTable, new StorageField[] {
                            new StorageField("name", LiteralBase.STRING, group.getName()),
                            new StorageField("tag", LiteralBase.STRING, group.getTag()),
                            new StorageField("owner", LiteralBase.LONG, group.getOwner().getId()),
                            new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                            new StorageField("state", LiteralBase.INT, group.getState().getCode()),
                            new StorageField("context", LiteralBase.STRING,
                                    (null == group.getContext()) ? null : group.getContext().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                    });

                    storage.executeUpdate(groupMemberTable, new StorageField[] {
                            new StorageField("contact_name", LiteralBase.STRING, owner.getName()),
                            new StorageField("contact_context", LiteralBase.STRING,
                                    (null == owner.getContext()) ? null : owner.getContext().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, group.getId())),
                            Conditional.createAnd(),
                            Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, owner.getId()))
                    });
                }
            }
        });
    }

    /**
     * 更新群组的最近活跃时间戳。
     *
     * @param group
     */
    public void updateGroupActiveTime(final Group group) {
        String domain = group.getDomain().getName();

        String groupTable = this.groupTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeUpdate(groupTable, new StorageField[] {
                        new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime())
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                });
            }
        });
    }

    /**
     * 更新群组状态。
     *
     * @param group
     * @param read 如果设置为 {@code true} 则更新状态数据之后从库里重新查询数据并返回。
     * @return
     */
    public Group updateGroupState(final Group group, boolean read) {
        String domain = group.getDomain().getName();

        String groupTable = this.groupTableNameMap.get(domain);

        if (read) {
            // 执行更新
            this.storage.executeUpdate(groupTable, new StorageField[]{
                    new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                    new StorageField("state", LiteralBase.INT, group.getState().getCode())
            }, new Conditional[]{
                    Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
            });

            return this.readGroup(domain, group.getId());
        }
        else {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    storage.executeUpdate(groupTable, new StorageField[]{
                            new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                            new StorageField("state", LiteralBase.INT, group.getState().getCode())
                    }, new Conditional[]{
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                    });
                }
            });
            return null;
        }
    }

    /**
     * 更新群成员信息。
     *
     * @param group
     * @param member
     */
    public void updateGroupMember(final Group group, final Contact member) {
        String domain = group.getDomain().getName();
        String groupMemberTable = this.groupMemberTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeUpdate(groupMemberTable, new StorageField[] {
                        new StorageField("contact_name", LiteralBase.STRING, member.getName()),
                        new StorageField("contact_context", LiteralBase.STRING,
                                (null != member.getContext()) ? member.getContext().toString() : null)
                }, new Conditional[] {
                        Conditional.createEqualTo("group", LiteralBase.LONG, group.getId()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", LiteralBase.LONG, member.getId())
                });
            }
        });

        // 更新活跃时间
        this.updateGroupActiveTime(group);
    }

    /**
     * 添加群成员。
     *
     * @param group
     * @param memberList
     * @param operatorId
     * @param completed
     */
    public void addGroupMembers(final Group group, final List<Contact> memberList, final Long operatorId, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                Long time = System.currentTimeMillis();

                for (Contact member : memberList) {
                    // 先查询该成员是否之前就在群里
                    List<StorageField[]> queryResult = storage.executeQuery(groupMemberTable, new StorageField[] {
                            new StorageField("sn", LiteralBase.LONG)
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, group.getId())),
                            Conditional.createAnd(),
                            Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, member.getId()))
                    });

                    if (queryResult.isEmpty()) {
                        // 没有记录，插入新记录
                        StorageField[] fields = new StorageField[]{
                                new StorageField("group", LiteralBase.LONG, group.getId()),
                                new StorageField("contact_id", LiteralBase.LONG, member.getId()),
                                new StorageField("contact_name", LiteralBase.STRING, member.getName()),
                                new StorageField("contact_context", LiteralBase.STRING,
                                        (null == member.getContext()) ? null : member.getContext().toString()),
                                new StorageField("adding_time", LiteralBase.LONG, time),
                                new StorageField("adding_operator", LiteralBase.LONG, operatorId)
                        };

                        // 插入数据
                        storage.executeInsert(groupMemberTable, fields);
                    }
                    else {
                        // 已经有记录，则更新记录
                        Long sn = queryResult.get(0)[0].getLong();

                        storage.executeUpdate(groupMemberTable, new StorageField[] {
                                new StorageField("contact_name", LiteralBase.STRING, member.getName()),
                                new StorageField("contact_context", LiteralBase.STRING,
                                        (null == member.getContext()) ? null : member.getContext().toString()),
                                new StorageField("adding_time", LiteralBase.LONG, time),
                                new StorageField("adding_operator", LiteralBase.LONG, operatorId),
                                new StorageField("removing_time", LiteralBase.LONG, 0L),
                                new StorageField("removing_operator", LiteralBase.LONG, 0L)
                        }, new Conditional[] {
                                Conditional.createEqualTo(new StorageField("sn", LiteralBase.LONG, sn))
                        });
                    }
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    /**
     * 删除群成员。
     *
     * @param group
     * @param memberList
     * @param operatorId
     * @param completed
     */
    public void removeGroupMembers(final Group group, final List<Contact> memberList, final Long operatorId, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                Long time = System.currentTimeMillis();

                for (Contact member : memberList) {
                    storage.executeUpdate(groupMemberTable, new StorageField[] {
                            new StorageField("removing_time", LiteralBase.LONG, time),
                            new StorageField("removing_operator", LiteralBase.LONG, operatorId)
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, group.getId())),
                            Conditional.createAnd(),
                            Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, member.getId()))
                    });
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    /**
     * 写入附录数据。
     *
     * @param appendix
     */
    public void writeAppendix(ContactAppendix appendix) {
        String table = this.appendixTableNameMap.get(appendix.getOwner().getDomain().getName());

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> list = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getOwner().getId()))
                });

                if (list.isEmpty()) {
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", LiteralBase.LONG, appendix.getOwner().getId()),
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    });
                }
                else {
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getOwner().getId()))
                    });
                }
            }
        });
    }

    /**
     * 读取附录数据。
     *
     * @param contact
     * @return
     */
    public ContactAppendix readAppendix(Contact contact) {
        String table = this.appendixTableNameMap.get(contact.getDomain().getName());

        List<StorageField[]> result = this.storage.executeQuery(table, this.appendixFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, contact.getId()))
        });

        if (result.isEmpty()) {
            return null;
        }

        String appendixContent = result.get(0)[1].getString();

        return new ContactAppendix(contact, new JSONObject(appendixContent));
    }

    /**
     * 写入附录数据。
     *
     * @param appendix
     */
    public void writeAppendix(GroupAppendix appendix) {
        String table = this.appendixTableNameMap.get(appendix.getOwner().getDomain().getName());

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> list = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getOwner().getId()))
                });

                if (list.isEmpty()) {
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", LiteralBase.LONG, appendix.getOwner().getId()),
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    });
                }
                else {
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getOwner().getId()))
                    });
                }
            }
        });
    }

    /**
     * 读取附录数据。
     *
     * @param group
     * @return
     */
    public GroupAppendix readAppendix(Group group) {
        String table = this.appendixTableNameMap.get(group.getDomain().getName());

        List<StorageField[]> result = this.storage.executeQuery(table, this.appendixFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
        });

        if (result.isEmpty()) {
            return null;
        }

        String appendixContent = result.get(0)[1].getString();
        return new GroupAppendix(group, new JSONObject(appendixContent));
    }

    /**
     * 搜索包含指定关键字的联系人。
     *
     * @param domain
     * @param keyword
     * @return 如果没有匹配的数据返回 {@code null} 值。
     */
    public List<Contact> searchContacts(String domain, String keyword) {
        List<Contact> result = null;
        String table = this.contactTableNameMap.get(domain);

        List<StorageField[]> list = this.storage.executeQuery(table, new StorageField[] {
                    new StorageField("id", LiteralBase.LONG),
                    new StorageField("name", LiteralBase.STRING),
                    new StorageField("context", LiteralBase.STRING)
            }, new Conditional[] {
                    Conditional.createLike("id", keyword),
                    Conditional.createOr(),
                    Conditional.createLike("name", keyword)
            });

        if (list.isEmpty()) {
            return null;
        }

        result = new ArrayList<>(list.size());

        for (StorageField[] field : list) {
            Contact contact = new Contact(field[0].getLong(), domain, field[1].getString());
            if (!field[2].isNullValue()) {
                contact.setContext(new JSONObject(field[2].getString()));
            }
            result.add(contact);
        }

        return result;
    }

    /**
     * 搜索包含指定关键字的群组。
     *
     * @param domain
     * @param keyword
     * @return 如果没有匹配的数据返回 {@code null} 值。
     */
    public List<Group> searchGroups(String domain, String keyword) {
        List<Group> result = null;
        String table = this.groupTableNameMap.get(domain);

        List<StorageField[]> list = this.storage.executeQuery(table, this.groupFields, new Conditional[] {
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo("state", LiteralBase.INT, GroupState.Normal),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("tag", LiteralBase.STRING, "public")
                    }),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createLike("id", keyword),
                            Conditional.createOr(),
                            Conditional.createLike("name", keyword)
                    })
            }
        );

        if (list.isEmpty()) {
            return null;
        }

        result = new ArrayList<>(list.size());

        for (StorageField[] data : list) {
            Map<String, StorageField> map = StorageFields.get(data);
            Contact owner = this.readContact(domain, map.get("owner").getLong());
            Group group = new Group(map.get("id").getLong(), domain, map.get("name").getString(), owner,
                    map.get("creation_time").getLong());
            group.setTag(map.get("tag").getString());
            group.setLastActiveTime(map.get("last_active").getLong());
            String strContext = map.get("context").isNullValue() ? null : map.get("context").getString();
            if (null != strContext) {
                group.setContext(new JSONObject(strContext));
            }

            result.add(group);
        }

        return result;
    }

    private void checkContactTable(String domain) {
        String table = this.contactTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.contactTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.UNIQUE, Constraint.NOT_NULL
                    }),
                    new StorageField("name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("context", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("recent_device_name", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("recent_device_platform", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }

            // 插入内建数据
            List<StorageField[]> data = this.buildinData();
            for (StorageField[] dataFields : data) {
                this.storage.executeInsert(table, dataFields);
            }
        }
    }

    private void checkContactZoneTable(String domain) {
        String table = this.contactZoneTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.contactZoneTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            if (this.storage.executeCreate(table, this.contactZoneFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkGroupTable(String domain) {
        String table = this.groupTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.groupTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.UNIQUE, Constraint.NOT_NULL
                    }),
                    new StorageField("name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("tag", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("owner", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("creation_time", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("last_active", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("state", LiteralBase.INT, new Constraint[] {
                            Constraint.DEFAULT_0
                    }),
                    new StorageField("context", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkGroupMemberTable(String domain) {
        String table = this.groupMemberTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.groupMemberTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[]{
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("group", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("contact_name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("contact_context", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    }),
                    new StorageField("adding_time", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("adding_operator", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("removing_time", LiteralBase.LONG, new Constraint[] {
                            Constraint.DEFAULT_0
                    }),
                    new StorageField("removing_operator", LiteralBase.LONG, new Constraint[] {
                            Constraint.DEFAULT_0
                    }),
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkAppendixTable(String domain) {
        String table = this.appendixTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.appendixTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY
                    }),
                    new StorageField("appendix", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("reserved", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private StorageField[] createGroupMemberFields(String table) {
        StorageField[] result = new StorageField[this.groupMembersFields.length];
        for (int i = 0; i < result.length; ++i) {
            StorageField src = this.groupMembersFields[i];
            result[i] = new StorageField(table, src.getName(), src.getLiteralBase());
        }
        return result;
    }

    private List<StorageField[]> buildinData() {
        List<StorageField[]> list = new ArrayList<>();

        JSONObject context = new JSONObject("{" +
                "\"id\": 50001001," +
                "\"account\": \"cube1\"," +
                "\"name\": \"李国诚\"," +
                "\"avatar\": \"avatar01.png\"," +
                "\"state\": 0," +
                "\"region\": \"北京\"," +
                "\"department\": \"产品中心\"," +
                "\"last\": 0" +
                "}");
        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, context.getLong("id")),
                new StorageField("name", LiteralBase.STRING, context.getString("name")),
                new StorageField("context", LiteralBase.STRING, context.toString())
        };
        list.add(fields);

        context = new JSONObject("{" +
                "\"id\": 50001002," +
                "\"account\": \"cube2\"," +
                "\"name\": \"王沛珊\"," +
                "\"avatar\": \"avatar13.png\"," +
                "\"state\": 0," +
                "\"region\": \"武汉\"," +
                "\"department\": \"媒介部\"," +
                "\"last\": 0" +
                "}");
        fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, context.getLong("id")),
                new StorageField("name", LiteralBase.STRING, context.getString("name")),
                new StorageField("context", LiteralBase.STRING, context.toString())
        };
        list.add(fields);

        context = new JSONObject("{" +
                "\"id\": 50001003," +
                "\"account\": \"cube3\"," +
                "\"name\": \"郝思雁\"," +
                "\"avatar\": \"avatar15.png\"," +
                "\"state\": 0," +
                "\"region\": \"上海\"," +
                "\"department\": \"公关部\"," +
                "\"last\": 0" +
                "}");
        fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, context.getLong("id")),
                new StorageField("name", LiteralBase.STRING, context.getString("name")),
                new StorageField("context", LiteralBase.STRING, context.toString())
        };
        list.add(fields);

        context = new JSONObject("{" +
                "\"id\": 50001004," +
                "\"account\": \"cube4\"," +
                "\"name\": \"高海光\"," +
                "\"avatar\": \"avatar09.png\"," +
                "\"state\": 0," +
                "\"region\": \"成都\"," +
                "\"department\": \"技术部\"," +
                "\"last\": 0" +
                "}");
        fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, context.getLong("id")),
                new StorageField("name", LiteralBase.STRING, context.getString("name")),
                new StorageField("context", LiteralBase.STRING, context.toString())
        };
        list.add(fields);

        context = new JSONObject("{" +
                "\"id\": 50001005," +
                "\"account\": \"cube5\"," +
                "\"name\": \"张明宇\"," +
                "\"avatar\": \"avatar12.png\"," +
                "\"state\": 0," +
                "\"region\": \"广州\"," +
                "\"department\": \"设计部\"," +
                "\"last\": 0" +
                "}");
        fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG, context.getLong("id")),
                new StorageField("name", LiteralBase.STRING, context.getString("name")),
                new StorageField("context", LiteralBase.STRING, context.toString())
        };
        list.add(fields);

        return list;
    }
}
