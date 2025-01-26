/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.storage;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.console.mgmt.ServiceServer;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.storage.StorageFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 服务单元信息存储器。
 */
public class ServiceStorage extends AbstractStorage {

    private final StorageField[] serviceFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTO_INCREMENT
            }),
            new StorageField("tag", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("deploy_path", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("config_path", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("cellets_path", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final String serviceTable = "service";

    public ServiceStorage(Properties properties) {
        super("ServiceStorage", properties);
    }

    public void open() {
        this.storage.open();
        this.autoCheckTable();
    }

    public void close() {
        this.storage.close();
    }

    public void writeServer(String tag, String deployPath, String configPath, String celletsPath) {
        StorageField[] fields = new StorageField[] {
                new StorageField("tag", LiteralBase.STRING, tag),
                new StorageField("deploy_path", LiteralBase.STRING, deployPath),
                new StorageField("config_path", LiteralBase.STRING, configPath),
                new StorageField("cellets_path", LiteralBase.STRING, celletsPath)
        };

        this.storage.executeInsert(this.serviceTable, fields);
    }

    public ServiceServer readServer(String tag, String deployPath) {
        List<StorageField[]> result = this.storage.executeQuery(this.serviceTable, this.serviceFields, new Conditional[] {
                Conditional.createEqualTo("tag", LiteralBase.STRING, tag),
                Conditional.createAnd(),
                Conditional.createEqualTo("deploy_path", LiteralBase.STRING, deployPath)
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        ServiceServer server = new ServiceServer(tag, deployPath, map.get("config_path").getString(),
                map.get("cellets_path").getString());
        return server;
    }

    public List<ServiceServer> listServers() {
        List<StorageField[]> list = this.storage.executeQuery(this.serviceTable, this.serviceFields);
        if (list.isEmpty()) {
            return null;
        }

        List<ServiceServer> result = new ArrayList<>();
        for (StorageField[] fields : list) {
            Map<String, StorageField> map = StorageFields.get(fields);
            ServiceServer server = new ServiceServer(map.get("tag").getString(),
                    map.get("deploy_path").getString(), map.get("config_path").getString(),
                    map.get("cellets_path").getString());
            result.add(server);
        }

        return result;
    }

    private void autoCheckTable() {
        if (!this.storage.exist(this.serviceTable)) {
            this.storage.executeCreate(this.serviceTable, this.serviceFields);
            Logger.i(this.getClass(), "Create table '" + this.serviceTable + "'");
        }
    }
}
