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
import cube.common.entity.Group;
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

    private final String groupTablePrefix = "group_";

    private final String groupMemberTablePrefix = "group_member_";

    /**
     * 群组字段描述。
     */
    private final StorageField[] groupFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("owner", LiteralBase.LONG),
            new StorageField("creation_time", LiteralBase.LONG),
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
            new StorageField("contact_context", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> groupTableNameMap;
    private Map<String, String> groupMemberTableNameMap;

    public ContactStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.groupTableNameMap = new HashMap<>();
        this.groupMemberTableNameMap = new HashMap<>();
    }

    public ContactStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "ContactStorage", config);
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
            this.checkGroupTable(domain);
            this.checkGroupMemberTable(domain);
        }
    }

    public void writeContact(Contact contact) {

    }

    public Contact readContact(String domain, Long id) {
        return null;
    }

    public void writeGroup(final Group group, final Runnable completed) {
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
                                new StorageField("context", LiteralBase.STRING,
                                        (null == group.getContext()) ? null : group.getContext().toString())
                        }, new Conditional[] {
                                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, groupId))
                        });
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
                            Conditional.createEqualTo(new StorageField(groupTable, "id", LiteralBase.LONG),
                                    new StorageField(groupMemberTable, "group", LiteralBase.LONG))
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

            // 添加成员
            for (Contact member : members) {
                group.addMember(member);
            }

            // 是否有上下文数据
            if (!groupFields[4].isNullValue()) {
                JSONObject context = new JSONObject(groupFields[4].getString());
                group.setContext(context);
            }
        } catch (JSONException e) {
            Logger.e(this.getClass(), "JSON format error", e);
            return null;
        }

        return group;
    }

    public void writeGroupMember(final Group group, final Contact member, final Runnable completed) {
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
                storage.executeInsert(groupMemberTable, fields);

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    public void deleteGroupMember(final Group group, final Long memberId, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                storage.executeDelete(groupMemberTable, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, group.getId())),
                        Conditional.createAnd(),
                        Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, memberId))
                });

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    private void checkGroupTable(String domain) {
        String table = this.groupTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.groupTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[]{
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
