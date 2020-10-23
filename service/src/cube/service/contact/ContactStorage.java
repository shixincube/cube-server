/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.service.contact;

import cell.core.talk.LiteralBase;
import cell.util.json.JSONObject;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.core.AbstractStorage;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import cube.util.SQLUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 联系人存储。
 */
public class ContactStorage {

    private final String version = "1.0";

    private final String groupTablePrefix = "group_";

    private final String groupMemberTablePrefix = "group_member_";

    /**
     * 群组字段描述。
     */
    private final StorageField[] groupFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("name", LiteralBase.STRING),
            new StorageField("timestamp", LiteralBase.LONG),
            new StorageField("owner", LiteralBase.LONG),
            new StorageField("context", LiteralBase.STRING),
            new StorageField("reserved", LiteralBase.STRING)
    };

    private final StorageField[] groupMembersFields = new StorageField[] {
            new StorageField("group", LiteralBase.LONG),
            new StorageField("contactId", LiteralBase.LONG),
            new StorageField("contactName", LiteralBase.STRING),
            new StorageField("timestamp", LiteralBase.LONG),
            new StorageField("reserved", LiteralBase.STRING)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> tableNameMap;

    public ContactStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.tableNameMap = new HashMap<>();
    }

    public ContactStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "ContactStorage", config);
        this.tableNameMap = new HashMap<>();
    }

    public void open() {
        this.storage.open();
    }

    public void close() {
        this.storage.close();
    }

    public void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            this.checkGroupTable(domain);
        }
    }

    public void writeContact(Contact contact) {

    }

    public Contact readContact(String domain, Long id) {
        return null;
    }

    public void writeGroup(Group group) {

    }

    public Group readGroup(String domain, Long groupId) {
        return null;
    }

    public List<Contact> readGroupMember(String domain, Long groupId) {
        return null;
    }

    private void checkGroupTable(String domain) {
        String table = this.groupTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.tableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[]{
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.UNIQUE
                    }),
                    new StorageField("name", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };
        }
    }
}
