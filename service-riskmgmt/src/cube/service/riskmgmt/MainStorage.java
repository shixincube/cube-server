/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.riskmgmt;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.service.riskmgmt.util.SensitiveWord;
import cube.service.riskmgmt.util.SensitiveWordBuildIn;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 数据存储器。
 */
public class MainStorage implements Storagable {

    private final String contactRiskTablePrefix = "contact_risk_";

    private final String contactBehaviorTablePrefix = "contact_behavior_";

    private final String sensitiveWordTablePrefix = "risk_sensitive_word_";

    private final String transChainTrackTablePrefix = "trans_chain_track_";

    private final String transChainNodeTablePrefix = "trans_chain_node_";

    private final StorageField[] contactRiskFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("risk_mask", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] contactBehaviorFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("behavior", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("contact", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("parameter", LiteralBase.STRING, new Constraint[]{
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] sensitiveWordFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("word", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("type", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] transChainTrackFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("track", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("node_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] transChainNodeFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("event", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("who", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("who_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("what", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("what_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("when", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("method", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("previous_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("next_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> contactRiskTableNameMap;
    private Map<String, String> contactBehaviorTableNameMap;
    private Map<String, String> sensitiveWordTableNameMap;
    private Map<String, String> transChainTrackTableNameMap;
    private Map<String, String> transChainNodeTableNameMap;

    public MainStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "RickMgmtMainStorage", config);
        this.contactRiskTableNameMap = new HashMap<>();
        this.contactBehaviorTableNameMap = new HashMap<>();
        this.sensitiveWordTableNameMap = new HashMap<>();
        this.transChainTrackTableNameMap = new HashMap<>();
        this.transChainNodeTableNameMap = new HashMap<>();
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
            this.checkContactRiskTable(domain);
            this.checkContactBehaviorTable(domain);
            this.checkSensitiveWordTable(domain);
            this.checkChainTrackTable(domain);
            this.checkChainNodeTable(domain);
        }
    }

    public int readContactRiskMask(String domain, long contactId) {
        final String table = this.contactRiskTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.contactRiskFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId)
        });
        if (result.isEmpty()) {
            return 0;
        }

        Map<String, StorageField> data = StorageFields.get(result.get(0));
        return data.get("risk_mask").getInt();
    }

    public boolean writeContactRiskMask(String domain, long contactId, int mask) {
        final String table = this.contactRiskTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.contactRiskFields, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId)
        });
        if (result.isEmpty()) {
            // 插入
            return this.storage.executeInsert(table, new StorageField[] {
                    new StorageField("contact_id", contactId),
                    new StorageField("risk_mask", mask),
                    new StorageField("time", System.currentTimeMillis())
            });
        }
        else {
            // 更新
            return this.storage.executeUpdate(table, new StorageField[] {
                    new StorageField("risk_mask", mask),
                    new StorageField("time", System.currentTimeMillis())
            }, new Conditional[] {
                    Conditional.createEqualTo("contact_id", contactId)
            });
        }
    }

    public void writeContactBehavior(ContactBehavior contactBehavior) {
        final String table = this.contactBehaviorTableNameMap.get(contactBehavior.getDomain().getName());
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeInsert(table, new StorageField[] {
                        new StorageField("contact_id", contactBehavior.getContact().getId().longValue()),
                        new StorageField("behavior", contactBehavior.getBehavior()),
                        new StorageField("timestamp", contactBehavior.getTimestamp()),
                        new StorageField("contact", contactBehavior.getContact().toJSON().toString()),
                        new StorageField("device", (null == contactBehavior.getDevice()) ? null :
                                contactBehavior.getDevice().toJSON().toString()),
                        new StorageField("parameter", (null == contactBehavior.getParameter()) ? null :
                                contactBehavior.getParameter().toString())
                });
            }
        });
    }

    public List<ContactBehavior> readContactBehaviors(String domain, long contactId, long beginTime,
                                                      long endTime, String behavior) {
        List<ContactBehavior> list = new ArrayList<>();

        final String table = this.contactBehaviorTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, this.contactBehaviorFields,
                new Conditional[] {
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createGreaterThanEqual(new StorageField("timestamp", beginTime)),
                        Conditional.createAnd(),
                        Conditional.createLessThan(new StorageField("timestamp", endTime)),
                        (null != behavior) ? Conditional.createAnd() : null,
                        (null != behavior) ? Conditional.createEqualTo("behavior", behavior) : null,
                        Conditional.createOrderBy("timestamp", false)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            Contact contact = new Contact(new JSONObject(map.get("contact").getString()));
            // 创建行为
            ContactBehavior contactBehavior = new ContactBehavior(contact, map.get("behavior").getString());
            contactBehavior.setTimestamp(map.get("timestamp").getLong());
            if (!map.get("device").isNullValue()) {
                Device device = new Device(new JSONObject(map.get("device")));
                contactBehavior.setDevice(device);
            }
            if (!map.get("parameter").isNullValue()) {
                contactBehavior.setParameter(new JSONObject(map.get("parameter")));
            }

            list.add(contactBehavior);
        }

        return list;
    }

    public void writeSensitiveWord(String domain, SensitiveWord sensitiveWord) {
        String table = this.sensitiveWordTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.executeInsert(table, new StorageField[] {
                        new StorageField("word", LiteralBase.STRING, sensitiveWord.word),
                        new StorageField("type", LiteralBase.INT, sensitiveWord.type.code)
                });
            }
        });
    }

    public List<SensitiveWord> readAllSensitiveWords(String domain) {
        List<SensitiveWord> list = new ArrayList<>();

        String table = this.sensitiveWordTableNameMap.get(domain);
        List<StorageField[]> result = this.storage.executeQuery(table, new StorageField[] {
                new StorageField("word", LiteralBase.STRING),
                new StorageField("type", LiteralBase.INT)
        });
        if (result.isEmpty()) {
            return list;
        }

        for (StorageField[] row : result) {
            list.add(new SensitiveWord(row[0].getString(), row[1].getInt()));
        }
        return list;
    }

    public TransmissionChain queryTransmissionChain(String key) {
        return null;
    }

    public void expandTransmissionChain(String currentKey, String newKey) {

    }

    public void addTransmissionChainNode(ChainNode chainNode) {
        String domain = chainNode.getDomain().getName();
        String table = this.transChainTrackTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        synchronized (table) {
            List<String> tracks = chainNode.getTracks();
            for (String track : tracks) {
                List<StorageField[]> result = this.storage.executeQuery(table, this.transChainTrackFields,
                        new Conditional[] {
                                Conditional.createEqualTo("track", track),
                                Conditional.createAnd(),
                                Conditional.createEqualTo("node_id", chainNode.getId().longValue())
                        });
                if (result.isEmpty()) {
                    // 插入
                    this.storage.executeInsert(table, new StorageField[] {
                            new StorageField("track", track),
                            new StorageField("node_id", chainNode.getId().longValue()),
                            new StorageField("timestamp", chainNode.getTimestamp())
                    });
                }
            }
        }

        table = this.transChainNodeTableNameMap.get(domain);
        if (null == table) {
            return;
        }

        synchronized (table) {
            this.storage.executeInsert(table, new StorageField[] {
                    new StorageField("id", chainNode.getId().longValue()),
                    new StorageField("event", chainNode.getEvent()),
                    new StorageField("who", chainNode.getWho().toJSON().toString()),
                    new StorageField("who_id", chainNode.getWho().getId().longValue()),
                    new StorageField("what", chainNode.getWhat().toJSON().toString()),
                    new StorageField("what_id", chainNode.getWhat().getId().longValue()),
                    new StorageField("when", chainNode.getWhen()),
                    new StorageField("method", chainNode.getMethod().toJSON().toString())
            });
        }
    }

    private void checkContactRiskTable(String domain) {
        String table = this.contactRiskTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.contactRiskTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.contactRiskFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkContactBehaviorTable(String domain) {
        String table = this.contactBehaviorTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.contactBehaviorTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.contactBehaviorFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkSensitiveWordTable(String domain) {
        String table = this.sensitiveWordTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.sensitiveWordTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.sensitiveWordFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");

                // 插入内置的数据
                SensitiveWordBuildIn swbi = new SensitiveWordBuildIn();
                for (SensitiveWord word : swbi.sensitiveWords) {
                    this.writeSensitiveWord(domain, word);
                }
                swbi = null;
            }
        }
    }

    private void checkChainTrackTable(String domain) {
        String table = this.transChainTrackTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.transChainTrackTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.transChainTrackFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    private void checkChainNodeTable(String domain) {
        String table = this.transChainNodeTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.transChainNodeTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            if (this.storage.executeCreate(table, this.transChainNodeFields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }
}
