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

package cube.service.messaging;

import cell.core.talk.LiteralBase;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.entity.Message;
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
 * 消息存储器。
 */
public class MessagingStorage {

    private final String version = "1.0";

    private final String messageTablePrefix = "message_";

    /**
     * 消息字段描述。
     */
    private final StorageField[] messageFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("from", LiteralBase.LONG),
            new StorageField("to", LiteralBase.LONG),
            new StorageField("source", LiteralBase.LONG),
            new StorageField("lts", LiteralBase.LONG),
            new StorageField("rts", LiteralBase.LONG),
            new StorageField("device", LiteralBase.STRING),
            new StorageField("payload", LiteralBase.STRING)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> tableNameMap;

    public MessagingStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.tableNameMap = new HashMap<>();
    }

    public MessagingStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "MessagingStorage", config);
        this.tableNameMap = new HashMap<>();
    }

    public void open() {
        this.storage.open();
    }

    public void close() {
        this.storage.close();
    }

    public void execSelfChecking(List<String> domainNameList) {
        String table = "cube";

        StorageField[] fields = new StorageField[] {
                new StorageField("item", LiteralBase.STRING),
                new StorageField("desc", LiteralBase.STRING)
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
        }

        // 校验域对应的表
        for (String domain : domainNameList) {
            this.checkMessageTable(domain);
        }
    }

    public void write(final Message message) {
        this.write(message, null);
    }

    public void write(final Message message, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String domain = message.getDomain().getName();
                // 取表名
                String table = tableNameMap.get(domain);
                if (null == table) {
                    return;
                }

                StorageField[] fields = new StorageField[] {
                        new StorageField("id", LiteralBase.LONG, message.getId()),
                        new StorageField("from", LiteralBase.LONG, message.getFrom()),
                        new StorageField("to", LiteralBase.LONG, message.getTo()),
                        new StorageField("source", LiteralBase.LONG, message.getSource()),
                        new StorageField("lts", LiteralBase.LONG, message.getLocalTimestamp()),
                        new StorageField("rts", LiteralBase.LONG, message.getRemoteTimestamp()),
                        new StorageField("device", LiteralBase.STRING, message.getSourceDevice().toJSON().toString()),
                        new StorageField("payload", LiteralBase.STRING, message.getPayload().toString())
                };

                synchronized (storage) {
                    storage.executeInsert(table, fields);
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    public List<Message> read(String domain, List<Long> messageIdList) {
        // 取表名
        String table = this.tableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        Object[] values = new Object[messageIdList.size()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = messageIdList.get(i);
        }

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields,
                    new Conditional[] { Conditional.createIN(this.messageFields[0], values) });
        }

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            JSONObject device = null;
            JSONObject payload = null;
            try {
                device = new JSONObject(row[6].getString());
                payload = new JSONObject(row[7].getString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, row[0].getLong(), row[1].getLong(), row[2].getLong(), row[3].getLong(),
                    row[4].getLong(), row[5].getLong(), device, payload);
            messages.add(message);
        }

        return messages;
    }

    public List<Message> readWithFromOrderByTime(String domain, Long id, long timestamp) {
        // 取表名
        String table = this.tableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("from", LiteralBase.LONG, id)),
                            Conditional.createAnd(),
                            Conditional.createGreaterThan(new StorageField("rts", LiteralBase.LONG, timestamp))
            });
        }

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            JSONObject device = null;
            JSONObject payload = null;
            try {
                device = new JSONObject(row[6].getString());
                payload = new JSONObject(row[7].getString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, row[0].getLong(), row[1].getLong(), row[2].getLong(), row[3].getLong(),
                    row[4].getLong(), row[5].getLong(), device, payload);
            messages.add(message);
        }

        return messages;
    }

    public List<Message> readWithToOrderByTime(String domain, Long id, long timestamp) {
        // 取表名
        String table = this.tableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                    Conditional.createEqualTo(new StorageField("to", LiteralBase.LONG, id)),
                    Conditional.createAnd(),
                    Conditional.createGreaterThan(new StorageField("rts", LiteralBase.LONG, timestamp))
            });
        }

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            JSONObject device = null;
            JSONObject payload = null;
            try {
                device = new JSONObject(row[6].getString());
                payload = new JSONObject(row[7].getString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, row[0].getLong(), row[1].getLong(), row[2].getLong(), row[3].getLong(),
                    row[4].getLong(), row[5].getLong(), device, payload);
            messages.add(message);
        }

        return messages;
    }

    private void checkMessageTable(String domain) {
        String table = this.messageTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.tableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.UNIQUE
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
                    new StorageField("lts", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("rts", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("device", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("payload", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }
}
