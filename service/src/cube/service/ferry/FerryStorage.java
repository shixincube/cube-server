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

package cube.service.ferry;

import cell.core.net.Endpoint;
import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.IceServer;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.ferry.DomainInfo;
import cube.ferry.DomainMember;
import cube.ferry.JoinWay;
import cube.ferry.Role;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ferry 数据存储器。
 */
public class FerryStorage implements Storagable {

    private final StorageField[] accessPointFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("main_address", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("main_port", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("http_address", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("http_port", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("https_address", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("https_port", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("ice_server", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 优先级，从低到高，依次为 1 - 3
            new StorageField("priority", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] domainInfoFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("beginning", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("duration", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("limit", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("invitation_code", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("address", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    private final StorageField[] domainMemberFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 域
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 联系人 ID
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 加入方式
            new StorageField("way", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 时间戳
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 角色
            new StorageField("role", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 状态
            new StorageField("state", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            })
    };

    private final String accessPointTable = "ferry_access_point";

    private final String domainInfoTable = "ferry_domain";

    private final String domainMemberTable = "ferry_domain_member";

    private Storage storage;

    public FerryStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "FerryStorage", config);
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
        this.checkAccessPointTable();
        this.checkDomainInfoTable();
        this.checkDomainMemberTable();
    }

    /**
     * 写入接入点信息。
     *
     * @param mainEndpoint
     * @param httpEndpoint
     * @param httpsEndpoint
     * @param iceServer
     * @param domainName
     */
    public void writeAccessPoint(Endpoint mainEndpoint, Endpoint httpEndpoint, Endpoint httpsEndpoint,
                               IceServer iceServer, String domainName) {
        this.storage.executeInsert(this.accessPointTable, new StorageField[] {
                new StorageField("main_address", LiteralBase.STRING, mainEndpoint.getHost()),
                new StorageField("main_port", LiteralBase.INT, mainEndpoint.getPort()),
                new StorageField("http_address", LiteralBase.STRING, httpEndpoint.getHost()),
                new StorageField("http_port", LiteralBase.INT, httpEndpoint.getPort()),
                new StorageField("https_address", LiteralBase.STRING, httpsEndpoint.getHost()),
                new StorageField("https_port", LiteralBase.INT, httpsEndpoint.getPort()),
                new StorageField("ice_server", LiteralBase.STRING, iceServer.toJSON().toString()),
                new StorageField("priority", LiteralBase.INT, 1),
                new StorageField("domain", LiteralBase.STRING, (null != domainName) ? domainName : "")
        });
    }

    /**
     * 读取所有接入点。
     *
     * @return
     */
    public List<AccessPoint> readAccessPoints() {
        List<AccessPoint> list = new ArrayList<>();

        List<StorageField[]> result = this.storage.executeQuery(this.accessPointTable, this.accessPointFields,
                new Conditional[] {
                        Conditional.createOrderBy("priority", true)
                });

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            AccessPoint accessPoint = new AccessPoint();
            accessPoint.mainEndpoint = new Endpoint(map.get("main_address").getString(),
                    map.get("main_port").getInt());
            accessPoint.httpEndpoint = new Endpoint(map.get("http_address").getString(),
                    map.get("http_port").getInt());
            accessPoint.httpsEndpoint = new Endpoint(map.get("https_address").getString(),
                    map.get("https_port").getInt());
            accessPoint.iceServer = new IceServer(new JSONObject(map.get("ice_server").getString()));
            accessPoint.priority = map.get("priority").getInt();
            accessPoint.domainName = map.get("domain").getString();
            list.add(accessPoint);
        }

        return list;
    }

    /**
     * 读取指定域
     * @param domainName
     * @return
     */
    public AccessPoint readAccessPoint(String domainName) {
        AccessPoint accessPoint = null;

        List<StorageField[]> result = this.storage.executeQuery(this.accessPointTable, this.accessPointFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domainName),
                        Conditional.createOrderBy("priority", true)
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        accessPoint = new AccessPoint();
        accessPoint.mainEndpoint = new Endpoint(map.get("main_address").getString(),
                map.get("main_port").getInt());
        accessPoint.httpEndpoint = new Endpoint(map.get("http_address").getString(),
                map.get("http_port").getInt());
        accessPoint.httpsEndpoint = new Endpoint(map.get("https_address").getString(),
                map.get("https_port").getInt());
        accessPoint.iceServer = new IceServer(new JSONObject(map.get("ice_server").getString()));
        accessPoint.priority = map.get("priority").getInt();
        accessPoint.domainName = map.get("domain").getString();

        return accessPoint;
    }

    public void deleteAccessPoint(String domainName) {
        // TODO
    }

    public void writeDomainInfo(DomainInfo domainInfo) {
        this.writeDomainInfo(domainInfo.getDomain().getName(), domainInfo.getBeginning(),
                domainInfo.getDuration(), domainInfo.getLimit(), domainInfo.getAddress());
    }

    /**
     * 写入域信息。
     *
     * @param domainName
     * @param beginning
     * @param duration
     * @param limit
     * @param address
     */
    public synchronized void writeDomainInfo(String domainName, long beginning, long duration, int limit, String address) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainInfoTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("domain", domainName)
        });

        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.domainInfoTable, new StorageField[] {
                    new StorageField("domain", domainName),
                    new StorageField("beginning", beginning),
                    new StorageField("duration", duration),
                    new StorageField("limit", limit),
                    new StorageField("address", address)
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.domainInfoTable, new StorageField[] {
                    new StorageField("beginning", beginning),
                    new StorageField("duration", duration),
                    new StorageField("limit", limit),
                    new StorageField("address", address)
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", result.get(0)[0].getLong())
            });
        }
    }

    /**
     * 更新域的邀请码。
     *
     * @param domainName
     * @param invitationCode
     */
    public void updateInvitationCode(String domainName, String invitationCode) {
        this.storage.executeUpdate(this.domainInfoTable, new StorageField[] {
                new StorageField("invitation_code", invitationCode)
        }, new Conditional[] {
                Conditional.createEqualTo("domain", domainName)
        });
    }

    /**
     * 读取域信息。
     *
     * @param domainName
     * @return
     */
    public DomainInfo readDomainInfo(String domainName) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainInfoTable, this.domainInfoFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domainName)
                });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        long beginning = map.get("beginning").getLong();
        long duration = map.get("duration").getLong();
        int limit = map.get("limit").getInt();
        DomainInfo domainInfo = new DomainInfo(domainName, beginning, duration, limit);

        if (!map.get("address").isNullValue()) {
            domainInfo.setAddress(map.get("address").getString());
        }

        if (!map.get("invitation_code").isNullValue()) {
            domainInfo.setInvitationCode(map.get("invitation_code").getString());
        }
        return domainInfo;
    }

    /**
     * 查询指定域的所有成员。
     *
     * @param domainName
     * @return
     */
    public List<DomainMember> queryMembers(String domainName) {
        return this.queryMembers(domainName, -1);
    }

    /**
     * 查询指定域的成员。
     *
     * @param domainName
     * @param state
     * @return
     */
    public List<DomainMember> queryMembers(String domainName, int state) {
        List<DomainMember> list = new ArrayList<>();

        Conditional[] conditionals = null;
        if (state >= 0) {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("domain", domainName),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("state", state)
            };
        }
        else {
            conditionals = new Conditional[] {
                    Conditional.createEqualTo("domain", domainName)
            };
        }

        List<StorageField[]> result = this.storage.executeQuery(this.domainMemberTable, this.domainMemberFields,
                conditionals);
        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);
            DomainMember member = new DomainMember(domainName, map.get("contact_id").getLong(),
                    JoinWay.parse(map.get("way").getInt()), map.get("timestamp").getLong(),
                    Role.parse(map.get("role").getInt()), map.get("state").getInt());
            list.add(member);
        }

        return list;
    }

    /**
     * 域成员数量。
     *
     * @param domainName
     * @return
     */
    public int countDomainMembers(String domainName) {
        List<StorageField[]> result = this.storage.executeQuery("SELECT COUNT(sn) FROM `"
                + this.domainMemberTable + "` WHERE `domain`='" + domainName + "'");
        if (result.isEmpty()) {
            return -1;
        }

        return result.get(0)[0].getInt();
    }

    /**
     * 写入域成员数据。
     *
     * @param member
     */
    public void writeMember(DomainMember member) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainMemberTable,
                new StorageField[] {
                        new StorageField("sn", LiteralBase.LONG)
                }, new Conditional[] {
                        Conditional.createEqualTo("domain", member.getDomain().getName()),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", member.getContactId())
                });
        if (result.isEmpty()) {
            // 插入
            this.storage.executeInsert(this.domainMemberTable, new StorageField[] {
                    new StorageField("domain", LiteralBase.STRING, member.getDomain().getName()),
                    new StorageField("contact_id", LiteralBase.LONG, member.getContactId()),
                    new StorageField("way", LiteralBase.INT, member.getJoinWay().code),
                    new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis()),
                    new StorageField("role", LiteralBase.INT, member.getRole().code),
                    new StorageField("state", LiteralBase.INT, member.getState())
            });
        }
        else {
            // 更新
            this.storage.executeUpdate(this.domainMemberTable, new StorageField[] {
                    new StorageField("way", LiteralBase.INT, member.getJoinWay().code),
                    new StorageField("role", LiteralBase.INT, member.getRole().code),
                    new StorageField("state", LiteralBase.INT, member.getState())
            }, new Conditional[] {
                    Conditional.createEqualTo("sn", result.get(0)[0].getLong())
            });
        }
    }

    /**
     * 读取指定域成员。
     *
     * @param domainName
     * @param contactId
     * @return
     */
    public DomainMember readMember(String domainName, Long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainMemberTable, this.domainMemberFields,
                new Conditional[] {
                        Conditional.createEqualTo("domain", domainName),
                        Conditional.createAnd(),
                        Conditional.createEqualTo("contact_id", contactId.longValue())
                });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        DomainMember member = new DomainMember(domainName, map.get("contact_id").getLong(),
                JoinWay.parse(map.get("way").getInt()), map.get("timestamp").getLong(),
                Role.parse(map.get("role").getInt()), map.get("state").getInt());
        return member;
    }

    /**
     * 更新域成员状态。
     *
     * @param domainName
     * @param contactId
     * @param state
     */
    public void updateMemberState(String domainName, Long contactId, int state) {
        this.storage.executeUpdate(this.domainMemberTable, new StorageField[] {
                new StorageField("state", state)
        }, new Conditional[] {
                Conditional.createEqualTo("domain", domainName),
                Conditional.createAnd(),
                Conditional.createEqualTo("contact_id", contactId.longValue())
        });
    }

    private void checkAccessPointTable() {
        if (!this.storage.exist(this.accessPointTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.accessPointTable, this.accessPointFields)) {
                Logger.i(this.getClass(), "Created table '" + this.accessPointTable + "' successfully");

                // 插入 Demo 数据

                Endpoint main = new Endpoint("192.168.0.200", 7000);
                Endpoint http = new Endpoint("192.168.0.200", 7010);
                Endpoint https = new Endpoint("192.168.0.200", 7017);
                IceServer iceServer = new IceServer("turn:52.83.195.35:3478", "cube", "cube887");
                this.writeAccessPoint(main, http, https, iceServer, null);
            }
        }
    }

    private void checkDomainInfoTable() {
        if (!this.storage.exist(this.domainInfoTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.domainInfoTable, this.domainInfoFields)) {
                Logger.i(this.getClass(), "Created table '" + this.domainInfoTable + "' successfully");
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

    public class AccessPoint {

        public Endpoint mainEndpoint;

        public Endpoint httpEndpoint;

        public Endpoint httpsEndpoint;

        public IceServer iceServer;

        public int priority;

        public String domainName;

        public AccessPoint() {
        }
    }
}
