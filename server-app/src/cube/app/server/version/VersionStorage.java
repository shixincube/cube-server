/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.version;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.app.server.util.AbstractStorage;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.storage.StorageFields;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 版本存储器。
 */
public class VersionStorage extends AbstractStorage {

    public final static String TABLE_VERSION = "version";

    private final StorageField[] versionFields = new StorageField[] {
            new StorageField("sn", LiteralBase.INT, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTO_INCREMENT
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("major", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("minor", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("revision", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("build", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("important", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("download", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    public VersionStorage(Properties properties) {
        super("VersionStorage", properties);
    }

    public void open() {
        this.storage.open();

        if (!this.storage.exist(TABLE_VERSION)) {
            this.storage.executeCreate(TABLE_VERSION, this.versionFields);
        }

        Logger.i(this.getClass(), "Open");
    }

    public void close() {
        this.storage.close();

        Logger.i(this.getClass(), "Close");
    }

    public AppVersion getVersion(String device) {
        List<StorageField[]> result = this.storage.executeQuery(TABLE_VERSION, this.versionFields,
                new Conditional[] {
                        Conditional.createLike("device", device)
                });

        if (result.isEmpty()) {
            return null;
        }

        AppVersion version = null;

        StorageField[] fields = result.get(0);
        Map<String, StorageField> map = StorageFields.get(fields);
        version = new AppVersion(map.get("device").getString(), map.get("major").getInt(), map.get("minor").getInt(),
                map.get("revision").getInt(), map.get("important").getInt() == 1);

        if (!map.get("build").isNullValue()) {
            version.setBuild(map.get("build").getString());
        }

        if (!map.get("download").isNullValue()) {
            version.setDownload(map.get("download").getString());
        }

        return version;
    }
}
