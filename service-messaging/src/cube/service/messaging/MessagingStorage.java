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

package cube.service.messaging;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
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
 * 消息存储器。
 */
public class MessagingStorage implements Storagable {

    private final String version = "1.0";

    private final String messageTablePrefix = "message_";

    private final String conversationTablePrefix = "conversation_";

    /**
     * 消息字段描述。
     */
    private final StorageField[] messageFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("from", LiteralBase.LONG),
            new StorageField("to", LiteralBase.LONG),
            new StorageField("source", LiteralBase.LONG),
            new StorageField("owner", LiteralBase.LONG),
            new StorageField("lts", LiteralBase.LONG),
            new StorageField("rts", LiteralBase.LONG),
            new StorageField("state", LiteralBase.INT),
            new StorageField("scope", LiteralBase.INT),
            new StorageField("device", LiteralBase.STRING),
            new StorageField("payload", LiteralBase.STRING),
            new StorageField("attachment", LiteralBase.STRING)
    };

    /**
     * 会话描述。
     */
    private final StorageField[] conversationFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("owner", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("type", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("remind", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("pivotal_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("recent_message_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("avatar_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("avatar_url", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> messageTableNameMap;

    private Map<String, String> conversationTableNameMap;

    public MessagingStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.messageTableNameMap = new HashMap<>();
        this.conversationTableNameMap = new HashMap<>();
    }

    public MessagingStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "MessagingStorage", config);
        this.messageTableNameMap = new HashMap<>();
        this.conversationTableNameMap = new HashMap<>();
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
        /*String table = "cube";

        StorageField[] fields = new StorageField[] {
                new StorageField("item", LiteralBase.STRING, new Constraint[] {
                        Constraint.NOT_NULL
                }),
                new StorageField("desc", LiteralBase.STRING, new Constraint[] {
                        Constraint.NOT_NULL
                })
        };

        List<StorageField[]> result = this.storage.executeQuery(table, fields);
        if (result.isEmpty()) {
            // 数据库没有找到表，创建新表
            if (this.storage.executeCreate(table, fields)) {
                // 插入数据
                StorageField[] data = new StorageField[] {
                        new StorageField("item", LiteralBase.STRING, "version"),
                        new StorageField("desc", LiteralBase.STRING, this.version)
                };
                this.storage.executeInsert(table, data);
                Logger.i(this.getClass(), "Insert into 'cube' data");
            }
            else {
                Logger.e(this.getClass(), "Create table 'cube' failed - " + this.storage.getName());
            }
        }
        else {
            // 校验版本
            for (StorageField[] row : result) {
                if (row[0].getString().equals("version")) {
                    Logger.i(this.getClass(), "Message storage version " + row[1].getString());
                }
            }
        }*/

        // 校验域对应的表
        for (String domain : domainNameList) {
            // 检查消息表
            this.checkMessageTable(domain);

            // 检查会话表
            this.checkConversationTable(domain);
        }
    }

    /**
     * 写入消息。
     *
     * @param message
     */
    public void write(final Message message) {
        this.write(message, null);
    }

    /**
     * 写入消息。
     *
     * @param message
     * @param completed
     */
    public void write(final Message message, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String domain = message.getDomain().getName();
                // 取表名
                String table = messageTableNameMap.get(domain);
                if (null == table) {
                    return;
                }

                StorageField[] fields = new StorageField[] {
                        new StorageField("id", LiteralBase.LONG, message.getId()),
                        new StorageField("from", LiteralBase.LONG, message.getFrom()),
                        new StorageField("to", LiteralBase.LONG, message.getTo()),
                        new StorageField("source", LiteralBase.LONG, message.getSource()),
                        new StorageField("owner", LiteralBase.LONG, message.getOwner()),
                        new StorageField("lts", LiteralBase.LONG, message.getLocalTimestamp()),
                        new StorageField("rts", LiteralBase.LONG, message.getRemoteTimestamp()),
                        new StorageField("state", LiteralBase.INT, message.getState().getCode()),
                        new StorageField("scope", LiteralBase.INT, message.getScope()),
                        new StorageField("device", LiteralBase.STRING, message.getSourceDevice().toJSON().toString()),
                        new StorageField("payload", LiteralBase.STRING, message.getPayload().toString()),
                        new StorageField("attachment", LiteralBase.STRING,
                                (null != message.getAttachment()) ? message.getAttachment().toJSON().toString() : null)
                };

                storage.executeInsert(table, fields);

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    /**
     * 读取属于指定联系人的消息。
     *
     * @param domain
     * @param contactId
     * @param messageId
     * @return
     */
    public Message read(String domain, Long contactId, Long messageId) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                Conditional.createEqualTo("id", LiteralBase.LONG, messageId),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", LiteralBase.LONG, contactId)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        JSONObject device = null;
        JSONObject payload = null;
        JSONObject attachment = null;
        try {
            device = new JSONObject(map.get("device").getString());
            payload = new JSONObject(map.get("payload").getString());
            if (!map.get("attachment").isNullValue()) {
                attachment = new JSONObject(map.get("attachment").getString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Message message = new Message(domain, map.get("id").getLong(), map.get("from").getLong(),
                map.get("to").getLong(), map.get("source").getLong(), map.get("owner").getLong(),
                map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                map.get("scope").getInt(),
                device, payload, attachment);
        return message;
    }

    /**
     * 读取指定消息 ID 的所有消息。
     *
     * @param domain
     * @param messageId
     * @return
     */
    public List<Message> read(String domain, Long messageId) {
        List<Message> result = new ArrayList<>();

        String table = this.messageTableNameMap.get(domain);

        List<StorageField[]> list = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId))
        });

        for (StorageField[] row : list) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(), map.get("from").getLong(),
                    map.get("to").getLong(), map.get("source").getLong(), map.get("owner").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    map.get("scope").getInt(),
                    device, payload, attachment);

            result.add(message);
        }

        return result;
    }

    /**
     * 读取消息。
     *
     * @param domain
     * @param contactId
     * @param messageIdList
     * @return
     */
    public List<Message> read(String domain, Long contactId, List<Long> messageIdList) {
        // 取表名
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        Object[] values = new Object[messageIdList.size()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = messageIdList.get(i);
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.messageFields,
                new Conditional[] { Conditional.createIN(this.messageFields[0], values) });

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(), map.get("from").getLong(),
                    map.get("to").getLong(), map.get("source").getLong(), map.get("owner").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    map.get("scope").getInt(),
                    device, payload, attachment);

            if (message.getOwner().longValue() == contactId.longValue()) {
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * 按照指定的起止时间顺序读取消息。
     *
     * @param domain
     * @param contactId
     * @param beginning
     * @param ending
     * @return
     */
    public List<Message> readOrderByTime(String domain, Long contactId, long beginning, long ending) {
        // 取表名
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("owner", LiteralBase.LONG, contactId)),
                Conditional.createAnd(),
                Conditional.createGreaterThan(new StorageField("rts", LiteralBase.LONG, beginning)),
                Conditional.createAnd(),
                Conditional.createLessThanEqual(new StorageField("rts", LiteralBase.LONG, ending))
        });

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(), map.get("from").getLong(),
                    map.get("to").getLong(), map.get("source").getLong(), map.get("owner").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    map.get("scope").getInt(),
                    device, payload, attachment);

            if (message.getState() == MessageState.Read || message.getState() == MessageState.Sent) {
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * 按照指定的起止时间顺序读取消息。
     *
     * @param domain
     * @param groupId
     * @param beginning
     * @param ending
     * @return
     */
    public List<Message> readWithGroupOrderByTime(String domain, Long groupId, long beginning, long ending) {
        // 取表名
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("source", LiteralBase.LONG, groupId)),
                Conditional.createAnd(),
                Conditional.createGreaterThan(new StorageField("rts", LiteralBase.LONG, beginning)),
                Conditional.createAnd(),
                Conditional.createLessThanEqual(new StorageField("rts", LiteralBase.LONG, ending))
        });

        ArrayList<Long> idList = new ArrayList<>();
        ArrayList<Message> messages = new ArrayList<>(result.size());

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            // 判断 ID 是否重复
            Long messageId = map.get("id").getLong();
            if (idList.contains(messageId)) {
                continue;
            }

            idList.add(messageId);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, messageId, map.get("from").getLong(),
                    map.get("to").getLong(), map.get("source").getLong(), map.get("owner").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    map.get("scope").getInt(),
                    device, payload, attachment);

            if (message.getState() == MessageState.Read || message.getState() == MessageState.Sent) {
                messages.add(message);
            }
        }

        idList.clear();

        return messages;
    }

    /**
     * 写入消息状态。
     *
     * @param domain
     * @param messageId
     * @param state
     */
    public void writeMessageState(String domain, Long messageId, MessageState state) {
        String table = this.messageTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] fields = new StorageField[] {
                        new StorageField("state", LiteralBase.INT, state.getCode())
                };

                storage.executeUpdate(table, fields, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId))
                });
            }
        });
    }

    /**
     * 写入消息状态。
     *
     * @param domain
     * @param contactId
     * @param messageId
     * @param state
     */
    public void writeMessageState(String domain, Long contactId, Long messageId, MessageState state) {
        final String table = this.messageTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] fields = new StorageField[] {
                        new StorageField("state", LiteralBase.INT, state.code)
                };

                storage.executeUpdate(table, fields, new Conditional[] {
                        Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId)),
                        Conditional.createAnd(),
                        Conditional.createEqualTo(new StorageField("owner", LiteralBase.LONG, contactId))
                });
            }
        });
    }

    /**
     * 批量写入消息状态。
     *
     * @param domain
     * @param contactId
     * @param messageIds
     * @param state
     */
    public void writeMessagesState(String domain, Long contactId, List<Long> messageIds, MessageState state) {
        final String table = this.messageTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] fields = new StorageField[] {
                        new StorageField("state", state.code)
                };

                for (Long messageId : messageIds) {
                    storage.executeUpdate(table, fields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", messageId)),
                            Conditional.createAnd(),
                            Conditional.createEqualTo(new StorageField("owner", contactId.longValue()))
                    });
                }
            }
        });
    }

    /**
     * 读取消息状态。
     *
     * @param domain
     * @param contactId
     * @param messageId
     * @return
     */
    public MessageState readMessageState(String domain, Long contactId, Long messageId) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("state", LiteralBase.INT)
        };

        List<StorageField[]> result = this.storage.executeQuery(table, fields, new Conditional[]{
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId)),
                Conditional.createAnd(),
                Conditional.createEqualTo(new StorageField("owner", LiteralBase.LONG, contactId))
        });

        if (result.isEmpty()) {
            return null;
        }

        int state = result.get(0)[0].getInt();
        return MessageState.parse(state);
    }

    /**
     * 按照时间倒序读取会话列表。该方法仅返回状态正常的会话。
     *
     * @param contact
     * @return
     */
    public List<Conversation> readConversationByDescendingOrder(Contact contact) {
        String domain = contact.getDomain().getName();
        String table = this.conversationTableNameMap.get(domain);

        String sql = "SELECT * FROM `" + table + "` WHERE `owner`=" + contact.getId() + " ORDER BY `timestamp` DESC";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            // 无数据
            return null;
        }

        List<Conversation> list = new ArrayList<>(result.size());

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);
            if (map.get("state").getInt() == ConversationState.Deleted.code) {
                // 跳过已删除的会话
                continue;
            }

            Long id = map.get("id").getLong();
            long timestamp = map.get("timestamp").getLong();
            Long owner = map.get("owner").getLong();
            ConversationType type = ConversationType.parse(map.get("type").getInt());
            ConversationState state = ConversationState.parse(map.get("state").getInt());
            Long pivotalId = map.get("pivotal_id").getLong();
            ConversationRemindType remind = ConversationRemindType.parse(map.get("remind").getInt());

            // 实例化
            Conversation conversation = new Conversation(id, domain, timestamp, owner, type, state, pivotalId, remind);

            // 查消息
            Long recentMessageId = map.get("recent_message_id").getLong();
            Message message = this.read(domain, owner, recentMessageId);
            conversation.setRecentMessage(message);

            // Avatar
            if (!map.get("avatar_name").isNullValue()) {
                conversation.setAvatarName(map.get("avatar_name").getString());
            }
            if (!map.get("avatar_url").isNullValue()) {
                conversation.setAvatarURL(map.get("avatar_url").getString());
            }

            // 添加到列表
            list.add(conversation);
        }

        return list;
    }

    /**
     * 更新指定新信息的会话。
     *
     * @param domain
     * @param ownerId
     * @param pivotalId
     * @param messageId
     * @param timestamp
     * @param type
     */
    public void updateConversation(String domain, Long ownerId, Long pivotalId, Long messageId,
                                   long timestamp, ConversationType type) {
        final String table = this.conversationTableNameMap.get(domain);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("owner", LiteralBase.LONG, ownerId.longValue()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("pivotal_id", LiteralBase.LONG, pivotalId.longValue())
                });

                if (result.isEmpty()) {
                    // 无数据，插入新数据
                    // 会话的 ID 就是关键实体的 ID
                    long conversationId = pivotalId.longValue();
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", LiteralBase.LONG, conversationId),
                            new StorageField("owner", LiteralBase.LONG, ownerId.longValue()),
                            new StorageField("timestamp", LiteralBase.LONG, timestamp),
                            new StorageField("type", LiteralBase.INT, type.code),
                            new StorageField("state", LiteralBase.INT, ConversationState.Normal.code),
                            new StorageField("remind", LiteralBase.INT, ConversationRemindType.Normal.code),
                            new StorageField("pivotal_id", LiteralBase.LONG, pivotalId.longValue()),
                            new StorageField("recent_message_id", LiteralBase.LONG, messageId.longValue())
                    });
                }
                else {
                    // 有数据，进行更新
                    long sn = result.get(0)[0].getLong();
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("timestamp", LiteralBase.LONG, timestamp),
                            new StorageField("recent_message_id", LiteralBase.LONG, messageId.longValue()),
                    }, new Conditional[] {
                            Conditional.createEqualTo("sn", LiteralBase.LONG, sn)
                    });
                }
            }
        });
    }

    public int countUnread(Conversation conversation) {
        String domain = conversation.getDomain().getName();
        String table = this.messageTableNameMap.get(domain);
        String sql = null;

        if (conversation.getType() == ConversationType.Contact) {
            sql = "SELECT COUNT(`id`) FROM `" + table + "` WHERE `source`=0 AND `owner`=" + conversation.getOwnerId() +
                    " AND `from`=" + conversation.getPivotalId() +
                    " AND `state`=" + MessageState.Sent.code;
        }
        else if (conversation.getType() == ConversationType.Group) {
            sql = "SELECT COUNT(`id`) FROM `" + table + "` WHERE `source`=" + conversation.getPivotalId() +
                    " AND `owner`=" + conversation.getOwnerId() +
                    " AND `from`<>" + conversation.getOwnerId() +
                    " AND `state`=" + MessageState.Sent.code;
        }

        if (null == sql) {
            return 0;
        }

        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].getInt();
    }

    private void checkMessageTable(String domain) {
        String table = this.messageTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.messageTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("from", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("to", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("source", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("owner", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("lts", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("rts", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("state", LiteralBase.INT, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("scope", LiteralBase.INT, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("device", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("payload", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("attachment", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkConversationTable(String domain) {
        String table = this.conversationTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.conversationTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            if (this.storage.executeCreate(table, this.conversationFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }
}
