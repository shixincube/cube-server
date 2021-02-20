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

package cube.console.storage;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.console.mgmt.UserToken;
import cube.console.mgmt.User;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.storage.StorageFields;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 用户信息存储。
 */
public class UserStorage extends ConsoleStorage {

    private final StorageField[] userFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG),
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("password", LiteralBase.STRING)
    };

    private final StorageField[] tokenFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG),
            new StorageField("user_id", LiteralBase.LONG),
            new StorageField("token", LiteralBase.STRING),
            new StorageField("creation", LiteralBase.LONG),
            new StorageField("expire", LiteralBase.LONG)
    };

    private final String userTable = "user";

    private final String tokenTable = "user_token";

    public UserStorage(Properties properties) {
        super("UserStorage", properties);
    }

    public void open() {
        this.storage.open();
        this.autoCheckTable();
    }

    public void close() {
        this.storage.close();
    }

    public User readUser(String name) {
        List<StorageField[]> result = this.storage.executeQuery(this.userTable, this.userFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("name", LiteralBase.STRING, name))
        });

        if (result.isEmpty()) {
            return null;
        }

        StorageField[] data = result.get(0);
        Map<String, StorageField> map = StorageFields.get(data);
        return new User(map.get("id").getLong(), map.get("name").getString(), map.get("password").getString());
    }

    public User readUser(long id) {
        List<StorageField[]> result = this.storage.executeQuery(this.userTable, this.userFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, id))
        });

        if (result.isEmpty()) {
            return null;
        }

        StorageField[] data = result.get(0);
        Map<String, StorageField> map = StorageFields.get(data);
        return new User(map.get("id").getLong(), map.get("name").getString(), map.get("password").getString());
    }

    public void writeToken(UserToken token) {
        StorageField[] fields = new StorageField[] {
                new StorageField("user_id", LiteralBase.LONG, token.user.id),
                new StorageField("token", LiteralBase.STRING, token.token),
                new StorageField("creation", LiteralBase.LONG, token.creation),
                new StorageField("expire", LiteralBase.LONG, token.expire)
        };

        this.storage.executeInsert(this.tokenTable, fields);
    }

    public UserToken readToken(String tokenString) {
        List<StorageField[]> result = this.storage.executeQuery(this.tokenTable, this.tokenFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("token", LiteralBase.STRING, tokenString))
        });

        if (result.isEmpty()) {
            return null;
        }

        StorageField[] data = result.get(0);
        Map<String, StorageField> map = StorageFields.get(data);
        UserToken userToken = new UserToken(map.get("token").getString(),
                map.get("creation").getLong(), map.get("expire").getLong());

        User user = this.readUser(map.get("user_id").getLong());
        userToken.user = user;
        return userToken;
    }

    private void autoCheckTable() {
        if (!this.storage.exist(this.userTable)) {
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.UNIQUE, Constraint.NOT_NULL
                    }),
                    new StorageField("name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("password", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };

            this.storage.executeCreate(this.userTable, fields);

            // 插入默认用户
            // 密码：shixincube
            fields = new StorageField[] {
                    new StorageField("id", LiteralBase.LONG, Utils.generateSerialNumber()),
                    new StorageField("name", LiteralBase.STRING, "cube"),
                    new StorageField("password", LiteralBase.STRING, "c7af98d321febe62e04d45e8806852e0")
            };
            this.storage.executeInsert(this.userTable, fields);

            Logger.i(this.getClass(), "Create table '" + this.userTable + "'");
        }

        if (!this.storage.exist(this.tokenTable)) {
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("user_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("token", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("creation", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("expire", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };

            this.storage.executeCreate(this.tokenTable, fields);

            Logger.i(this.getClass(), "Create table '" + this.tokenTable + "'");
        }
    }
}
