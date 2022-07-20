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
            new StorageField("openid", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("session_key", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("unionid", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("account_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
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

    public long queryAccountIdByOpenId(String openId) {
        List<StorageField[]> result = this.storage.executeQuery(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("account_id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("openid", openId)
        });

        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].getLong();
    }

    public void writeAccountSession(long accountId, String device, String openId, String sessionKey, String unionId) {
        this.storage.executeInsert(TABLE_APPLET_SESSION, new StorageField[] {
                new StorageField("openid", openId),
                new StorageField("session_key", sessionKey),
                new StorageField("unionid", unionId),
                new StorageField("account_id", accountId),
                new StorageField("device", device),
                new StorageField("time", System.currentTimeMillis())
        });
    }
}
