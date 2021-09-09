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

package cube.app.server.account;

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
 * 账号存储器。
 */
public class AccountStorage extends AbstractStorage {

    public final static String TABLE_ACCOUNT = "account";

    public final static String TABLE_TOKEN = "token";

    private final StorageField[] accountFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            new StorageField("account", LiteralBase.STRING, new Constraint[] {
                    Constraint.UNIQUE, Constraint.NOT_NULL
            }),
            new StorageField("phone", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_EMPTY
            }),
            new StorageField("password", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("avatar", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("region", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_EMPTY
            }),
            new StorageField("department", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_EMPTY
            }),
            new StorageField("registration", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] tokenFields = new StorageField[]{
            new StorageField("id", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("account_id", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("token", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_EMPTY
            }),
            new StorageField("creation", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("expire", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
    };

    public AccountStorage(Properties properties) {
        super("AccountStorage", properties);
    }

    public void open() {
        this.storage.open();

        if (!this.storage.exist(TABLE_ACCOUNT)) {
            this.storage.executeCreate(TABLE_ACCOUNT, this.accountFields);
        }

        if (!this.storage.exist(TABLE_TOKEN)) {
            this.storage.executeCreate(TABLE_TOKEN, this.tokenFields);
        }

        Logger.i(this.getClass(), "Open");
    }

    public void close() {
        this.storage.close();

        Logger.i(this.getClass(), "Close");
    }

    public Token readToken(String code) {
        String sql = "SELECT * FROM " + TABLE_TOKEN + " WHERE `token`='" + code + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Token token = new Token(dataMap.get("id").getLong(), dataMap.get("account_id").getLong(),
                dataMap.get("token").getString(), dataMap.get("device").getString(),
                dataMap.get("creation").getLong(), dataMap.get("expire").getLong());
        return token;
    }

    public Token writeToken(Token token) {
        if (token.id < 0) {
            this.storage.executeInsert(TABLE_TOKEN, new StorageField[] {
                    new StorageField("account_id", LiteralBase.LONG, token.accountId),
                    new StorageField("token", LiteralBase.STRING, token.code),
                    new StorageField("device", LiteralBase.STRING, token.device),
                    new StorageField("creation", LiteralBase.LONG, token.creation),
                    new StorageField("expire", LiteralBase.LONG, token.expire)
            });
        }
        else {
            this.storage.executeUpdate(TABLE_TOKEN, new StorageField[] {
                    new StorageField("expire", LiteralBase.LONG, token.expire)
            }, new Conditional[] {
                    Conditional.createEqualTo("id", LiteralBase.LONG, token.id)
            });
        }

        return this.readToken(token.code);
    }

    public Account readAccount(Long accountId) {
        String sql = "SELECT * FROM " + TABLE_ACCOUNT + " WHERE id=" + accountId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Account account = new Account(dataMap.get("id").getLong(), dataMap.get("account").getString(),
                dataMap.get("phone").getString(), dataMap.get("password").getString(), dataMap.get("name").getString(),
                dataMap.get("avatar").getString(), dataMap.get("state").getInt());
        account.registration = dataMap.get("registration").getLong();
        account.region = dataMap.get("region").getString();
        account.department = dataMap.get("department").getString();
        return account;
    }

    public Account readAccount(String accountName, String password) {
        String sql = "SELECT * FROM " + TABLE_ACCOUNT + " WHERE `account`='" + accountName + "' AND `password`='" + password + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Account account = new Account(dataMap.get("id").getLong(), dataMap.get("account").getString(),
                dataMap.get("phone").getString(), dataMap.get("password").getString(), dataMap.get("name").getString(),
                dataMap.get("avatar").getString(), dataMap.get("state").getInt());
        account.registration = dataMap.get("registration").getLong();
        account.region = dataMap.get("region").getString();
        account.department = dataMap.get("department").getString();
        return account;
    }

    public Account writeAccount(Long accountId, String accountName, String password, String nickname, String avatar) {
        long registration = System.currentTimeMillis();
        boolean result = this.storage.executeInsert(TABLE_ACCOUNT, new StorageField[]{
                new StorageField("id", LiteralBase.LONG, accountId),
                new StorageField("account", LiteralBase.STRING, accountName),
                new StorageField("password", LiteralBase.STRING, password),
                new StorageField("name", LiteralBase.STRING, nickname),
                new StorageField("avatar", LiteralBase.STRING, avatar),
                new StorageField("registration", LiteralBase.LONG, registration)
        });

        if (!result) {
            return null;
        }

        Account account = new Account(accountId, accountName, "", password, nickname, avatar, 0);
        account.registration = registration;
        account.region = "--";
        account.department = "--";
        return account;
    }

    public boolean existsAccountId(Long accountId) {
        String sql = "SELECT `state` FROM " + TABLE_ACCOUNT + " WHERE `id`=" + accountId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return (!result.isEmpty());
    }
}
