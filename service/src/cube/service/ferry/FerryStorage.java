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

    private final StorageField[] accessPointFields = new StorageField[]{
            new StorageField("sn", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("main_address", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("main_port", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("http_address", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("http_port", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("https_address", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("https_port", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("ice_server", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 优先级，从低到高，依次为 1 - 3
            new StorageField("priority", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[]{
                    Constraint.DEFAULT_NULL
            })
    };

    private final String accessPointTable = "ferry_access_point";

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

        List<StorageField[]> result = this.storage.executeQuery(this.accessPointTable, this.accessPointFields);
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

    public void deleteAccessPoint(String domainName) {

    }

    private void checkAccessPointTable() {
        if (!this.storage.exist(this.accessPointTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.accessPointTable, this.accessPointFields)) {
                Logger.i(this.getClass(), "Created table '" + this.accessPointTable + "' successfully");

                // 插入 Demo 数据

                Endpoint main = new Endpoint("api.shixincube.com", 7000);
                Endpoint http = new Endpoint("api.shixincube.com", 7010);
                Endpoint https = new Endpoint("api.shixincube.com", 7017);
                IceServer iceServer = new IceServer("turn:52.83.195.35:3478", "cube", "cube887");
                this.writeAccessPoint(main, http, https, iceServer, null);
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