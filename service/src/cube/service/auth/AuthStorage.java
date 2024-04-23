/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.auth;

import cell.core.net.Endpoint;
import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.auth.PrimaryDescription;
import cube.common.Storagable;
import cube.common.entity.AuthDomain;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

/**
 * 授权模块存储器。
 */
public class AuthStorage implements Storagable {

    /**
     * 域字段。
     */
    private final StorageField[] domainFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 域名称
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 应用 ID
            new StorageField("app_id", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 应用的访问 Key
            new StorageField("app_key", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 应用的访问地址
            new StorageField("address", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 可访问的 SHM 端口
            new StorageField("port", LiteralBase.INT, new Constraint[] {
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
            new StorageField("ice_servers", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("ferry", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            })
    };

    /**
     * 访问令牌字段。
     */
    private final StorageField[] tokenFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("app_key", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("code", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("expiry", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("issues", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("cid", LiteralBase.LONG, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            new StorageField("primary_content", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("ferry", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL, Constraint.DEFAULT_0
            })
    };

    private final String domainTable = "auth_domain";

    private final String tokenTable = "auth_token";

    private Storage storage;

    public AuthStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "AuthStorage", config);
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
        this.checkDomainTable();
        this.checkTokenTable();
        this.buildInData();
    }

    /**
     * 列举所有域名称。
     *
     * @return
     */
    public List<String> listDomains() {
        List<StorageField[]> result = this.storage.executeQuery(this.domainTable, new StorageField[] {
                new StorageField("domain", LiteralBase.STRING)
        });

        if (null == result) {
            return null;
        }

        List<String> list = new ArrayList<>();
        if (!result.isEmpty()) {
            for (StorageField[] data : result) {
                String domain = data[0].getString();
                if (!list.contains(domain)) {
                    list.add(domain);
                }
            }
        }

        return list;
    }

    /**
     * 是否存在指定的域。
     *
     * @param domainName
     * @return
     */
    public boolean existsDomain(String domainName) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainTable, new StorageField[] {
                new StorageField("sn", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("domain", domainName)
        });

        if (result.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * 添加域应用。
     *
     * @param domain 域名称。
     * @param appId App ID 。
     * @param appKey App Key 。
     * @param main 主访问节点。
     * @param http HTTP 访问节点。
     * @param https HTTPS 访问节点。
     * @param iceServers Ice Server 服务器列表。
     * @param ferry 是否 Ferry 属性。
     */
    public void addDomainApp(String domain, String appId, String appKey, Endpoint main, Endpoint http, Endpoint https,
        JSONArray iceServers, boolean ferry) {
        List<StorageField[]> result = this.storage.executeQuery(this.domainTable, this.domainFields, new Conditional[] {
                Conditional.createEqualTo("domain", domain),
                Conditional.createAnd(),
                Conditional.createEqualTo("app_id", appId)
        });
        if (result.isEmpty()) {
            this.storage.executeInsert(this.domainTable, new StorageField[] {
                    new StorageField("domain", LiteralBase.STRING, domain),
                    new StorageField("app_id", LiteralBase.STRING, appId),
                    new StorageField("app_key", LiteralBase.STRING, appKey),
                    new StorageField("address", LiteralBase.STRING, main.getHost()),
                    new StorageField("port", LiteralBase.INT, main.getPort()),
                    new StorageField("http_address", LiteralBase.STRING, http.getHost()),
                    new StorageField("http_port", LiteralBase.INT, http.getPort()),
                    new StorageField("https_address", LiteralBase.STRING, https.getHost()),
                    new StorageField("https_port", LiteralBase.INT, https.getPort()),
                    new StorageField("ice_servers", LiteralBase.STRING, iceServers.toString()),
                    new StorageField("ferry", LiteralBase.INT, ferry ? 1 : 0)
            });
        }
        else {
            this.storage.executeUpdate(this.domainTable, new StorageField[] {
                    new StorageField("app_key", LiteralBase.STRING, appKey),
                    new StorageField("address", LiteralBase.STRING, main.getHost()),
                    new StorageField("port", LiteralBase.INT, main.getPort()),
                    new StorageField("http_address", LiteralBase.STRING, http.getHost()),
                    new StorageField("http_port", LiteralBase.INT, http.getPort()),
                    new StorageField("https_address", LiteralBase.STRING, https.getHost()),
                    new StorageField("https_port", LiteralBase.INT, https.getPort()),
                    new StorageField("ice_servers", LiteralBase.STRING, iceServers.toString()),
                    new StorageField("ferry", LiteralBase.INT, ferry ? 1 : 0)
            }, new Conditional[] {
                    Conditional.createEqualTo("domain", domain),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("app_id", appId)
            });
        }
    }

    /**
     * 移除域应用。
     *
     * @param domain
     * @param appId
     * @param appKey
     */
    public void removeDomainApp(String domain, String appId, String appKey) {
        this.storage.executeDelete(this.domainTable, new Conditional[] {
                Conditional.createEqualTo("domain", LiteralBase.STRING, domain),
                Conditional.createAnd(),
                Conditional.createEqualTo("app_id", LiteralBase.STRING, appId),
                Conditional.createAnd(),
                Conditional.createEqualTo("app_key", LiteralBase.STRING, appKey)
        });
    }

    /**
     * 更新指定域的接入点数据。
     *
     * @param domain
     * @param main
     * @param http
     * @param https
     * @return
     */
    public boolean updateDomain(String domain, Endpoint main, Endpoint http, Endpoint https) {
        return this.storage.executeUpdate(this.domainTable, new StorageField[] {
                new StorageField("address", main.getHost()),
                new StorageField("port", main.getPort()),
                new StorageField("http_address", http.getHost()),
                new StorageField("http_port", http.getPort()),
                new StorageField("https_address", https.getHost()),
                new StorageField("https_port", https.getPort()),
        }, new Conditional[] {
                Conditional.createEqualTo("domain", domain),
        });
    }

    /**
     * 获取访问域。
     *
     * @param domain
     * @return
     */
    public AuthDomain getDomain(String domain) {
        return this.getDomain(domain, null);
    }

    /**
     * 获取访问域。
     *
     * @param domain
     * @param appKey 可以设置 {@code null} 值。
     * @return
     */
    public AuthDomain getDomain(String domain, String appKey) {
        List<StorageField[]> result = null;

        if (null != appKey) {
            result = this.storage.executeQuery(this.domainTable, this.domainFields,
                    new Conditional[] {
                            Conditional.createEqualTo("domain", LiteralBase.STRING, domain),
                            Conditional.createAnd(),
                            Conditional.createEqualTo("app_key", LiteralBase.STRING, appKey),
                    });
        }
        else {
            result = this.storage.executeQuery(this.domainTable, this.domainFields,
                    new Conditional[] {
                            Conditional.createEqualTo("domain", LiteralBase.STRING, domain)
                    });
        }

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        Endpoint mainEndpoint = new Endpoint(map.get("address").getString(), map.get("port").getInt());
        Endpoint httpEndpoint = new Endpoint(map.get("http_address").getString(), map.get("http_port").getInt());
        Endpoint httpsEndpoint = new Endpoint(map.get("https_address").getString(), map.get("https_port").getInt());
        JSONArray iceServers = new JSONArray(map.get("ice_servers").getString());
        boolean ferry = map.get("ferry").getInt() == 1;

        AuthDomain authDomain = new AuthDomain(domain, (null == appKey) ? map.get("app_key").getString() : appKey,
                map.get("app_id").getString(),
                mainEndpoint, httpEndpoint, httpsEndpoint, iceServers, ferry);

        return authDomain;
    }

    public boolean writeToken(AuthToken token) {
        return this.storage.executeInsert(this.tokenTable, new StorageField[] {
                new StorageField("domain", LiteralBase.STRING, token.getDomain()),
                new StorageField("app_key", LiteralBase.STRING, token.getAppKey()),
                new StorageField("code", LiteralBase.STRING, token.getCode()),
                new StorageField("expiry", LiteralBase.LONG, token.getExpiry()),
                new StorageField("issues", LiteralBase.LONG, token.getIssues()),
                new StorageField("cid", LiteralBase.LONG, token.getContactId()),
                new StorageField("primary_content", LiteralBase.STRING,
                        token.getDescription().toJSON().toString()),
                new StorageField("ferry", LiteralBase.INT, token.isFerry() ? 1 : 0)
        });
    }

    public AuthToken readToken(String code) {
        List<StorageField[]> result = this.storage.executeQuery(this.tokenTable, this.tokenFields, new Conditional[] {
                Conditional.createEqualTo("code", LiteralBase.STRING, code)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        Date issues = new Date(map.get("issues").getLong());
        Date expiry = new Date(map.get("expiry").getLong());

        AuthToken token = new AuthToken(map.get("code").getString(), map.get("domain").getString(),
                map.get("app_key").getString(), map.get("cid").getLong(), issues, expiry,
                new PrimaryDescription(new JSONObject(map.get("primary_content").getString())),
                map.get("ferry").getInt() == 1);
        return token;
    }

    public AuthToken readToken(String domain, long contactId) {
        List<StorageField[]> result = this.storage.executeQuery(this.tokenTable, this.tokenFields, new Conditional[] {
                Conditional.createEqualTo("domain", domain),
                Conditional.createAnd(),
                Conditional.createEqualTo("cid", contactId)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        Date issues = new Date(map.get("issues").getLong());
        Date expiry = new Date(map.get("expiry").getLong());

        AuthToken token = new AuthToken(map.get("code").getString(), map.get("domain").getString(),
                map.get("app_key").getString(), map.get("cid").getLong(), issues, expiry,
                new PrimaryDescription(new JSONObject(map.get("primary_content").getString())),
                map.get("ferry").getInt() == 1);
        return token;
    }

    public boolean deleteToken(String domain, long contactId) {
        return this.storage.executeDelete(this.tokenTable, new Conditional[] {
                Conditional.createEqualTo("domain", domain),
                Conditional.createAnd(),
                Conditional.createEqualTo("cid", contactId)
        });
    }

    public void updateToken(AuthToken token) {
        this.storage.executeUpdate(this.tokenTable, new StorageField[] {
                new StorageField("cid", token.getContactId())
        }, new Conditional[] {
                Conditional.createEqualTo("code", token.getCode())
        });
    }

    public boolean existsToken(String tokenCode) {
        List<StorageField[]> result = this.storage.executeQuery(this.tokenTable, new StorageField[] {
                new StorageField("cid", LiteralBase.LONG)
        }, new Conditional[] {
                Conditional.createEqualTo("code", tokenCode)
        });

        return !result.isEmpty();
    }

    /**
     * 通过 CID 查询授权令牌。
     *
     * @param cid 指定联系人 ID 。
     * @return 返回授权令牌实例。
     */
    public AuthToken queryToken(long cid) {
        List<StorageField[]> result = this.storage.executeQuery(this.tokenTable, this.tokenFields, new Conditional[] {
                Conditional.createEqualTo("cid", cid)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        Date issues = new Date(map.get("issues").getLong());
        Date expiry = new Date(map.get("expiry").getLong());

        AuthToken token = new AuthToken(map.get("code").getString(), map.get("domain").getString(),
                map.get("app_key").getString(), map.get("cid").getLong(), issues, expiry,
                new PrimaryDescription(new JSONObject(map.get("primary_content").getString())),
                map.get("ferry").getInt() == 1);
        return token;
    }

    private void checkDomainTable() {
        if (!this.storage.exist(this.domainTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.domainTable, this.domainFields)) {
                Logger.i(this.getClass(), "Created table '" + this.domainTable + "' successfully");

                // 插入默认数据

                JSONArray iceServers = new JSONArray("[{" +
                        "\"urls\": \"turn:52.83.195.35:3478\"," +
                        "\"username\": \"cube\"," +
                        "\"credential\": \"cube887\"" +
                    "}]");
                this.addDomainApp(AuthConsts.DEFAULT_DOMAIN, AuthConsts.DEFAULT_APP_ID,
                        AuthConsts.DEFAULT_APP_KEY,
                        new Endpoint("127.0.0.1", 7000),
                        new Endpoint("127.0.0.1", 7010),
                        new Endpoint("127.0.0.1", 7017),
                        iceServers,
                        false);
            }
        }
    }

    private void checkTokenTable() {
        if (!this.storage.exist(this.tokenTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.tokenTable, this.tokenFields)) {
                Logger.i(this.getClass(), "Created table '" + this.tokenTable + "' successfully");
            }
        }
    }

    private void buildInData() {
        File file = new File("config/auth.properties");
        if (file.exists()) {
            try {
                JSONArray iceServers = new JSONArray("[{" +
                        "\"urls\": \"turn:52.83.195.35:3478\"," +
                        "\"username\": \"cube\"," +
                        "\"credential\": \"cube887\"" +
                    "}]");

                Properties properties = ConfigUtils.readProperties(file.getAbsolutePath());
                boolean enabled = Boolean.parseBoolean(properties.getProperty("domain.1.enabled").trim());
                if (enabled) {
                    String domain = properties.getProperty("domain.1.name").trim();
                    String appId = properties.getProperty("domain.1.app_id").trim();
                    String appKey = properties.getProperty("domain.1.app_key").trim();
                    String host = properties.getProperty("domain.1.host").trim();
                    String http = properties.getProperty("domain.1.http").trim();
                    String https = properties.getProperty("domain.1.https").trim();

                    String[] tmp = host.split(":");
                    Endpoint mainHost = new Endpoint(tmp[0], Integer.parseInt(tmp[1]));

                    tmp = http.split(":");
                    Endpoint httpHost = new Endpoint(tmp[0], Integer.parseInt(tmp[1]));

                    tmp = https.split(":");
                    Endpoint httpsHost = new Endpoint(tmp[0], Integer.parseInt(tmp[1]));

                    Logger.i(this.getClass(), "Activate domain app: " + domain + "/" + appId);

                    this.addDomainApp(domain, appId, appKey,
                            mainHost,
                            httpHost,
                            httpsHost,
                            iceServers,
                            false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e(this.getClass(), "#buildInData", e);
            }
        }
    }
}
