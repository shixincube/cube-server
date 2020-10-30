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

import cell.core.talk.LiteralBase;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Group;
import cube.common.entity.GroupState;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import cube.util.SQLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 联系人存储。
 */
public class ContactStorage {

    private final String version = "1.0";

    private final String contactTablePrefix = "contact_";

    private final String groupTablePrefix = "group_";

    private final String groupMemberTablePrefix = "group_member_";

    private final StorageField[] contactFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("context", LiteralBase.STRING),
            new StorageField("recent_device_name", LiteralBase.STRING),
            new StorageField("recent_device_platform", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    /**
     * 群组字段描述。
     */
    private final StorageField[] groupFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
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
            new StorageField("removing_time", LiteralBase.LONG),
            new StorageField("removing_operator", LiteralBase.LONG)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> contactTableNameMap;
    private Map<String, String> groupTableNameMap;
    private Map<String, String> groupMemberTableNameMap;

    public ContactStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.contactTableNameMap = new HashMap<>();
        this.groupTableNameMap = new HashMap<>();
        this.groupMemberTableNameMap = new HashMap<>();
    }

    public ContactStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "ContactStorage", config);
        this.contactTableNameMap = new HashMap<>();
        this.groupTableNameMap = new HashMap<>();
        this.groupMemberTableNameMap = new HashMap<>();
    }

    public void open() {
        this.storage.open();
    }

    public void close() {
        this.storage.close();
    }

    public void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            this.checkContactTable(domain);
            this.checkGroupTable(domain);
            this.checkGroupMemberTable(domain);
        }
    }

    public void writeContact(final Contact contact) {
        this.writeContact(contact, null, null);
    }

    public void writeContact(final Contact contact, final Device device) {
        this.writeContact(contact, device, null);
    }

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

    public Contact readContact(String domain, Long id) {
        List<StorageField[]> result = null;

        synchronized (this.storage) {
            String table = this.contactTableNameMap.get(domain);
            result = this.storage.executeQuery(table, this.contactFields,
                    new Conditional[] { Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, id)) });
        }

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

    public void writeGroup(final Group group) {
        this.writeGroup(group, true, null);
    }

    public void writeGroup(final Group group, final Runnable completed) {
        this.writeGroup(group, true, completed);
    }

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
                                                (null == contact.getContext()) ? null : contact.getContext().toString())
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
                            // TODO 更新成员列表
                        }
                    }
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    public Group readGroup(String domain, Long groupId) {
        String groupTable = this.groupTableNameMap.get(domain);
        String groupMemberTable = this.groupMemberTableNameMap.get(domain);

        StorageField[] memberFields = this.createGroupMemberFields(groupMemberTable);

        List<StorageField[]> groupResult = null;
        List<StorageField[]> groupMemberResult = null;
        synchronized (this.storage) {
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
        }

        if (groupResult.isEmpty() || groupMemberResult.isEmpty()) {
            return null;
        }

        Group group = null;

        try {
            StorageField[] groupFields = groupResult.get(0);
            Long ownerId = groupFields[2].getLong();
            Contact owner = null;

            ArrayList<Contact> members = new ArrayList<>(groupMemberResult.size());
            for (StorageField[] fields : groupMemberResult) {
                Long id = fields[1].getLong();
                String name = fields[2].getString();
                String context = fields[3].isNullValue() ? null : fields[3].getString();

                Contact contact = new Contact(id, domain, name);
                if (null != context) {
                    contact.setContext(new JSONObject(context));
                }
                members.add(contact);

                if (ownerId.equals(id)) {
                    owner = contact;
                }
            }

            group = new Group(groupFields[0].getLong(), domain, groupFields[1].getString(), owner, groupFields[3].getLong());
            group.setLastActiveTime(groupFields[4].getLong());
            group.setState(GroupState.parse(groupFields[5].getInt()));

            // 添加成员
            for (Contact member : members) {
                group.addMember(member);
            }

            // 是否有上下文数据
            if (!groupFields[6].isNullValue()) {
                JSONObject context = new JSONObject(groupFields[6].getString());
                group.setContext(context);
            }
        } catch (JSONException e) {
            Logger.e(this.getClass(), "JSON format error", e);
            return null;
        }

        return group;
    }

    public List<Group> readGroupsWithMember(String domain, Long memberId, long beginningLastActive, long endingLastActive) {
        List<Group> result = new ArrayList<>();

        String groupTable = this.groupTableNameMap.get(domain);
        String groupMemberTable = this.groupMemberTableNameMap.get(domain);

        synchronized (this.storage) {
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
        }

        return result;
    }

    public void updateGroupName(final Group group) {

    }

    /**
     * 更新群组的最近活跃时间戳。
     *
     * @param group
     */
    public void updateGroupActiveName(final Group group) {
        String domain = group.getDomain().getName();

        String groupTable = this.groupTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (storage) {
                    storage.executeUpdate(groupTable, new StorageField[] {
                            new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                    });
                }
            }
        });
    }

    /**
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
            synchronized (this.storage) {
                this.storage.executeUpdate(groupTable, new StorageField[]{
                        new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                        new StorageField("state", LiteralBase.INT, group.getState().getCode())
                }, new Conditional[]{
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                });
            }

            return this.readGroup(domain, group.getId());
        }
        else {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (storage) {
                        storage.executeUpdate(groupTable, new StorageField[]{
                                new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                                new StorageField("state", LiteralBase.INT, group.getState().getCode())
                        }, new Conditional[]{
                                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                        });
                    }
                }
            });
            return null;
        }
    }

    public void addGroupMember(final Group group, final Contact member, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                StorageField[] fields = new StorageField[] {
                        new StorageField("group", LiteralBase.LONG, group.getId()),
                        new StorageField("contact_id", LiteralBase.LONG, member.getId()),
                        new StorageField("contact_name", LiteralBase.STRING, member.getName()),
                        new StorageField("contact_context", LiteralBase.STRING,
                                (null == member.getContext()) ? null : member.getContext().toString())
                };

                // 插入数据
                synchronized (storage) {
                    storage.executeInsert(groupMemberTable, fields);
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    public void removeGroupMembers(final Group group, final List<Contact> memberList, final Long operatorId, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                Long time = System.currentTimeMillis();

                synchronized (storage) {
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
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
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

    private StorageField[] createGroupMemberFields(String table) {
        StorageField[] result = new StorageField[this.groupMembersFields.length];
        for (int i = 0; i < result.length; ++i) {
            StorageField src = this.groupMembersFields[i];
            result[i] = new StorageField(table, src.getName(), src.getLiteralBase());
        }
        return result;
    }
}
