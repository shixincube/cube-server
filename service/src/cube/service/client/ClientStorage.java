/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.ClientDescription;
import cube.common.entity.ClientState;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * 客户端管理器存储器。
 */
public class ClientStorage implements Storagable {

    private final StorageField[] clientFields = new StorageField[] {
            // 序号
            new StorageField("sn", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 客户端名
            new StorageField("name", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 密码
            new StorageField("password", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 创建日期
            new StorageField("creation", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 状态
            new StorageField("state", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            })
    };

    private final String clientTable = "server_client";

    private Storage storage;

    public ClientStorage(StorageType type, JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(type, "ClientStorage", config);
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
        checkClientTable();
    }

    /**
     * 添加新客户端。
     *
     * @param client
     * @return
     */
    public boolean addClient(ClientDescription client) {
        List<StorageField[]> result = this.storage.executeQuery(this.clientTable, new StorageField[] {
                new StorageField("state", LiteralBase.INT),
        }, new Conditional[] {
                Conditional.createEqualTo("name", client.getName())
        });

        if (!result.isEmpty()) {
            return false;
        }

        return this.storage.executeInsert(this.clientTable, new StorageField[] {
                new StorageField("name", client.getName()),
                new StorageField("password", client.getPassword()),
                new StorageField("creation", System.currentTimeMillis()),
                new StorageField("state", client.getState().code)
        });
    }

    /**
     * 删除客户端数据。
     *
     * @param client
     * @return
     */
    public boolean deleteClient(ClientDescription client) {
        return this.storage.executeDelete(this.clientTable, new Conditional[] {
                Conditional.createEqualTo("name", client.getName()),
                Conditional.createAnd(),
                Conditional.createEqualTo("password", client.getPassword()),
        });
    }

    /**
     * 获取指定名称和密码的客户端。
     *
     * @param name
     * @param password
     * @return
     */
    public ClientDescription getClient(String name, String password) {
        List<StorageField[]> result = this.storage.executeQuery(this.clientTable, this.clientFields, new Conditional[] {
                Conditional.createEqualTo("name", name),
                Conditional.createAnd(),
                Conditional.createEqualTo("password", password.toLowerCase())
        });

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        return new ClientDescription(map.get("name").getString(), map.get("password").getString(),
                ClientState.parse(map.get("state").getInt()));
    }

    private void checkClientTable() {
        if (!this.storage.exist(this.clientTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.clientTable, this.clientFields)) {
                Logger.i(this.getClass(), "Created table '" + this.clientTable + "' successfully");

                // 插入演示数据
                String password = null;
                MessageDigest md5 = null;
                try {
                    md5 = MessageDigest.getInstance("MD5");
                    md5.update("shixincube.com".getBytes(Charset.forName("UTF-8")));
                    byte[] hashMD5 = md5.digest();
                    password = FileUtils.bytesToHexString(hashMD5);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ClientDescription client = new ClientDescription("admin", password);
                this.addClient(client);
            }
        }
    }
}
