/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

import cell.core.talk.LiteralBase;
import cell.util.Base64;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.service.CipherMachine;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
            new StorageField("reminding", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("pivotal_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("recent_message_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("context", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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

                // 加密 Payload
                String payloadCiphertext = encrypt(message.getRemoteTimestamp(), message.getPayload().toString());

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
                        new StorageField("payload", LiteralBase.STRING, payloadCiphertext),
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
     * 以紧凑结构读出消息。
     *
     * @param domain
     * @param contactId
     * @param messageId
     * @return
     */
    public Message readCompact(String domain, Long contactId, Long messageId) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("id", LiteralBase.LONG),
                new StorageField("from", LiteralBase.LONG),
                new StorageField("to", LiteralBase.LONG),
                new StorageField("source", LiteralBase.LONG),
                new StorageField("owner", LiteralBase.LONG),
                new StorageField("lts", LiteralBase.LONG),
                new StorageField("rts", LiteralBase.LONG),
                new StorageField("state", LiteralBase.INT),
                new StorageField("scope", LiteralBase.INT)
        };

        List<StorageField[]> result = this.storage.executeQuery(table, fields, new Conditional[] {
                Conditional.createEqualTo("id", messageId),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", contactId)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        Message message = new Message(domain, map.get("id").getLong(), map.get("from").getLong(),
                map.get("to").getLong(), map.get("source").getLong(), map.get("owner").getLong(),
                map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                map.get("scope").getInt(),
                null, null, null);
        return message;
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
                Conditional.createEqualTo("id", messageId),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", contactId)
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

            long rts = map.get("rts").getLong();
            String payloadCiphertext = map.get("payload").getString();
            // 解密
            String payloadString = decrypt(rts, payloadCiphertext);
            payload = new JSONObject(payloadString);

            if (!map.get("attachment").isNullValue() && map.get("attachment").getString().length() > 2) {
                attachment = new JSONObject(map.get("attachment").getString());
            }
        } catch (JSONException e) {
            Logger.e(this.getClass(), "#read", e);
            return null;
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

                long rts = map.get("rts").getLong();
                String payloadCiphertext = map.get("payload").getString();
                // 解密
                String payloadString = decrypt(rts, payloadCiphertext);
                payload = new JSONObject(payloadString);

                if (!map.get("attachment").isNullValue() && map.get("attachment").getString().length() > 2) {
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

                long rts = map.get("rts").getLong();
                String payloadCiphertext = map.get("payload").getString();
                // 解密
                String payloadString = decrypt(rts, payloadCiphertext);
                payload = new JSONObject(payloadString);

                if (!map.get("attachment").isNullValue() && map.get("attachment").getString().length() > 2) {
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

                long rts = map.get("rts").getLong();
                String payloadCiphertext = map.get("payload").getString();
                // 解密
                String payloadString = decrypt(rts, payloadCiphertext);
                payload = new JSONObject(payloadString);

                if (!map.get("attachment").isNullValue() && map.get("attachment").getString().length() > 2) {
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

                long rts = map.get("rts").getLong();
                String payloadCiphertext = map.get("payload").getString();
                // 解密
                String payloadString = decrypt(rts, payloadCiphertext);
                payload = new JSONObject(payloadString);

                if (!map.get("attachment").isNullValue() && map.get("attachment").getString().length() > 2) {
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
                        new StorageField("state", LiteralBase.INT, state.code)
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
                            Conditional.createEqualTo(new StorageField("owner", contactId))
                    });
                }
            }
        });
    }

    /**
     * 修改消息状态。
     *
     * @param domain
     * @param contactId
     * @param messageIds
     * @param sourceState
     * @param newState
     * @return
     */
    public List<Long> writeMessagesState(String domain, Long contactId, List<Long> messageIds,
                                   MessageState sourceState, MessageState newState) {
        List<Long> resultIdList = new ArrayList<>();

        String table = this.messageTableNameMap.get(domain);

        StorageField[] fields = new StorageField[] {
                new StorageField("state", newState.code)
        };

        for (Long messageId : messageIds) {
            boolean updated = this.storage.executeUpdate(table, fields, new Conditional[] {
                    Conditional.createEqualTo(new StorageField("id", messageId)),
                    Conditional.createAnd(),
                    Conditional.createEqualTo(new StorageField("owner", contactId)),
                    Conditional.createAnd(),
                    Conditional.createEqualTo(new StorageField("state", sourceState.code))
            });

            if (updated) {
                // 选出符合条件的消息 ID
                resultIdList.add(messageId);
            }
        }

        return resultIdList;
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
     * 擦除消息负载及其相关数据。
     *
     * @param domain 指定域。
     * @param contactId 指定联系人 ID 。
     * @param messageId 指定消息 ID 。
     * @param payload 指定合规的新负载内容。
     */
    public void eraseMessagePayload(String domain, Long contactId, Long messageId, JSONObject payload) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 查询 rts 数据，以便加密
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                        new StorageField("rts", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("id", messageId.longValue()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("owner", contactId.longValue())
                });

                if (result.isEmpty()) {
                    return;
                }

                long rts = result.get(0)[0].getLong();
                String payloadCiphertext = encrypt(rts, payload.toString());

                storage.executeUpdate(table, new StorageField[] {
                        new StorageField("payload", payloadCiphertext),
                        new StorageField("attachment", "")
                }, new Conditional[] {
                        Conditional.createEqualTo("id", messageId.longValue()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("owner", contactId.longValue())
                });
            }
        });
    }

    /**
     * 计算指定域的消息条目数。
     *
     * @param domain
     * @return
     */
    public int countMessages(String domain) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return 0 ;
        }

        String sql = "SELECT COUNT(DISTINCT(id)) FROM `" + table + "`";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].getInt();
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

        String sql = "SELECT * FROM `" + table + "` WHERE `owner`=" + contact.getId()
                + " AND `state`<>" + ConversationState.Destroyed.toString() + " ORDER BY `timestamp` DESC";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            // 无数据
            return null;
        }

        List<Conversation> list = new ArrayList<>(result.size());

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);
//            if (map.get("state").getInt() == ConversationState.Destroyed.code) {
//                // 跳过已销毁的会话
//                continue;
//            }

            Long id = map.get("id").getLong();
            long timestamp = map.get("timestamp").getLong();
            Long owner = map.get("owner").getLong();
            ConversationType type = ConversationType.parse(map.get("type").getInt());
            ConversationState state = ConversationState.parse(map.get("state").getInt());
            Long pivotalId = map.get("pivotal_id").getLong();
            ConversationRemindType reminding = ConversationRemindType.parse(map.get("reminding").getInt());

            // 实例化
            Conversation conversation = new Conversation(id, domain, timestamp, owner, type, state, pivotalId, reminding);

            if (!map.get("context").isNullValue()) {
                conversation.setContext(new JSONObject(map.get("context").getString()));
            }

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
     * 写入指定新信息的会话。
     *
     * @param conversation 指定待更新的会话。
     */
    public void writeConversation(Conversation conversation) {
        long messageId = (null != conversation.getRecentMessage()) ? conversation.getRecentMessage().getId() : 0;
        this.writeConversation(conversation.getDomain().getName(), conversation.getOwnerId(),
                conversation.getPivotalId(), messageId,
                conversation.getTimestamp(), conversation.getType(), conversation.getState(),
                conversation.getRemindType(), conversation.getContext());
    }

    /**
     * 写入指定新信息的会话。
     *
     * @param domain
     * @param ownerId
     * @param pivotalId
     * @param messageId
     * @param timestamp
     * @param type
     * @param state
     * @param remindType
     * @param context
     */
    public void writeConversation(String domain, Long ownerId, Long pivotalId, long messageId,
                                   long timestamp, ConversationType type, ConversationState state,
                                   ConversationRemindType remindType, JSONObject context) {
        final String table = this.conversationTableNameMap.get(domain);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                List<StorageField[]> result = storage.executeQuery(table, new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("owner", ownerId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("pivotal_id", pivotalId)
                });

                if (result.isEmpty()) {
                    // 无数据，插入新数据
                    // 会话的 ID 就是关键实体的 ID
                    long conversationId = pivotalId.longValue();
                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("id", conversationId),
                            new StorageField("owner", ownerId),
                            new StorageField("pivotal_id", pivotalId),
                            new StorageField("timestamp", timestamp),
                            new StorageField("type", type.code),
                            new StorageField("state", state.code),
                            new StorageField("reminding", remindType.code),
                            new StorageField("recent_message_id", messageId),
                            new StorageField("context", (null != context) ? context.toString() : null)
                    });
                }
                else {
                    // 有数据，进行更新
                    long sn = result.get(0)[0].getLong();
                    storage.executeUpdate(table, new StorageField[] {
                            new StorageField("timestamp", timestamp),
                            new StorageField("state", state.code),
                            new StorageField("reminding", remindType.code),
                            new StorageField("recent_message_id", messageId),
                            new StorageField("context", (null != context) ? context.toString() : null)
                    }, new Conditional[] {
                            Conditional.createEqualTo("sn", sn)
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
            sql = "SELECT COUNT(DISTINCT `id`) FROM `" + table + "` WHERE `source`=0 AND `owner`=" + conversation.getOwnerId() +
                    " AND `from`=" + conversation.getPivotalId() +
                    " AND `state`=" + MessageState.Sent.code;
        }
        else if (conversation.getType() == ConversationType.Group) {
            sql = "SELECT COUNT(DISTINCT `id`) FROM `" + table + "` WHERE `source`=" + conversation.getPivotalId() +
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

    private String encrypt(long timestamp, String plaintext) {
        String ciphertext = null;

        byte[] keys = CipherMachine.getInstance().getCipher(timestamp);
        try {
            // 创建 Key
            SecretKey secretKey = new SecretKeySpec(keys, "AES");

            // AES 加密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] result = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Base64 编码
            ciphertext = Base64.encodeBytes(result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return ciphertext;
    }

    private String decrypt(long timestamp, String ciphertext) {
        String plaintext = null;

        byte[] keys = CipherMachine.getInstance().getCipher(timestamp);
        try {
            // 创建 Key
            SecretKey secretKey = new SecretKeySpec(keys, "AES");

            // Base64 解码
            byte[] bytes = Base64.decode(ciphertext.getBytes(StandardCharsets.UTF_8));

            // AES 解密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] result = cipher.doFinal(bytes);

            plaintext = new String(result, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plaintext = ciphertext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return plaintext;
    }
}
