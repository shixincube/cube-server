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

package cube.ferryhouse;

import cell.core.talk.LiteralBase;
import cell.util.Cryptology;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.common.entity.Message;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.ferry.DomainMember;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.FileType;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息摆渡机。
 */
public class FerryStorage implements Storagable {

    /**
     * 配置数据字段。
     */
    private final StorageField[] propertyFields = new StorageField[] {
            new StorageField("sn", LiteralBase.INT, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("item", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("value", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            })
    };

    /**
     * 联系人数据字段。
     */
    private final StorageField[] contactFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY,
            }),
            new StorageField("name", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("data", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            })
    };

    /**
     * 域成员字段。
     */
    private final StorageField[] domainMemberFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 域
            new StorageField("domain", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 联系人 ID
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 加入方式
            new StorageField("way", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 时间戳
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 角色
            new StorageField("role", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 状态
            new StorageField("state", LiteralBase.INT, new Constraint[]{
                    Constraint.DEFAULT_0
            })
    };

    /**
     * 消息字段。
     */
    private final StorageField[] messageFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("from", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("to", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("source", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("owner", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("lts", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("rts", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("scope", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            }),
            new StorageField("device", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("payload", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("attachment", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] fileLabelFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("file_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("owner_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("file_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("file_size", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("last_modified", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("completed_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("expiry_time", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("file_type", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("md5", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("sha1", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("file_url", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("file_secure_url", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("direct_url", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final String propertyTable = "property";

    private final String contactTable = "contact";

    private final String domainMemberTable = "domain_member";

    private final String messageTable = "message";

    private final String fileLabelTable = "file_label";

    private Storage storage;

    private String domainName;

    private final byte[] key = new byte[8];

    public FerryStorage(String domainName, JSONObject config) {
        this.domainName = domainName;
        this.storage = StorageFactory.getInstance().createStorage(StorageType.MySQL,
                "FerryStorage", config);

        byte[] md5 = Cryptology.getInstance().hashWithMD5(this.domainName.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < this.key.length; ++i) {
            this.key[i] = (byte)(md5[i] + md5[i + 8]);
        }
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
        checkPropertyTable();
        checkContactTable();
        checkDomainMemberTable();
        checkMessageTable();
        checkFileLabelTable();
    }

    public void writeLicence(JSONObject licenceJson) {
        HashMap<String, String> data = new HashMap<>();
        data.put("domain", licenceJson.getString("domain"));
        data.put("beginning", Long.toString(licenceJson.getLong("beginning")));
        data.put("duration", Long.toString(licenceJson.getLong("duration")));
        data.put("limit", Integer.toString(licenceJson.getInt("limit")));

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String item = entry.getKey();
            String value = entry.getValue();

            if (!this.storage.executeUpdate(this.propertyTable, new StorageField[] {
                    new StorageField("value", value)
                }, new Conditional[] {
                    Conditional.createEqualTo("item", item)
                })) {
                // 插入数据
                this.storage.executeInsert(this.propertyTable, new StorageField[] {
                        new StorageField("item", item),
                        new StorageField("value", value)
                });
            }
        }
    }

    /**
     * 读取指定属性值。
     *
     * @param name
     * @return
     */
    public String readProperty(String name) {
        synchronized (this) {
            List<StorageField[]> result = this.storage.executeQuery(this.propertyTable, new StorageField[] {
                    new StorageField("value", LiteralBase.STRING)
            }, new Conditional[] {
                    Conditional.createEqualTo("item", name)
            });

            if (result.isEmpty()) {
                return null;
            }

            return result.get(0)[0].getString();
        }
    }

    /**
     * 写入指定属性值。
     *
     * @param name
     * @param value
     */
    public void writeProperty(String name, String value) {
        synchronized (this) {
            if (!this.storage.executeUpdate(this.propertyTable, new StorageField[] {
                    new StorageField("value", value)
            }, new Conditional[] {
                    Conditional.createEqualTo("item", name)
            })) {
                // 插入数据
                this.storage.executeInsert(this.propertyTable, new StorageField[] {
                        new StorageField("item", name),
                        new StorageField("value", value)
                });
            }
        }
    }

    public synchronized void writeContact(Contact contact) {
        List<StorageField[]> result = this.storage.executeQuery(this.contactTable, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("id", contact.getId().longValue())
        });

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.contactTable, new StorageField[] {
                    new StorageField("id", contact.getId().longValue()),
                    new StorageField("name", contact.getName()),
                    new StorageField("data", contact.toJSON().toString())
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.contactTable, new StorageField[] {
                    new StorageField("name", contact.getName()),
                    new StorageField("data", contact.toJSON().toString())
            }, new Conditional[] {
                    Conditional.createEqualTo("id", contact.getId().longValue())
            });
        }
    }

    public boolean existsContact(Long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.contactTable, new StorageField[] {
                new StorageField("id", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("id", contactId.longValue())
        });

        return !result.isEmpty();
    }

    public synchronized void writeDomainMember(DomainMember member) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainMemberTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_id", member.getContactId().longValue())
        });

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.domainMemberTable, new StorageField[] {
                    new StorageField("domain", member.getDomain().getName()),
                    new StorageField("contact_id", member.getContactId().longValue()),
                    new StorageField("way", member.getJoinWay().code),
                    new StorageField("timestamp", member.getTimestamp()),
                    new StorageField("role", member.getRole().code),
                    new StorageField("state", member.getState())
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.domainMemberTable, new StorageField[] {
                    new StorageField("domain", member.getDomain().getName()),
                    new StorageField("contact_id", member.getContactId().longValue()),
                    new StorageField("way", member.getJoinWay().code),
                    new StorageField("timestamp", member.getTimestamp()),
                    new StorageField("role", member.getRole().code),
                    new StorageField("state", member.getState())
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", result.get(0)[0].getLong())
            });
        }
    }

    public boolean existsDomainMember(Long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainMemberTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("contact_id", contactId.longValue())
        });

        return !result.isEmpty();
    }

    public synchronized void writeMessage(Message message) {
        List<StorageField[]> result = this.storage.executeQuery(this.messageTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("id", message.getId().longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", message.getOwner().longValue())
        });

        String payload = message.getPayload().toString();
        byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(payload.getBytes(StandardCharsets.UTF_8), this.key);
        String ciphertextString = Cryptology.getInstance().encodeBase64(ciphertext);

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.messageTable, new StorageField[] {
                    new StorageField("id", message.getId().longValue()),
                    new StorageField("from", message.getFrom().longValue()),
                    new StorageField("to", message.getTo().longValue()),
                    new StorageField("source", message.getSource().longValue()),
                    new StorageField("owner", message.getOwner().longValue()),
                    new StorageField("lts", message.getLocalTimestamp()),
                    new StorageField("rts", message.getRemoteTimestamp()),
                    new StorageField("state", message.getState().code),
                    new StorageField("scope", message.getScope()),
                    new StorageField("device", message.getSourceDevice().toJSON().toString()),
                    new StorageField("payload", ciphertextString),
                    new StorageField("attachment",
                            (null != message.getAttachment()) ? message.getAttachment().toJSON().toString() : null)
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.messageTable, new StorageField[] {
                    new StorageField("from", message.getFrom().longValue()),
                    new StorageField("to", message.getTo().longValue()),
                    new StorageField("source", message.getSource().longValue()),
                    new StorageField("owner", message.getOwner().longValue()),
                    new StorageField("lts", message.getLocalTimestamp()),
                    new StorageField("rts", message.getRemoteTimestamp()),
                    new StorageField("state", message.getState().code),
                    new StorageField("scope", message.getScope()),
                    new StorageField("device", message.getSourceDevice().toJSON().toString()),
                    new StorageField("payload", ciphertextString),
                    new StorageField("attachment",
                            (null != message.getAttachment()) ? message.getAttachment().toJSON().toString() : null)
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", result.get(0)[0].getLong())
            });
        }
    }

    public synchronized void updateMessageState(Message message) {
        this.storage.executeUpdate(this.messageTable, new StorageField[] {
                new StorageField("state", message.getState().code)
        }, new Conditional[] {
                Conditional.createEqualTo("id", message.getId().longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", message.getOwner().longValue())
        });
    }

    public synchronized void updateMessagePayload(Message message) {
        String payload = message.getPayload().toString();
        byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(payload.getBytes(StandardCharsets.UTF_8), this.key);
        String ciphertextString = Cryptology.getInstance().encodeBase64(ciphertext);

        this.storage.executeUpdate(this.messageTable, new StorageField[] {
                new StorageField("state", message.getState().code),
                new StorageField("payload", ciphertextString)
        }, new Conditional[] {
                Conditional.createEqualTo("id", message.getId().longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", message.getOwner().longValue())
        });
    }

    public synchronized void deleteMessage(Message message) {
        this.storage.executeDelete(this.messageTable, new Conditional[] {
                Conditional.createEqualTo("id", message.getId().longValue()),
                Conditional.createAnd(),
                Conditional.createEqualTo("owner", message.getOwner().longValue())
        });
    }

    /**
     * 删除指定时间戳之前的消息。
     *
     * @param timestamp
     */
    public void deleteAllMessages(long timestamp) {
        this.storage.executeDelete(this.messageTable, new Conditional[]{
                Conditional.createLessThan(new StorageField("rts", LiteralBase.LONG, timestamp))
        });
    }

    /**
     * 写入文件标签数据。
     *
     * @param fileLabel
     */
    public void writeFileLabel(FileLabel fileLabel) {
        List<StorageField[]> result = this.storage.executeQuery(this.fileLabelTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("file_code", fileLabel.getFileCode())
        });

        StorageField[] fields = new StorageField[] {
                new StorageField("id", fileLabel.getId().longValue()),
                new StorageField("file_code", fileLabel.getFileCode()),
                new StorageField("owner_id", fileLabel.getOwnerId().longValue()),
                new StorageField("file_name", fileLabel.getFileName()),
                new StorageField("file_size", fileLabel.getFileSize()),
                new StorageField("last_modified", fileLabel.getLastModified()),
                new StorageField("completed_time", fileLabel.getCompletedTime()),
                new StorageField("expiry_time", fileLabel.getExpiryTime()),
                new StorageField("file_type", fileLabel.getFileType().getPreferredExtension()),
                new StorageField("md5", fileLabel.getMD5Code()),
                new StorageField("sha1", fileLabel.getSHA1Code()),
                new StorageField("file_url", fileLabel.getFileURL()),
                new StorageField("file_secure_url", fileLabel.getFileSecureURL()),
                new StorageField("direct_url", fileLabel.getDirectURL())
        };

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.fileLabelTable, fields);
        }
        else {
            // 更新
            this.storage.executeUpdate(this.fileLabelTable, fields, new Conditional[] {
                    Conditional.createEqualTo("sn", result.get(0)[0].getLong())
            });
        }
    }

    /**
     * 读取文件标签。
     *
     * @param fileCode
     * @return
     */
    public FileLabel readFileLabel(String fileCode) {
        List<StorageField[]> result = this.storage.executeQuery(this.fileLabelTable, this.fileLabelFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("file_code", fileCode))
        });

        if (!result.isEmpty()) {
            StorageField[] fields = result.get(0);

            // 将字段转为映射关系，便于代码阅读
            Map<String, StorageField> map = StorageFields.get(fields);

            FileLabel label = new FileLabel(map.get("id").getLong(), this.domainName, map.get("file_code").getString(),
                    map.get("owner_id").getLong(), map.get("file_name").getString(), map.get("file_size").getLong(),
                    map.get("last_modified").getLong(), map.get("completed_time").getLong(), map.get("expiry_time").getLong());

            label.setFileType(FileType.matchExtension(map.get("file_type").getString()));

            if (!map.get("md5").isNullValue()) {
                label.setMD5Code(map.get("md5").getString());
            }

            if (!map.get("sha1").isNullValue()) {
                label.setSHA1Code(map.get("sha1").getString());
            }

            label.setFileURLs(map.get("file_url").getString(),
                    (map.get("file_secure_url").isNullValue()) ? null : map.get("file_secure_url").getString());

            label.setDirectURL(map.get("direct_url").isNullValue() ? null : map.get("direct_url").getString());

            return label;
        }

        return null;
    }

    /**
     * 获取所有文件标签。
     *
     * @return
     */
    public List<FileLabel> getAllFileLabels() {
        List<FileLabel> fileLabels = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.fileLabelTable, this.fileLabelFields);
        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            FileLabel label = new FileLabel(map.get("id").getLong(), this.domainName, map.get("file_code").getString(),
                    map.get("owner_id").getLong(), map.get("file_name").getString(), map.get("file_size").getLong(),
                    map.get("last_modified").getLong(), map.get("completed_time").getLong(), map.get("expiry_time").getLong());

            label.setFileType(FileType.matchExtension(map.get("file_type").getString()));

            if (!map.get("md5").isNullValue()) {
                label.setMD5Code(map.get("md5").getString());
            }

            if (!map.get("sha1").isNullValue()) {
                label.setSHA1Code(map.get("sha1").getString());
            }

            label.setFileURLs(map.get("file_url").getString(),
                    (map.get("file_secure_url").isNullValue()) ? null : map.get("file_secure_url").getString());

            label.setDirectURL(map.get("direct_url").isNullValue() ? null : map.get("direct_url").getString());

            fileLabels.add(label);
        }

        return fileLabels;
    }

    private void checkPropertyTable() {
        if (!this.storage.exist(this.propertyTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.propertyTable, this.propertyFields)) {
                Logger.i(this.getClass(), "Created table '" + this.propertyTable + "' successfully");
            }
        }
    }

    private void checkContactTable() {
        if (!this.storage.exist(this.contactTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.contactTable, this.contactFields)) {
                Logger.i(this.getClass(), "Created table '" + this.contactTable + "' successfully");
            }
        }
    }

    private void checkDomainMemberTable() {
        if (!this.storage.exist(this.domainMemberTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.domainMemberTable, this.domainMemberFields)) {
                Logger.i(this.getClass(), "Created table '" + this.domainMemberTable + "' successfully");
            }
        }
    }

    private void checkMessageTable() {
        if (!this.storage.exist(this.messageTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.messageTable, this.messageFields)) {
                Logger.i(this.getClass(), "Created table '" + this.messageTable + "' successfully");
            }
        }
    }

    private void checkFileLabelTable() {
        if (!this.storage.exist(this.fileLabelTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.fileLabelTable, this.fileLabelFields)) {
                Logger.i(this.getClass(), "Created table '" + this.fileLabelTable + "' successfully");
            }
        }
    }
}
