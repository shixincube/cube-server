/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.account;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
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
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("account", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("phone", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
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
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("department", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("registration", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] tokenFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("account_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("token", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("creation", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("expire", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] preferenceFields = new StorageField[] {
            // 账号 ID
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY
            }),
            // 个人存储空间大小
            new StorageField("storage_size", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 空间大小生效日期
            new StorageField("storage_size_effective", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 空间大小到期日期
            new StorageField("storage_size_expiry", LiteralBase.LONG, new Constraint[] {
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
                dataMap.get("domain").getString(),
                dataMap.get("token").getString(), dataMap.get("device").getString(),
                dataMap.get("creation").getLong(), dataMap.get("expire").getLong());
        return token;
    }

    public Token writeToken(Token token) {
        if (token.id < 0) {
            this.storage.executeInsert(TABLE_TOKEN, new StorageField[] {
                    new StorageField("account_id", LiteralBase.LONG, token.accountId),
                    new StorageField("domain", LiteralBase.STRING, token.domain),
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
        String sql = "SELECT * FROM " + TABLE_ACCOUNT + " WHERE `id`=" + accountId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Account account = new Account(dataMap.get("id").getLong(), dataMap.get("domain").getString(),
                dataMap.get("account").getString(), dataMap.get("phone").getString(),
                dataMap.get("password").getString(), dataMap.get("name").getString(),
                dataMap.get("avatar").getString(), dataMap.get("state").getInt());
        account.registration = dataMap.get("registration").getLong();
        account.region = dataMap.get("region").getString();
        account.department = dataMap.get("department").getString();
        return account;
    }

    public Account readAccount(String accountName, String password) {
        String sql = "SELECT * FROM " + TABLE_ACCOUNT +
                " WHERE `account`='" + accountName + "' AND `password`='" + password.toLowerCase() + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Account account = new Account(dataMap.get("id").getLong(), dataMap.get("domain").getString(),
                dataMap.get("account").getString(), dataMap.get("phone").getString(),
                dataMap.get("password").getString(), dataMap.get("name").getString(),
                dataMap.get("avatar").getString(), dataMap.get("state").getInt());
        account.registration = dataMap.get("registration").getLong();
        account.region = dataMap.get("region").getString();
        account.department = dataMap.get("department").getString();
        return account;
    }

    public Account readAccountByPhoneNumber(String phoneNumber) {
        String sql = "SELECT * FROM " + TABLE_ACCOUNT + " WHERE `phone`='" + phoneNumber + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Account account = new Account(dataMap.get("id").getLong(), dataMap.get("domain").getString(),
                dataMap.get("account").getString(), dataMap.get("phone").getString(),
                dataMap.get("password").getString(), dataMap.get("name").getString(),
                dataMap.get("avatar").getString(), dataMap.get("state").getInt());
        account.registration = dataMap.get("registration").getLong();
        account.region = dataMap.get("region").getString();
        account.department = dataMap.get("department").getString();
        return account;
    }

    public Account readAccountByAccountName(String accountName) {
        String sql = "SELECT * FROM " + TABLE_ACCOUNT + " WHERE `account`='" + accountName + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> dataMap = StorageFields.get(result.get(0));
        Account account = new Account(dataMap.get("id").getLong(), dataMap.get("domain").getString(),
                dataMap.get("account").getString(), dataMap.get("phone").getString(),
                dataMap.get("password").getString(), dataMap.get("name").getString(),
                dataMap.get("avatar").getString(), dataMap.get("state").getInt());
        account.registration = dataMap.get("registration").getLong();
        account.region = dataMap.get("region").getString();
        account.department = dataMap.get("department").getString();
        return account;
    }

    public Account writeAccountWithAccountName(long accountId, String domain, String accountName, String password,
                                               String nickname, String avatar) {
        if (this.existsAccountName(accountName)) {
            // 账号名重复
            return null;
        }

        long registration = System.currentTimeMillis();
        String name = (null != nickname) ? nickname : "Cube-" + Utils.randomString(10).toLowerCase();

        boolean result = this.storage.executeInsert(TABLE_ACCOUNT, new StorageField[]{
                new StorageField("id", LiteralBase.LONG, accountId),
                new StorageField("domain", LiteralBase.STRING, domain),
                new StorageField("account", LiteralBase.STRING, accountName),
                new StorageField("password", LiteralBase.STRING, password.toLowerCase()),
                new StorageField("name", LiteralBase.STRING, name),
                new StorageField("avatar", LiteralBase.STRING, avatar),
                new StorageField("registration", LiteralBase.LONG, registration)
        });

        if (!result) {
            return null;
        }

        Account account = new Account(accountId, domain, accountName, "", password, nickname, avatar, 0);
        account.registration = registration;
        account.region = "--";
        account.department = "--";
        return account;
    }

    public Account writeAccountWithPhoneNumber(long accountId, String domain, String phoneNumber, String password,
                                               String nickname, String avatar) {
        if (this.existsPhoneNumber(phoneNumber)) {
            // 手机号码重复
            return null;
        }

        long registration = System.currentTimeMillis();
        String name = (null != nickname) ? nickname : "Cube-" + Utils.randomString(10).toLowerCase();

        boolean result = this.storage.executeInsert(TABLE_ACCOUNT, new StorageField[] {
                new StorageField("id", LiteralBase.LONG, accountId),
                new StorageField("domain", LiteralBase.STRING, domain),
                new StorageField("account", LiteralBase.STRING, phoneNumber),
                new StorageField("phone", LiteralBase.STRING, phoneNumber),
                new StorageField("password", LiteralBase.STRING, password.toLowerCase()),
                new StorageField("name", LiteralBase.STRING, name),
                new StorageField("avatar", LiteralBase.STRING, avatar),
                new StorageField("registration", LiteralBase.LONG, registration)
        });

        if (!result) {
            return null;
        }

        Account account = new Account(accountId, domain, phoneNumber, phoneNumber, password, nickname, avatar, 0);
        account.registration = registration;
        account.region = "--";
        account.department = "--";
        return account;
    }

    /**
     * 写入账号数据。
     *
     * @param account
     * @return
     */
    public Account writeAccount(Account account) {
        if (this.existsAccount(account.id, account.account, account.phone)) {
            return null;
        }

        boolean result = this.storage.executeInsert(TABLE_ACCOUNT, new StorageField[] {
                new StorageField("id", account.id),
                new StorageField("domain", account.domain),
                new StorageField("account", account.account),
                new StorageField("phone", account.phone),
                new StorageField("password", account.password.toLowerCase()),
                new StorageField("name", account.name),
                new StorageField("avatar", account.avatar),
                new StorageField("registration", account.registration),
                new StorageField("region", account.region),
                new StorageField("department", account.department)
        });
        return result ? account : null;
    }

    public void updateAccount(long accountId, String nickname, String avatar) {
        this.storage.executeUpdate(TABLE_ACCOUNT, new StorageField[] {
                new StorageField("name", LiteralBase.STRING, nickname),
                new StorageField("avatar", LiteralBase.STRING, avatar)
        }, new Conditional[] {
                Conditional.createEqualTo("id", LiteralBase.LONG, accountId)
        });
    }

    public boolean existsAccount(long accountId, String accountName, String phoneNumber) {
        List<StorageField[]> result = this.storage.executeQuery(TABLE_ACCOUNT, new StorageField[] {
                new StorageField("state", LiteralBase.INT)
        }, new Conditional[] {
                Conditional.createEqualTo("id", accountId),
                Conditional.createOr(),
                Conditional.createEqualTo("account", accountName),
                Conditional.createOr(),
                Conditional.createEqualTo("phone", phoneNumber),
        });
        return (!result.isEmpty());
    }

    public boolean existsAccountId(Long accountId) {
        String sql = "SELECT `state` FROM " + TABLE_ACCOUNT + " WHERE `id`=" + accountId;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return (!result.isEmpty());
    }

    public boolean existsAccountName(String accountName) {
        String sql = "SELECT `state` FROM " + TABLE_ACCOUNT + " WHERE `account`=" + accountName;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return (!result.isEmpty());
    }

    public boolean existsPhoneNumber(String phoneNumber) {
        String sql = "SELECT `state` FROM " + TABLE_ACCOUNT + " WHERE `phone`=" + phoneNumber;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        return (!result.isEmpty());
    }
}
