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

package cube.console.storage;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.console.mgmt.DispatcherServer;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.storage.StorageFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 网关机信息存储器。
 */
public class DispatcherStorage extends AbstractStorage {

    private final StorageField[] dispatcherFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTO_INCREMENT
            }),
            new StorageField("tag", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("deploy_path", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("config", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("properties", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final String dispatcherTable = "dispatcher";

    public DispatcherStorage(Properties properties) {
        super("DispatcherStorage", properties);
    }

    public void open() {
        this.storage.open();
        this.autoCheckTable();
    }

    public void close() {
        this.storage.close();
    }

    public void writeServer(String tag, String deployPath, String cellConfigFile, String propertiesFile) {
        StorageField[] fields = new StorageField[] {
                new StorageField("tag", LiteralBase.STRING, tag),
                new StorageField("deploy_path", LiteralBase.STRING, deployPath),
                new StorageField("config", LiteralBase.STRING, cellConfigFile),
                new StorageField("properties", LiteralBase.STRING, propertiesFile)
        };

        this.storage.executeInsert(this.dispatcherTable, fields);
    }

    public DispatcherServer readServer(String tag, String deployPath) {
        List<StorageField[]> result = this.storage.executeQuery(this.dispatcherTable, this.dispatcherFields, new Conditional[] {
                Conditional.createEqualTo("tag", LiteralBase.STRING, tag),
                Conditional.createAnd(),
                Conditional.createEqualTo("deploy_path", LiteralBase.STRING, deployPath)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        DispatcherServer server = new DispatcherServer(tag, deployPath, map.get("config").getString(),
                map.get("properties").getString());
        return server;
    }

    public List<DispatcherServer> listServers() {
        List<StorageField[]> list = this.storage.executeQuery(this.dispatcherTable, this.dispatcherFields);
        if (list.isEmpty()) {
            return null;
        }

        List<DispatcherServer> result = new ArrayList<>();
        for (StorageField[] fields : list) {
            Map<String, StorageField> map = StorageFields.get(fields);
            DispatcherServer server = new DispatcherServer(map.get("tag").getString(),
                    map.get("deploy_path").getString(), map.get("config").getString(), map.get("properties").getString());
            result.add(server);
        }

        return result;
    }

    private void autoCheckTable() {
        if (!this.storage.exist(this.dispatcherTable)) {
            this.storage.executeCreate(this.dispatcherTable, this.dispatcherFields);
            Logger.i(this.getClass(), "Create table '" + this.dispatcherTable + "'");
        }
    }
}
