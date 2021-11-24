/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

    private final String contactZoneParticipantTablePrefix = "contact_zone_participant_";

    private final String groupTablePrefix = "group_";

    private final String groupMemberTablePrefix = "group_member_";

    private final String appendixTablePrefix = "appendix_";

    private final String topListTablePrefix = "contact_top_";

    private final String blockListTablePrefix = "contact_block_list_";

    /**
     * 联系人字段描述。
     */
    private final StorageField[] contactFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("timestamp", LiteralBase.LONG),
            new StorageField("context", LiteralBase.STRING),
            new StorageField("recent_device_name", LiteralBase.STRING),
            new StorageField("recent_device_platform", LiteralBase.STRING)
            //new StorageField("reserved", LiteralBase.STRING)
    };

    /**
     * 联系人分区字段描述。
     */
    private final StorageField[] contactZoneFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE, Constraint.NOT_NULL
            }),
            new StorageField("owner", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("display_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("peer_mode", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("context", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    /**
     * 联系人分区参与者字段描述。
     */
    private final StorageField[] contactZoneParticipantFields = new StorageField[] {
            new StorageField("contact_zone_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("type", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("inviter_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("postscript", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("context", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    /**
     * 群组字段描述。
     */
    private final StorageField[] groupFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("tag", LiteralBase.STRING),
            new StorageField("owner_id", LiteralBase.LONG),
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

    /**
     * 置顶列表。
     */
    private final StorageField[] topListFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("top_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("type", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    /**
     * 阻止列表。
     */
    private final StorageField[] blockListFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("block_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> contactTableNameMap;
    private Map<String, String> contactZoneTableNameMap;
    private Map<String, String> contactZoneParticipantTableNameMap;
    private Map<String, String> groupTableNameMap;
    private Map<String, String> groupMemberTableNameMap;
    private Map<String, String> appendixTableNameMap;

    public ContactStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.contactTableNameMap = new HashMap<>();
        this.contactZoneTableNameMap = new HashMap<>();
        this.contactZoneParticipantTableNameMap = new HashMap<>();
        this.groupTableNameMap = new HashMap<>();
        this.groupMemberTableNameMap = new HashMap<>();
        this.appendixTableNameMap = new HashMap<>();
    }

    public ContactStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "ContactStorage", config);
        this.contactTableNameMap = new HashMap<>();
        this.contactZoneTableNameMap = new HashMap<>();
        this.contactZoneParticipantTableNameMap = new HashMap<>();
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
            this.checkContactZoneParticipantTable(domain);
            this.checkGroupTable(domain);
            this.checkGroupMemberTable(domain);
            this.checkAppendixTable(domain);
            this.checkTopListTable(domain);
            this.checkBlockListTable(domain);
        }
    }

    /**
     * 返回类型。
     *
     * @return 返回类型。
     */
    public StorageType getStorageType() {
        return StorageFactory.getInstance().getStorageType(this.storage);
    }

    /**
     * 返回配置。
     *
     * @return 返回配置。
     */
    public JSONObject getConfig() {
        return this.storage.getConfig();
    }

    /**
     * 获取指定域当前的所有联系人总数。
     *
     * @param domain 指定域。
     * @return 返回指定域当前的所有联系人总数。
     */
    public int countContacts(String domain) {
        String table = this.contactTableNameMap.get(domain);

        StringBuilder sql = new StringBuilder("SELECT COUNT(id) FROM ");
        sql.append(table);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].getInt();
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
                            new StorageField("timestamp", LiteralBase.LONG, contact.getTimestamp()),
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
                    if (null != device) {
                        storage.executeUpdate(table, new StorageField[] {
                                new StorageField("name", LiteralBase.STRING, contact.getName()),
                                new StorageField("timestamp", LiteralBase.LONG, contact.getTimestamp()),
                                new StorageField("context", LiteralBase.STRING,
                                        (null != contact.getContext()) ? contact.getContext().toString() : null),
                                new StorageField("recent_device_name", LiteralBase.STRING, device.getName()),
                                new StorageField("recent_device_platform", LiteralBase.STRING, device.getPlatform())
                        }, new Conditional[] {
                                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, contact.getId()))
                        });
                    }
                    else {
                        storage.executeUpdate(table, new StorageField[] {
                                new StorageField("name", LiteralBase.STRING, contact.getName()),
                                new StorageField("timestamp", LiteralBase.LONG, contact.getTimestamp()),
                                new StorageField("context", LiteralBase.STRING,
                                        (null != contact.getContext()) ? contact.getContext().toString() : null)
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
            Map<String, StorageField> map = StorageFields.get(data);
            Long contactId = map.get("id").getLong();
            String name = map.get("name").getString();
            long timestamp = map.get("timestamp").getLong();
            JSONObject context = map.get("context").isNullValue() ? null : new JSONObject(map.get("context").getString());
            String deviceName = map.get("recent_device_name").isNullValue() ? null : map.get("recent_device_name").getString();
            String devicePlatform = map.get("recent_device_platform").isNullValue() ? null : map.get("recent_device_platform").getString();

            contact = new Contact(contactId, domain, name, timestamp);
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
     * 更新联系人时间戳。
     *
     * @param contact
     */
    public void updateContactTimestamp(Contact contact) {
        final String table = this.contactTableNameMap.get(contact.getDomain().getName());
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeUpdate(table, new StorageField[] {
                        new StorageField("timestamp", contact.getTimestamp())
                }, new Conditional[] {
                        Conditional.createEqualTo("id", contact.getId())
                });
            }
        });
    }

    /**
     * 读取联系人所有的区分列表。
     *
     * @param contact
     * @param timestamp
     * @param limit
     * @return
     */
    public List<ContactZone> readContactZoneList(Contact contact, long timestamp, int limit) {
        List<ContactZone> list = new ArrayList<>();

        String domain = contact.getDomain().getName();

        String table = this.contactZoneTableNameMap.get(domain);

        Conditional[] conditionals = null;
        if (limit > 0) {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("owner", contact.getId()),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", ContactZoneState.Normal.code),
                    Conditional.createAnd(),
                    Conditional.createGreaterThan(new StorageField("timestamp", timestamp)),
                    Conditional.createLimit(limit)
            };
        }
        else {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("owner", contact.getId()),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", ContactZoneState.Normal.code),
                    Conditional.createAnd(),
                    Conditional.createGreaterThan(new StorageField("timestamp", timestamp))
            };
        }

        // 查询
        List<StorageField[]> result = this.storage.executeQuery(table, this.contactZoneFields, conditionals);

        if (result.isEmpty()) {
            return list;
        }

        // 成员表
        table = this.contactZoneParticipantTableNameMap.get(domain);

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            ContactZone zone = new ContactZone(map.get("id").getLong(),
                    domain,
                    map.get("owner").getLong(),
                    map.get("name").getString(),
                    map.get("timestamp").getLong(),
                    map.get("display_name").getString(),
                    ContactZoneState.parse(map.get("state").getInt()),
                    map.get("peer_mode").getInt() == 1);

            // 查询成员
            List<StorageField[]> partResult = this.storage.executeQuery(table, this.contactZoneParticipantFields, new Conditional[] {
                    Conditional.createEqualTo("contact_zone_id", zone.getId()),
            });

            for (StorageField[] partRow : partResult) {
                Map<String, StorageField> data = StorageFields.get(partRow);
                ContactZoneParticipant participant = new ContactZoneParticipant(data.get("id").getLong(),
                        ContactZoneParticipantType.parse(data.get("type").getInt()),
                        data.get("timestamp").getLong(),
                        data.get("inviter_id").getLong(),
                        data.get("postscript").getString(),
                        ContactZoneParticipantState.parse(data.get("state").getInt()));
                zone.addParticipant(participant);
            }

            // 添加
            list.add(zone);
        }

        return list;
    }

    /**
     * 添加参与人到指定分区。
     *
     * @param zone
     * @param participant
     */
    public void addZoneParticipant(ContactZone zone, ContactZoneParticipant participant) {
        this.addZoneParticipant(zone.getDomain().getName(), zone.owner, zone.name, participant);
    }

    /**
     * 添加参与人到指定分区。
     *
     * @param domain
     * @param owner
     * @param name
     * @param participant
     */
    public void addZoneParticipant(String domain, long owner, String name, ContactZoneParticipant participant) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String table = contactZoneTableNameMap.get(domain);
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("owner", owner),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("name", name)
                });

                if (result.isEmpty()) {
                    // 没有找到 Zone
                    return;
                }

                long time = System.currentTimeMillis();
                Long zoneId = result.get(0)[0].getLong();

                // 更新时间戳
                storage.executeUpdate(table, new StorageField[] {
                        new StorageField("timestamp", time)
                }, new Conditional[] {
                        Conditional.createEqualTo("id", zoneId)
                });

                table = contactZoneParticipantTableNameMap.get(domain);

                // 删除旧数据
                storage.executeDelete(table, new Conditional[] {
                        Conditional.createEqualTo("contact_zone_id", zoneId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("id", participant.id)
                });

                // 插入新数据
                storage.executeInsert(table, new StorageField[] {
                        new StorageField("contact_zone_id", zoneId),
                        new StorageField("id", participant.id),
                        new StorageField("type", participant.type.code),
                        new StorageField("state", participant.state.code),
                        new StorageField("timestamp", participant.timestamp),
                        new StorageField("inviter_id", participant.inviterId),
                        new StorageField("postscript", participant.postscript)
                });
            }
        });
    }

    /**
     * 从指定分区移除参与人。
     *
     * @param domain
     * @param owner
     * @param name
     * @param participant
     */
    public void removeZoneParticipant(String domain, long owner, String name, ContactZoneParticipant participant) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String table = contactZoneTableNameMap.get(domain);
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("owner", LiteralBase.LONG, owner),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("name", LiteralBase.STRING, name)
                });

                if (result.isEmpty()) {
                    // 没有找到 Zone
                    return;
                }

                long time = System.currentTimeMillis();
                Long zoneId = result.get(0)[0].getLong();

                // 更新时间戳
                storage.executeUpdate(table, new StorageField[] {
                        new StorageField("timestamp", LiteralBase.LONG, time)
                }, new Conditional[] {
                        Conditional.createEqualTo("id", LiteralBase.LONG, zoneId)
                });

                table = contactZoneParticipantTableNameMap.get(domain);

                storage.executeDelete(table, new Conditional[] {
                        Conditional.createEqualTo("contact_zone_id", zoneId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("id", participant.id)
                });
            }
        });
    }

    /**
     * 删除指定指定参与人所在的 Zone 里的基础。
     * 即将指定 owner 里的所有 zone 里包含 participantId 的记录删除。
     *
     * @param domain
     * @param owner
     * @param participantId
     */
    public void clearParticipantInZones(String domain, long owner, Long participantId) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String table = contactZoneTableNameMap.get(domain);
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("owner", LiteralBase.LONG, owner)
                });

                if (result.isEmpty()) {
                    return;
                }

                long time = System.currentTimeMillis();
                String participantTable = contactZoneParticipantTableNameMap.get(domain);

                for (StorageField[] row : result) {
                    Long zoneId = row[0].getLong();

                    // 更新时间戳
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("timestamp", LiteralBase.LONG, time)
                    }, new Conditional[] {
                            Conditional.createEqualTo("id", LiteralBase.LONG, zoneId)
                    });

                    storage.executeDelete(participantTable, new Conditional[] {
                            Conditional.createEqualTo("contact_zone_id", LiteralBase.LONG, zoneId),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("id", LiteralBase.LONG, participantId)
                    });
                }
            }
        });
    }

    /**
     * 指定分区是否包含指定的联系人。
     *
     * @param domain
     * @param owner
     * @param name
     * @param participantId
     * @return
     */
    public boolean hasParticipantInZone(String domain, long owner, String name, Long participantId) {
        String table = this.contactZoneTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("owner", LiteralBase.LONG, owner),
                Conditional.createAnd(),
                Conditional.createEqualTo("name", LiteralBase.STRING, name)
        });

        if (result.isEmpty()) {
            return false;
        }

        Long zoneId = result.get(0)[0].getLong();

        table = this.contactZoneParticipantTableNameMap.get(domain);

        result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("state", LiteralBase.INT)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_zone_id", zoneId),
                Conditional.createAnd(),
                Conditional.createEqualTo("id", participantId)
        });

        return (!result.isEmpty());
    }

    /**
     * 写入联系人分区。
     *
     * @param zone
     * @param completed
     */
    public void writeContactZone(final ContactZone zone, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String table = contactZoneTableNameMap.get(zone.getDomain().getName());
                String participantTable = contactZoneParticipantTableNameMap.get(zone.getDomain().getName());

                Long zoneId = zone.getId();

                // 查询是否已存在
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                                new StorageField("id", LiteralBase.LONG)
                        }, new Conditional[] {
                                Conditional.createEqualTo("owner", zone.owner),
                                Conditional.createAnd(),
                                Conditional.createEqualTo("name", zone.name)
                        });

                if (result.isEmpty()) {
                    // 插入
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", zone.getId()),
                            new StorageField("owner", zone.owner),
                            new StorageField("name", zone.name),
                            new StorageField("display_name", zone.displayName),
                            new StorageField("peer_mode", zone.peerMode ? 1 : 0),
                            new StorageField("state", zone.state.code),
                            new StorageField("timestamp", zone.getTimestamp())
                    });
                }
                else {
                    // 更新

                    // 旧的 ID
                    zoneId = result.get(0)[0].getLong();

                    // 清空原参与人表
                    storage.executeDelete(participantTable, new Conditional[] {
                            Conditional.createEqualTo("contact_zone_id", zoneId)
                    });

                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("id", zone.getId()),
                            new StorageField("display_name", zone.displayName),
                            new StorageField("peer_mode", zone.peerMode ? 1 : 0),
                            new StorageField("state", zone.state.code),
                            new StorageField("timestamp", zone.getTimestamp())
                    }, new Conditional[] {
                            Conditional.createEqualTo("owner", zone.owner),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("name", zone.name)
                    });

                    // 新的 ID
                    zoneId = zone.getId();
                }

                // 参与人
                for (ContactZoneParticipant participant : zone.getParticipants()) {
                    storage.executeInsert(participantTable, new StorageField[] {
                            new StorageField("contact_zone_id", zoneId),
                            new StorageField("id", participant.id),
                            new StorageField("type", participant.type.code),
                            new StorageField("state", participant.state.code),
                            new StorageField("timestamp", participant.timestamp),
                            new StorageField("inviter_id", participant.inviterId),
                            new StorageField("postscript", participant.postscript),
                    });
                }

                if (null != completed) {
                    completed.run();
                }
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
        String table = this.contactZoneTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.contactZoneFields, new Conditional[] {
                Conditional.createEqualTo("owner", owner),
                Conditional.createAnd(),
                Conditional.createEqualTo("name", name)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        ContactZone zone = new ContactZone(map.get("id").getLong(),
                domain,
                owner,
                name,
                map.get("timestamp").getLong(),
                map.get("display_name").getString(),
                ContactZoneState.parse(map.get("state").getInt()),
                map.get("peer_mode").getInt() == 1);

        // 查询成员
        table = this.contactZoneParticipantTableNameMap.get(domain);
        result = this.storage.executeQuery(table, this.contactZoneParticipantFields, new Conditional[] {
                Conditional.createEqualTo("contact_zone_id", LiteralBase.LONG, zone.getId()),
        });

        for (StorageField[] row : result) {
            Map<String, StorageField> data = StorageFields.get(row);
            ContactZoneParticipant participant = new ContactZoneParticipant(data.get("id").getLong(),
                    ContactZoneParticipantType.parse(data.get("type").getInt()),
                    data.get("timestamp").getLong(),
                    data.get("inviter_id").getLong(),
                    data.get("postscript").getString(),
                    ContactZoneParticipantState.parse(data.get("state").getInt()));
            zone.addParticipant(participant);
        }

        return zone;
    }

    /**
     * 删除指定联系人分区。
     *
     * @param domain
     * @param owner
     * @param name
     */
    public void deleteContactZone(String domain, long owner, String name) {
        final String table = this.contactZoneTableNameMap.get(domain);
        this.executor.execute(() -> {
            storage.executeUpdate(table, new StorageField[] {
                    new StorageField("state", ContactZoneState.Deleted.code)
            }, new Conditional[] {
                    Conditional.createEqualTo("owner", owner),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("name", name)
            });
        });
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

                List<StorageField[]> list = storage.executeQuery(groupTable,
                        groupFields,
                        new Conditional[] { Conditional.createEqualTo(new StorageField("id", groupId)) });

                if (list.isEmpty()) {
                    // 执行插入
                    boolean successful = storage.executeInsert(groupTable, new StorageField[] {
                            new StorageField("id", LiteralBase.LONG, groupId),
                            new StorageField("name", LiteralBase.STRING, group.getName()),
                            new StorageField("tag", LiteralBase.STRING, group.getTag()),
                            new StorageField("owner_id", LiteralBase.LONG, group.getOwnerId()),
                            new StorageField("creation_time", LiteralBase.LONG, group.getCreationTime()),
                            new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                            new StorageField("state", LiteralBase.INT, group.getState().code),
                            new StorageField("context", LiteralBase.STRING,
                                    (null == group.getContext()) ? null : group.getContext().toString())
                    });

                    if (successful) {
                        // 插入成员数据
                        List<StorageField[]> data = new ArrayList<>(group.numMembers());
                        for (Long memberId : group.getMembers()) {
                            StorageField[] fields = new StorageField[] {
                                    new StorageField("group", LiteralBase.LONG, groupId),
                                    new StorageField("contact_id", LiteralBase.LONG, memberId),
                                    new StorageField("adding_time", LiteralBase.LONG, group.getLastActiveTime()),
                                    new StorageField("adding_operator", LiteralBase.LONG, group.getOwnerId())
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
                            new StorageField("owner_id", LiteralBase.LONG, group.getOwnerId()),
                            new StorageField("creation_time", LiteralBase.LONG, group.getCreationTime()),
                            new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                            new StorageField("state", LiteralBase.INT, group.getState().code),
                            new StorageField("context", LiteralBase.STRING,
                                    (null == group.getContext()) ? null : group.getContext().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, groupId))
                    });

                    if (writeMembers) {
                        // 更新成员列表
                        for (Long memberId : group.getMembers()) {
                            // 查询成员
                            List<StorageField[]> cr = storage.executeQuery(groupMemberTable, new StorageField[] {
                                    new StorageField("sn", LiteralBase.LONG)
                            }, new Conditional[] {
                                    Conditional.createEqualTo("group", LiteralBase.LONG, groupId),
                                    Conditional.createAnd(),
                                    Conditional.createEqualTo("contact_id", LiteralBase.LONG, memberId)
                            });

                            if (cr.isEmpty()) {
                                // 没有该成员数据，进行插入
                                // 成员数据字段
                                StorageField[] fields = new StorageField[] {
                                        new StorageField("group", LiteralBase.LONG, groupId),
                                        new StorageField("contact_id", LiteralBase.LONG, memberId),
                                        new StorageField("adding_time", LiteralBase.LONG, group.getLastActiveTime()),
                                        new StorageField("adding_operator", LiteralBase.LONG, group.getOwnerId())
                                };
                                storage.executeInsert(groupMemberTable, fields);
                            }
                            else {
                                // 有该成员数据
                                // Nothing
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
            Long ownerId = groupMap.get("owner_id").getLong();

            ArrayList<Long> members = new ArrayList<>(groupMemberResult.size());
            for (StorageField[] fields : groupMemberResult) {
                Map<String, StorageField> map = StorageFields.get(fields);
                Long contactId = map.get("contact_id").getLong();
                members.add(contactId);
            }

            group = new Group(groupMap.get("id").getLong(), domain, groupMap.get("name").getString(),
                    ownerId, groupMap.get("creation_time").getLong());
            group.setTag(groupMap.get("tag").getString());
            group.setLastActiveTime(groupMap.get("last_active").getLong());
            group.setState(GroupState.parse(groupMap.get("state").getInt()));

            // 添加成员
            for (Long memberId : members) {
                group.addMember(memberId);
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
     * @param groupState
     * @return
     */
    public List<Group> readGroupsWithMember(String domain, Long memberId,
                                            long beginningLastActive, long endingLastActive,
                                            int groupState) {
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
                        Conditional.createLessThan(new StorageField(groupTable, "last_active", LiteralBase.LONG, endingLastActive)),
                        Conditional.createAnd(),
                        // 群组的状态
                        Conditional.createEqualTo(new StorageField(groupTable, "state", LiteralBase.INT, groupState))
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

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeUpdate(groupTable, new StorageField[] {
                        new StorageField("name", LiteralBase.STRING, group.getName()),
                        new StorageField("tag", LiteralBase.STRING, group.getTag()),
                        new StorageField("owner_id", LiteralBase.LONG, group.getOwnerId()),
                        new StorageField("last_active", LiteralBase.LONG, group.getLastActiveTime()),
                        new StorageField("state", LiteralBase.INT, group.getState().code),
                        new StorageField("context", LiteralBase.STRING,
                                (null == group.getContext()) ? null : group.getContext().toString())
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                });
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
                    new StorageField("state", LiteralBase.INT, group.getState().code)
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
                            new StorageField("state", LiteralBase.INT, group.getState().code)
                    }, new Conditional[]{
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, group.getId()))
                    });
                }
            });
            return null;
        }
    }

    /**
     * 添加群成员。
     *
     * @param group
     * @param memberList
     * @param operatorId
     * @param completed
     */
    public void addGroupMembers(final Group group, final List<Long> memberList, final Long operatorId, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                Long time = System.currentTimeMillis();

                for (Long memberId : memberList) {
                    // 先查询该成员是否之前就在群里
                    List<StorageField[]> queryResult = storage.executeQuery(groupMemberTable, new StorageField[] {
                            new StorageField("sn", LiteralBase.LONG)
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, group.getId())),
                            Conditional.createAnd(),
                            Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, memberId))
                    });

                    if (queryResult.isEmpty()) {
                        // 没有记录，插入新记录
                        StorageField[] fields = new StorageField[]{
                                new StorageField("group", LiteralBase.LONG, group.getId()),
                                new StorageField("contact_id", LiteralBase.LONG, memberId),
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
    public void removeGroupMembers(final Group group, final List<Long> memberList, final Long operatorId, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String groupMemberTable = groupMemberTableNameMap.get(group.getDomain().getName());

                Long time = System.currentTimeMillis();

                for (Long memberId : memberList) {
                    storage.executeUpdate(groupMemberTable, new StorageField[] {
                            new StorageField("removing_time", LiteralBase.LONG, time),
                            new StorageField("removing_operator", LiteralBase.LONG, operatorId)
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("group", LiteralBase.LONG, group.getId())),
                            Conditional.createAnd(),
                            Conditional.createEqualTo(new StorageField("contact_id", LiteralBase.LONG, memberId))
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
        String table = this.appendixTableNameMap.get(appendix.getContact().getDomain().getName());

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> list = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getContact().getId()))
                });

                if (list.isEmpty()) {
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", LiteralBase.LONG, appendix.getContact().getId()),
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    });
                }
                else {
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getContact().getId()))
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
        String table = this.appendixTableNameMap.get(appendix.getGroup().getDomain().getName());

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> list = storage.executeQuery(table, new StorageField[] {
                        new StorageField("id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getGroup().getId()))
                });

                if (list.isEmpty()) {
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", LiteralBase.LONG, appendix.getGroup().getId()),
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    });
                }
                else {
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("appendix", LiteralBase.STRING, appendix.toJSON().toString())
                    }, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, appendix.getGroup().getId()))
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
                Conditional.createEqualTo(new StorageField("id", group.getId()))
        });

        if (result.isEmpty()) {
            return null;
        }

        String appendixContent = result.get(0)[1].getString();
        return new GroupAppendix(group, new JSONObject(appendixContent));
    }

    /**
     * 读取置顶列表。
     *
     * @param domain
     * @param contactId
     * @return
     */
    public List<JSONObject> readTopList(String domain, Long contactId) {
        String table = this.topListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        List<JSONObject> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                        new StorageField("top_id", LiteralBase.LONG),
                        new StorageField("type", LiteralBase.STRING)
                },
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId)
                });

        for (StorageField[] data : result) {
            JSONObject json = new JSONObject();
            json.put("id", data[0].getLong());
            json.put("type", data[1].getString());
            list.add(json);
        }

        return list;
    }

    /**
     * 写入置顶数据。
     *
     * @param domain
     * @param contactId
     * @param topId
     * @param type
     */
    public void writeTopList(String domain, Long contactId, Long topId, String type) {
        String table = this.topListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        List<StorageField[]> result = this.storage.executeQuery(table, this.topListFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("top_id", LiteralBase.LONG, topId)
        });

        if (!result.isEmpty()) {
            return;
        }

        this.storage.executeInsert(table, new StorageField[] {
                new StorageField("contact_id", LiteralBase.LONG, contactId),
                new StorageField("top_id", LiteralBase.LONG, topId),
                new StorageField("type", LiteralBase.STRING, type)
        });
    }

    /**
     * 删除置顶列表里的数据。
     *
     * @param domain
     * @param contactId
     * @param topId
     */
    public void deleteTopList(String domain, Long contactId, Long topId) {
        String table = this.topListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        this.storage.executeDelete(table, new Conditional[] {
                Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("top_id", LiteralBase.LONG, topId)
        });
    }

    /**
     * 判断是否被指定联系人阻止。
     *
     * @param domain
     * @param contactId
     * @param blockId
     * @return
     */
    public boolean hasBlocked(String domain, Long contactId, Long blockId) {
        String table = this.blockListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        List<StorageField[]> list = this.storage.executeQuery(table, this.blockListFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("block_id", LiteralBase.LONG, blockId)
        });

        return !list.isEmpty();
    }

    /**
     * 读取指定联系人的阻止列表。
     *
     * @param domain
     * @param contactId
     * @return
     */
    public List<Long> readBlockList(String domain, Long contactId) {
        List<Long> list = new ArrayList<>();

        String table = this.blockListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        List<StorageField[]> result = this.storage.executeQuery(table, this.blockListFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId)
        });

        for (StorageField[] data : result) {
            Map<String, StorageField> map = StorageFields.get(data);
            Long id = map.get("block_id").getLong();
            list.add(id);
        }

        return list;
    }

    /**
     * 写入新的阻止 ID 。
     *
     * @param domain
     * @param contactId
     * @param blockId
     */
    public void writeBlockList(String domain, Long contactId, Long blockId) {
        String table = this.blockListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        List<StorageField[]> result = this.storage.executeQuery(table, this.blockListFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("block_id", LiteralBase.LONG, blockId)
        });

        if (!result.isEmpty()) {
            return;
        }

        this.storage.executeInsert(table, new StorageField[] {
                new StorageField("contact_id", LiteralBase.LONG, contactId),
                new StorageField("block_id", LiteralBase.LONG, blockId),
                new StorageField("time", LiteralBase.LONG, System.currentTimeMillis())
        });
    }

    /**
     * 删除指定的阻止 ID 。
     *
     * @param domain
     * @param contactId
     * @param blockId
     */
    public void deleteBlockList(String domain, Long contactId, Long blockId) {
        String table = this.blockListTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        this.storage.executeDelete(table, new Conditional[] {
                Conditional.createEqualTo("contact_id", LiteralBase.LONG, contactId),
                Conditional.createAnd(),
                Conditional.createEqualTo("block_id", LiteralBase.LONG, blockId)
        });
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

        List<StorageField[]> list = this.storage.executeQuery(table, null, new Conditional[] {
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo("state", LiteralBase.INT, GroupState.Normal.code),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("tag", LiteralBase.STRING, GroupTag.Public)
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
            Long ownerId = map.get("owner_id").getLong();
            Group group = new Group(map.get("id").getLong(), domain, map.get("name").getString(), ownerId,
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
                    new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                            Constraint.DEFAULT_0
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

    private void checkContactZoneParticipantTable(String domain) {
        String table = this.contactZoneParticipantTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.contactZoneParticipantTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            if (this.storage.executeCreate(table, this.contactZoneParticipantFields)) {
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
                    new StorageField("owner_id", LiteralBase.LONG, new Constraint[] {
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

    private void checkTopListTable(String domain) {
        String table = this.topListTablePrefix + domain;

        table = SQLUtils.correctTableName(table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.topListFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkBlockListTable(String domain) {
        String table = this.blockListTablePrefix + domain;

        table = SQLUtils.correctTableName(table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.blockListFields)) {
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
                "\"avatar\": \"avatar01\"," +
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
                "\"avatar\": \"avatar13\"," +
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
                "\"avatar\": \"avatar15\"," +
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
                "\"avatar\": \"avatar09\"," +
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
                "\"avatar\": \"avatar12\"," +
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
