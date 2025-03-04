/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.applet;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.app.server.util.AbstractStorage;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;

import java.util.List;
import java.util.Properties;

/**
 * 小程序对应的存储器。
 */
public class AppletStorage extends AbstractStorage {

    public final static String TABLE_APPLET_SESSION = "applet_session";

    private final StorageField[] appletSessionFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("js_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("openid", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("session_key", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("unionid", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("account_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    public AppletStorage(Properties properties) {
        super("AppletStorage", properties);
    }

    public void open() {
        this.storage.open();

        if (!this.storage.exist(TABLE_APPLET_SESSION)) {
            this.storage.executeCreate(TABLE_APPLET_SESSION, this.appletSessionFields);
        }

        Logger.i(this.getClass(), "Open");
    }

    public void close() {
        this.storage.close();

        Logger.i(this.getClass(), "Close");
    }

    /**
     *
     * @param jsCode
     * @return 返回 -1 表示没有记录。
     */
    public long queryAccountIdByJsCode(String jsCode) {
        List<StorageField[]> result = this.storage.executeQuery(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("account_id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("js_code", jsCode)
        });

        if (result.isEmpty()) {
            return -1;
        }

        return result.get(0)[0].getLong();
    }

    /**
     * 查询账号 ID 。
     *
     * @param openid
     * @param jsCode
     * @return
     */
    public long queryAccountIdByOpenId(String openid, String jsCode) {
        List<StorageField[]> result = this.storage.executeQuery(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("account_id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("openid", openid)
        });

        if (result.isEmpty()) {
            return -1;
        }

        // 更新 JS Code
        this.storage.executeUpdate(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("js_code", jsCode)
        }, new Conditional[] {
                Conditional.createEqualTo("openid", openid)
        });

        return result.get(0)[0].getLong();
    }

    public synchronized void writeSessionCode(String jsCode, String openId, String sessionKey, String unionId) {
        List<StorageField[]> result = this.storage.executeQuery(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("js_code", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("openid", openId)
        });

        if (result.isEmpty()) {
            // 插入新数据
            this.storage.executeInsert(TABLE_APPLET_SESSION, new StorageField[] {
                    new StorageField("js_code", jsCode),
                    new StorageField("openid", openId),
                    new StorageField("session_key", sessionKey),
                    new StorageField("unionid", LiteralBase.STRING, unionId),
                    new StorageField("time", System.currentTimeMillis())
            });
        }
        else {
            // 更新数据
            this.storage.executeUpdate(TABLE_APPLET_SESSION, new StorageField[] {
                    new StorageField("js_code", jsCode),
                    new StorageField("session_key", sessionKey),
                    new StorageField("unionid", LiteralBase.STRING, unionId),
                    new StorageField("time", System.currentTimeMillis())
            }, new Conditional[] {
                    Conditional.createEqualTo("openid", openId)
            });
        }
    }

    public void writeAccountSession(long accountId, String device, String jsCode) {
        this.storage.executeUpdate(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("account_id", accountId),
                new StorageField("device", device),
                new StorageField("time", System.currentTimeMillis())
        }, new Conditional[] {
                Conditional.createEqualTo("js_code", jsCode)
        });
    }
}
