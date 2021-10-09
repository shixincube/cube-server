/*
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

package cube.service.riskmgmt;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.service.riskmgmt.util.SensitiveWordBuildIn;
import cube.storage.StorageFactory;
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

    private final String sensitiveWordTablePrefix = "risk_sensitive_word_";

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

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> sensitiveWordTableNameMap;

    public MainStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "RickMgmtMainStorage", config);
        this.sensitiveWordTableNameMap = new HashMap<>();
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
            this.checkSensitiveWordTable(domain);
        }
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
}
