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

package cube.service.hub;

import cell.core.talk.LiteralBase;
import cell.util.Base64;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.entity.*;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.hub.Product;
import cube.hub.data.ChannelCode;
import cube.hub.data.wechat.PlainMessage;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 授权管理器。
 */
public class ChannelManager {

    private final StorageField[] channelCodeFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 授权码
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 创建时间戳
            new StorageField("creation", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 到期时间戳
            new StorageField("expiration", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 产品
            new StorageField("product", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 状态
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] allocatingFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 授权码
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 分配的 ID
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 所在的伪装者节点
            new StorageField("pretender_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 时间戳
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] accountFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 账号 ID
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 账号名
            new StorageField("account_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据来源绑定的通道码
            new StorageField("source_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 产品分类
            new StorageField("product", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据 JSON
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] groupFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 群组所属的账号 ID
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 群组名称
            new StorageField("group_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 更新时间戳
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 产品分类
            new StorageField("product", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据 JSON
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] groupMemberFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 群组表的记录 SN
            new StorageField("group_sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 账号 ID
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            // 账号名
            new StorageField("account_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据 JSON
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] partnerMessageFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 授权码
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息的来源账号
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息对应的好友/伙伴
            new StorageField("partner_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 发件人 ID
            new StorageField("sender_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            // 消息时间戳
            new StorageField("date", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息时间精度
            new StorageField("date_precision", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息实体的 JSON 数据
            new StorageField("message", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] groupMessageFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 授权码
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息的来源账号
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息对应的群组名
            new StorageField("group_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 发件人 ID
            new StorageField("sender_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            // 发件人名称
            new StorageField("sender_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息时间戳
            new StorageField("date", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息时间精度
            new StorageField("date_precision", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 消息实体的 JSON 数据
            new StorageField("message", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final StorageField[] contactBookFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 对应的联系人 ID
            new StorageField("account_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 联系人 ID
            new StorageField("contact_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 联系人名称
            new StorageField("contact_name", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据更新时间戳
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 产品分类
            new StorageField("product", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据 JSON
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final String channelCodeTable = "hub_channel_code";

    private final String allocatingTable = "hub_allocating";

    private final String accountTable = "hub_account";

    private final String groupTable = "hub_group";

    private final String groupMemberTable = "hub_group_member";

    private final String partnerMessageTable = "hub_partner_message";

    private final String groupMessageTable = "hub_group_message";

    private final String contactBookTable = "hub_contact_book";

    private Storage storage;

    private ExecutorService executor;

    /**
     * 消息数量上限，保存的消息主要用于进行校验，数据库不对消息进行持久化存储。
     */
    private int partnerMessageLimit = 1000;

    /**
     * 消息数量上限，保存的消息主要用于进行校验，数据库不对消息进行持久化存储。
     */
    private int groupMessageLimit = 2000;

    /**
     * 用于临时存储通道码。
     */
    private Map<String, ChannelCode> channelCodeMap;

    public ChannelManager(JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(StorageType.MySQL,
                "HubService", config);
        this.channelCodeMap = new ConcurrentHashMap<>();
    }

    public void start(ExecutorService executor) {
        this.executor = executor;
        this.storage.open();
        this.execSelfChecking();
    }

    public void stop() {
        this.storage.close();
        this.channelCodeMap.clear();
    }

    /**
     * 获取指定的通道码。
     *
     * @param code
     * @return
     */
    public ChannelCode getChannelCode(String code) {
        if (null == code) {
            return null;
        }

        ChannelCode channelCode = this.channelCodeMap.get(code);
        if (null != channelCode) {
            return channelCode;
        }

        List<StorageField[]> result = this.storage.executeQuery(this.channelCodeTable,
                this.channelCodeFields, new Conditional[] {
                        Conditional.createEqualTo("code", code)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        channelCode = new ChannelCode(map.get("code").getString(), map.get("creation").getLong(),
                map.get("expiration").getLong(), Product.parse(map.get("product").getString()),
                map.get("state").getInt());
        // 加入到缓存
        this.channelCodeMap.put(code, channelCode);
        return channelCode;
    }

    public ChannelCode createChannelCode(Product product, long expiredDuration) {
        long now = System.currentTimeMillis();
        ChannelCode channelCode = new ChannelCode(Utils.randomString(32), now,
                now + expiredDuration, product, ChannelCode.ENABLED);
        this.storage.executeInsert(this.channelCodeTable, new StorageField[] {
                new StorageField("code", channelCode.code),
                new StorageField("creation", channelCode.creation),
                new StorageField("expiration", channelCode.expiration),
                new StorageField("product", channelCode.product.name),
                new StorageField("state", channelCode.state),
        });
        return channelCode;
    }

    /**
     * 获取通道码对应的账号 ID 。
     *
     * @param channelCode
     * @return
     */
    public String getAccountId(String channelCode) {
        List<StorageField[]> result = this.storage.executeQuery(this.allocatingTable,
                new StorageField[] {
                        new StorageField("account_id", LiteralBase.STRING)
                }, new Conditional[] {
                        Conditional.createEqualTo("code", channelCode)
                });
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0)[0].getString();
    }

    /**
     * 获取通道登录账号所在的伪装者 ID 。
     *
     * @param channelCode
     * @return
     */
    public Long getPretenderId(String channelCode) {
        List<StorageField[]> result = this.storage.executeQuery(this.allocatingTable,
                new StorageField[] {
                        new StorageField("pretender_id", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("code", channelCode)
                });
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0)[0].getLong();
    }

    /**
     * 获取账号 ID 对应的通道。
     *
     * @param accountId
     * @return
     */
    public String getChannelCodeWithAccountId(String accountId) {
        List<StorageField[]> result = this.storage.executeQuery(this.allocatingTable,
                new StorageField[] {
                        new StorageField("code", LiteralBase.STRING)
                }, new Conditional[] {
                        Conditional.createEqualTo("account_id", accountId)
                });
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0)[0].getString();
    }

    /**
     * 设置通道码对应的账号 ID 。
     *
     * @param channelCode
     * @param accountId
     * @param pretenderId
     * @return 如果返回 {@code false} 则表示设置失败。
     */
    public boolean allocAccountId(String channelCode, String accountId, Long pretenderId) {
        List<StorageField[]> result = this.storage.executeQuery(this.allocatingTable, this.allocatingFields,
                new Conditional[] {
                        Conditional.createEqualTo("code", channelCode)
                });
        if (!result.isEmpty()) {
            Map<String, StorageField> map = StorageFields.get(result.get(0));
            return map.get("account_id").getString().equals(accountId);
        }

        return this.storage.executeInsert(this.allocatingTable, new StorageField[] {
                new StorageField("code", channelCode),
                new StorageField("account_id", accountId),
                new StorageField("pretender_id", pretenderId.longValue()),
                new StorageField("timestamp", System.currentTimeMillis())
        });
    }

    /**
     * 删除通道码对应的账号。
     *
     * @param channelCode
     */
    public void freeAccountId(String channelCode) {
        this.storage.executeDelete(this.allocatingTable, new Conditional[] {
                Conditional.createEqualTo("code", channelCode)
        });
    }

    /**
     * 查询账号。
     *
     * @param accountId
     * @param product
     * @return
     */
    public Contact queryAccount(String accountId, Product product) {
        List<StorageField[]> result = this.storage.executeQuery(this.accountTable,
                new StorageField[] {
                        new StorageField("data", LiteralBase.STRING)
                }, new Conditional[] {
                        Conditional.createEqualTo("account_id", accountId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("product", product.name)
                });

        if (result.isEmpty()) {
            return null;
        }

        String data = result.get(0)[0].getString();
        JSONObject json = new JSONObject(data);
        return new Contact(json);
    }

    /**
     * 更新指定的账号信息。
     *
     * @param channelCode
     * @param account
     * @param product
     */
    public synchronized void updateAccount(String channelCode, Contact account, Product product) {
        String nameBase64 = Base64.encodeBytes(account.getName().getBytes(StandardCharsets.UTF_8));

        List<StorageField[]> result = this.storage.executeQuery(this.accountTable, new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("account_id", account.getExternalId()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("product", product.name)
                });

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.accountTable, new StorageField[] {
                    new StorageField("account_id", account.getExternalId()),
                    new StorageField("account_name", nameBase64),
                    new StorageField("source_code", channelCode),
                    new StorageField("product", product.name),
                    new StorageField("data", account.toJSON().toString())
            });
        }
        else {
            // 更新
            long sn = result.get(0)[0].getLong();
            this.storage.executeUpdate(this.accountTable, new StorageField[] {
                    new StorageField("account_id", account.getExternalId()),
                    new StorageField("account_name", nameBase64),
                    new StorageField("source_code", channelCode),
                    new StorageField("product", product.name),
                    new StorageField("data", account.toJSON().toString())
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", sn)
            });
        }
    }

    /**
     * 查询通讯录。
     *
     * @param accountId
     * @param product
     * @return
     */
    public List<Contact> queryContactBook(String accountId, Product product) {
        List<Contact> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.contactBookTable,
                new StorageField[] {
                        new StorageField("data", LiteralBase.STRING)
                }, new Conditional[] {
                        Conditional.createEqualTo("account_id", accountId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("product", product.name)
                });
        if (result.isEmpty()) {
            return list;
        }

        for (StorageField[] data : result) {
            String jsonString = data[0].getString();
            list.add(new Contact(new JSONObject(jsonString)));
        }

        return list;
    }

    /**
     * 更新通讯录数据。
     *
     * @param accountId
     * @param product
     * @param contact
     */
    public synchronized void updateContactBook(String accountId, Product product,
                                               Contact contact, boolean forceUpdate) {
        List<StorageField[]> result = this.storage.executeQuery(this.contactBookTable,
                new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG),
                        new StorageField("timestamp", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("account_id", accountId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contact.getExternalId()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("product", product.name)
                });

        if (result.isEmpty()) {
            // 插入
            String originalName = contact.getName();
            String contactName = Base64.encodeBytes(originalName.getBytes(StandardCharsets.UTF_8));
            contact.setName(contactName);
            this.storage.executeInsert(this.contactBookTable, new StorageField[] {
                    new StorageField("account_id", accountId),
                    new StorageField("contact_id", contact.getExternalId()),
                    new StorageField("contact_name", contactName),
                    new StorageField("timestamp", System.currentTimeMillis()),
                    new StorageField("product", product.name),
                    new StorageField("data", contact.toJSON().toString())
            });
            contact.setName(originalName);
        }
        else {
            // 更新
            long sn = result.get(0)[0].getLong();
            long timestamp = result.get(0)[1].getLong();
            if (!forceUpdate) {
                if (System.currentTimeMillis() - timestamp < 2L * 24 * 60 * 60 * 1000) {
                    // 更新间隔小于2天
                    return;
                }
            }

            String originalName = contact.getName();
            String contactName = Base64.encodeBytes(originalName.getBytes(StandardCharsets.UTF_8));
            contact.setName(contactName);

            this.storage.executeUpdate(this.contactBookTable, new StorageField[] {
                    new StorageField("contact_name", contactName),
                    new StorageField("timestamp", System.currentTimeMillis()),
                    new StorageField("data", contact.toJSON().toString())
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", sn)
            });

            contact.setName(originalName);
        }
    }

    /**
     * 查询通讯录里的联系人。
     *
     * @param account
     * @param contactId
     * @param product
     * @return
     */
    public Contact queryPartnerFromBook(Contact account, String contactId, Product product) {
        List<StorageField[]> result = this.storage.executeQuery(this.contactBookTable,
                new StorageField[] {
                        new StorageField("data", LiteralBase.STRING)
                }, new Conditional[] {
                        Conditional.createEqualTo("account_id", account.getExternalId()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("product", product.name)
                });

        if (result.isEmpty()) {
            return null;
        }

        Contact contact = new Contact(new JSONObject(result.get(0)[0].getString()));

        try {
            byte[] bytes = Base64.decode(contact.getName());
            String name = new String(bytes, StandardCharsets.UTF_8);
            contact.setName(name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contact;
    }

    public Group queryGroup(Contact account, String groupName, Product product) {
        String accountId = account.getExternalId();
        List<StorageField[]> result = this.storage.executeQuery(this.groupTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG),
                new StorageField("data", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("account_id", accountId),
                Conditional.createAnd(),
                Conditional.createEqualTo("group_name", groupName),
                Conditional.createAnd(),
                Conditional.createEqualTo("product", product.name)
        });

        if (result.isEmpty()) {
            return null;
        }

        long groupSN = result.get(0)[0].getLong();
        String dataString = result.get(0)[1].getString();

        Group group = new Group(new JSONObject(dataString));

        result = this.storage.executeQuery(this.groupMemberTable, new StorageField[] {
                new StorageField("data", LiteralBase.STRING)
        }, new Conditional[] {
                Conditional.createEqualTo("group_sn", groupSN)
        });
        for (StorageField[] data : result) {
            String memberDataString = data[0].getString();
            Contact contact = new Contact(new JSONObject(memberDataString));
            group.addMember(contact);
        }

        return group;
    }

    public synchronized void updateGroup(Contact account, Group group, Product product) {
        String accountId = account.getExternalId();
        List<StorageField[]> result = this.storage.executeQuery(this.groupTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("account_id", accountId),
                Conditional.createAnd(),
                Conditional.createEqualTo("group_name", group.getName())
        });

        long groupSN = 0;

        if (result.isEmpty()) {
            groupSN = group.getId();
            // 插入
            this.storage.executeInsert(this.groupTable, new StorageField[] {
                    new StorageField("sn", groupSN),
                    new StorageField("account_id", accountId),
                    new StorageField("group_name", group.getName()),
                    new StorageField("timestamp", System.currentTimeMillis()),
                    new StorageField("product", product.name),
                    new StorageField("data", group.toCompactJSON().toString())
            });
        }
        else {
            groupSN = result.get(0)[0].getLong();
            // 更新
            this.storage.executeUpdate(this.groupTable, new StorageField[] {
                    new StorageField("timestamp", System.currentTimeMillis()),
                    new StorageField("data", group.toCompactJSON().toString())
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", groupSN)
            });

            List<Contact> contacts = group.getMemberList();
            if (null != contacts) {
                // 删除所有成员数据
                this.storage.executeDelete(this.groupMemberTable, new Conditional[] {
                        Conditional.createEqualTo("group_sn", groupSN)
                });
            }
        }

        List<Contact> contacts = group.getMemberList();
        if (null != contacts) {
            for (Contact contact : contacts) {
                String cid = contact.getExternalId();
                this.storage.executeInsert(this.groupMemberTable, new StorageField[] {
                        new StorageField("group_sn", groupSN),
                        new StorageField("account_id", (null != cid) ? cid : ""),
                        new StorageField("account_name", contact.getName()),
                        new StorageField("data", contact.toJSON().toString())
                });
            }
        }
    }

    /**
     * 获取指定账号 ID 对应的消息伙伴的 ID 列表。
     *
     * @param channelCode
     * @param accountId
     * @return
     */
    public List<String> getMessagePartnerIdList(String channelCode, String accountId) {
        String sql = "SELECT DISTINCT `partner_id` FROM `" + this.partnerMessageTable + "` WHERE `code`='" +
                channelCode + "' AND `account_id`='" + accountId + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (StorageField[] data : result) {
            list.add(data[0].getString());
        }

        return list;
    }

    /**
     * 获取指定账号 ID  对应的群组名列表。
     *
     * @param channelCode
     * @param accountId
     * @return
     */
    public List<String> getMessageGroupNameList(String channelCode, String accountId) {
        String sql = "SELECT DISTINCT `group_name` FROM `" + this.groupMessageTable + "` WHERE `code`='" +
                channelCode + "' AND `account_id`='" + accountId + "'";
        List<StorageField[]> result = this.storage.executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (StorageField[] data : result) {
            list.add(data[0].getString());
        }

        return list;
    }

    /**
     * 获取按照时间倒序排序的会话列表。
     * 返回的会话里没有消息数据。
     *
     * @param channelCode
     * @param accountId
     * @return
     */
    public List<Conversation> queryRecentConversations(ChannelCode channelCode, String accountId,
                                                     int numConversations, int numMessages) {
        List<Conversation> list = new ArrayList<>();

        String sql = "SELECT DISTINCT `partner_id` FROM `" + this.partnerMessageTable +
                "` WHERE `code`='" + channelCode.code +
                "' AND `account_id`='" + accountId + "'" +
                " LIMIT " + numConversations;
        List<StorageField[]> result = this.storage.executeQuery(sql);
        for (StorageField[] data : result) {
            String partnerId = data[0].getString();

            Contact partner = this.queryAccount(partnerId, channelCode.product);
            if (null == partner) {
                continue;
            }

            Conversation conversation = null;

            sql = "SELECT `date`,`message` FROM `" + this.partnerMessageTable +
                    "` WHERE `partner_id`='" + partnerId +
                    "' AND `account_id`='" + accountId + "'" +
                    " ORDER BY `date` DESC" +
                    " LIMIT " + numMessages;
            List<StorageField[]> messageResult = this.storage.executeQuery(sql);
            for (StorageField[] messageData : messageResult) {
                if (null == conversation) {
                    long timestamp = messageData[0].getLong();
                    conversation = new Conversation(0L, "", timestamp,
                            0L, ConversationType.Contact,
                            ConversationState.Normal, 0L, ConversationRemindType.Normal);
                    // 设置实体
                    conversation.setPivotalEntity(partner);
                }

                String messageString = messageData[1].getString();
                Message message = new Message(new JSONObject(messageString));
                conversation.addRecentMessage(message);

                if (conversation.getRecentMessages().size() > numMessages) {
                    break;
                }
            }

            // 添加
            list.add(conversation);
        }

        sql = "SELECT DISTINCT `group_name` FROM `" + this.groupMessageTable +
                "` WHERE `code`='" + channelCode.code +
                "' AND `account_id`='" + accountId + "'" +
                " LIMIT " + numConversations;
        result = this.storage.executeQuery(sql);
        for (StorageField[] data : result) {
            String groupName = data[0].getString();

            Group group = new Group();
            group.setName(groupName);

            Conversation conversation = null;

            sql = "SELECT `date`,`message` FROM `" + this.groupMessageTable +
                    "` WHERE `group_name`='" + groupName +
                    "' AND `account_id`='" + accountId + "'" +
                    " ORDER BY `date` DESC" +
                    " LIMIT " + numMessages;
            List<StorageField[]> messageResult = this.storage.executeQuery(sql);
            for (StorageField[] messageData : messageResult) {
                if (null == conversation) {
                    long timestamp = messageData[0].getLong();
                    conversation = new Conversation(0L, "", timestamp,
                            0L, ConversationType.Group,
                            ConversationState.Normal, 0L, ConversationRemindType.Normal);
                    // 设置实体
                    conversation.setPivotalEntity(group);
                }

                String messageString = messageData[1].getString();
                Message message = new Message(new JSONObject(messageString));
                conversation.addRecentMessage(message);

                if (conversation.getRecentMessages().size() > numMessages) {
                    break;
                }
            }

            // 添加
            list.add(conversation);
        }

        // 时间倒序
        list.sort(new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conv1, Conversation conv2) {
                return (int)(conv2.getTimestamp() - conv1.getTimestamp());
            }
        });

        return list;
    }

    /**
     * 获取指定伙伴的消息记录列表。
     *
     * @param channelCode
     * @param accountId
     * @param partnerId
     * @param limit
     * @return
     */
    public List<Message> getMessagesByPartner(String channelCode, String accountId, String partnerId,
                                              int limit) {
//        List<StorageField[]> result = this.storage.executeQuery(this.partnerMessageTable,
//            new StorageField[] {
//                    new StorageField("sn", LiteralBase.LONG),
//                    new StorageField("message", LiteralBase.STRING)
//            }, new Conditional[] {
//                    Conditional.createEqualTo("code", channelCode),
//                    Conditional.createAnd(),
//                    Conditional.createEqualTo("account_id", accountId),
//                    Conditional.createAnd(),
//                    Conditional.createEqualTo("partner_id", partnerId)
//            });

        String sql = "SELECT `message` FROM `" + this.partnerMessageTable + "` " +
                "WHERE `code`='" + channelCode +
                "' AND account_id='" + accountId +
                "' AND partner_id='" + partnerId + "'" +
                " ORDER BY `date` DESC" +
                " LIMIT " + limit;
        List<StorageField[]> result = this.storage.executeQuery(sql);

        if (result.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> list = new ArrayList<>(result.size());

        for (StorageField[] data : result) {
            String messageString = data[0].getString();
            JSONObject json = new JSONObject(messageString);
            Message message = new Message(json);
            list.add(message);
        }

        // 翻转列表，按照时间顺序输出
        Collections.reverse(list);

        return list;
    }

    /**
     * 获取指定群组的消息。
     *
     * @param channelCode
     * @param accountId
     * @param groupName
     * @param limit
     * @return
     */
    public List<Message> getMessagesByGroup(String channelCode, String accountId, String groupName,
                                            int limit) {
//        List<StorageField[]> result = this.storage.executeQuery(this.groupMessageTable,
//                new StorageField[] {
//                        new StorageField("sn", LiteralBase.LONG),
//                        new StorageField("message", LiteralBase.STRING)
//                }, new Conditional[] {
//                        Conditional.createEqualTo("code", channelCode),
//                        Conditional.createAnd(),
//                        Conditional.createEqualTo("account_id", accountId),
//                        Conditional.createAnd(),
//                        Conditional.createEqualTo("group_name", groupName)
//                });

        String sql = "SELECT `message` FROM `" + this.groupMessageTable + "` " +
                "WHERE `code`='" + channelCode +
                "' AND account_id='" + accountId +
                "' AND group_name='" + groupName + "'" +
                " ORDER BY `date` DESC" +
                " LIMIT " + limit;
        List<StorageField[]> result = this.storage.executeQuery(sql);

        if (result.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> list = new ArrayList<>(result.size());

        for (StorageField[] data : result) {
            String messageString = data[0].getString();
            JSONObject json = new JSONObject(messageString);
            Message message = new Message(json);
            list.add(message);
        }

        // 翻转列表，按照时间顺序输出
        Collections.reverse(list);

        return list;
    }

    /**
     * 追加消息记录。
     *
     * @param channelCode
     * @param accountId
     * @param partnerId
     * @param senderId
     * @param message
     * @return
     */
    public boolean appendMessageByPartner(String channelCode, String accountId,
                                          String partnerId, String senderId, Message message) {
        PlainMessage plainMessage = PlainMessage.create(message);

        return this.storage.executeInsert(this.partnerMessageTable, new StorageField[] {
                new StorageField("code", channelCode),
                new StorageField("account_id", accountId),
                new StorageField("partner_id", partnerId),
                new StorageField("sender_id", senderId),
                new StorageField("date", plainMessage.getDate()),
                new StorageField("date_precision", plainMessage.getDatePrecision()),
                new StorageField("message", message.toJSON().toString())
        });
    }

    /**
     * 追加消息记录。
     *
     * @param channelCode
     * @param accountId
     * @param groupName
     * @param senderName
     * @param message
     * @return
     */
    public boolean appendMessageByGroup(String channelCode, String accountId,
                                        String groupName, String senderName, Message message) {
        PlainMessage plainMessage = PlainMessage.create(message);

        return this.storage.executeInsert(this.groupMessageTable, new StorageField[] {
                new StorageField("code", channelCode),
                new StorageField("account_id", accountId),
                new StorageField("group_name", groupName),
                new StorageField("sender_name", senderName),
                new StorageField("date", plainMessage.getDate()),
                new StorageField("date_precision", plainMessage.getDatePrecision()),
                new StorageField("message", message.toJSON().toString())
        });
    }

    private void execSelfChecking() {
        if (!this.storage.exist(this.channelCodeTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.channelCodeTable, this.channelCodeFields)) {
                Logger.i(this.getClass(), "Created table '" + this.channelCodeTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.allocatingTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.allocatingTable, this.allocatingFields)) {
                Logger.i(this.getClass(), "Created table '" + this.allocatingTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.accountTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.accountTable, this.accountFields)) {
                Logger.i(this.getClass(), "Created table '" + this.accountTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.contactBookTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.contactBookTable, this.contactBookFields)) {
                Logger.i(this.getClass(), "Created table '" + this.contactBookTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.groupTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.groupTable, this.groupFields)) {
                Logger.i(this.getClass(), "Created table '" + this.groupTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.groupMemberTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.groupMemberTable, this.groupMemberFields)) {
                Logger.i(this.getClass(), "Created table '" + this.groupMemberTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.partnerMessageTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.partnerMessageTable, this.partnerMessageFields)) {
                Logger.i(this.getClass(), "Created table '" + this.partnerMessageTable + "' successfully");
            }
        }

        if (!this.storage.exist(this.groupMessageTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.groupMessageTable, this.groupMessageFields)) {
                Logger.i(this.getClass(), "Created table '" + this.groupMessageTable + "' successfully");
            }
        }
    }
}
