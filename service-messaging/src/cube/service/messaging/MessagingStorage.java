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

package cube.service.messaging;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.Message;
import cube.common.entity.MessageState;
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

//    private final String stateTablePrefix = "state_";

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
     * 消息状态描述。
     */
//    private final StorageField[] stateFields = new StorageField[] {
//            new StorageField("contact_id", LiteralBase.LONG),
//            new StorageField("message_id", LiteralBase.LONG),
//            new StorageField("state", LiteralBase.INT),
//            new StorageField("timestamp", LiteralBase.LONG)
//    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> messageTableNameMap;

//    private Map<String, String> stateTableNameMap;

    public MessagingStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.messageTableNameMap = new HashMap<>();
//        this.stateTableNameMap = new HashMap<>();
    }

    public MessagingStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "MessagingStorage", config);
        this.messageTableNameMap = new HashMap<>();
//        this.stateTableNameMap = new HashMap<>();
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

            // 检查状态表
//            this.checkStateTable(domain);
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
        String table = this.messageTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] fields = new StorageField[] {
                        new StorageField("state", LiteralBase.INT, state.getCode())
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

    /*private void checkStateTable(String domain) {
        String table = this.stateTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.stateTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("message_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("state", LiteralBase.INT, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }*/
}
