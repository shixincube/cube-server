/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.common.JSONable;
import cube.console.tool.Detector;
import cube.util.NodeName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 调度服务器描述。
 */
public class DispatcherServer implements JSONable {

    public final long timestamp = System.currentTimeMillis();

    public final String tag;

    public final String deployPath;

    private boolean local = false;

    private String name;

    private CellConfigFile cellConfigFile;

    private DispatcherProperties propertiesFile;

    private boolean running = false;

    public DispatcherServer(String tag, String deployPath, String cellConfigFile, String propertiesFile) {
        this.tag = tag;
        this.deployPath = deployPath;
        this.cellConfigFile = new CellConfigFile(cellConfigFile);
        this.propertiesFile = new DispatcherProperties(propertiesFile);
    }

    public boolean isLocal() {
        return this.local;
    }

    public String getName() {
        return this.name;
    }

    public boolean isRunning() {
        return this.running;
    }

    /**
     * 更新配置。
     *
     * @param data
     * @throws JSONException
     */
    public void updateCellConfig(JSONObject data) throws JSONException {
        boolean cellModified = false;
        boolean propModified = false;

        if (data.has("server")) {
            JSONObject serverJson = data.getJSONObject("server");
            AccessPoint serverAP = new AccessPoint(serverJson);
            if (!this.cellConfigFile.getAccessPoint().equals(serverAP)) {
                this.cellConfigFile.setAccessPoint(serverAP);
                cellModified = true;
            }
        }

        if (data.has("wsServer")) {
            JSONObject serverJson = data.getJSONObject("wsServer");
            AccessPoint serverAP = new AccessPoint(serverJson);
            if (!this.cellConfigFile.getWSAccessPoint().equals(serverAP)) {
                this.cellConfigFile.setWSAccessPoint(serverAP);
                cellModified = true;
            }
        }

        if (data.has("wssServer")) {
            JSONObject serverJson = data.getJSONObject("wssServer");
            AccessPoint serverAP = new AccessPoint(serverJson);
            if (!this.cellConfigFile.getWSSAccessPoint().equals(serverAP)) {
                this.cellConfigFile.setWSSAccessPoint(serverAP);
                cellModified = true;
            }
        }

        if (data.has("http")) {
            JSONObject httpJson = data.getJSONObject("http");
            AccessPoint httpAP = new AccessPoint(httpJson);
            if (!this.propertiesFile.getHttpAccessPoint().equals(httpAP)) {
                this.propertiesFile.setHttpAccessPoint(httpAP);
                propModified = true;
            }
        }

        if (data.has("https")) {
            JSONObject httpsJson = data.getJSONObject("https");
            AccessPoint httpsAP = new AccessPoint(httpsJson);
            if (!this.propertiesFile.getHttpsAccessPoint().equals(httpsAP)) {
                this.propertiesFile.setHttpsAccessPoint(httpsAP);
                propModified = true;
            }
        }

        if (data.has("ssl")) {
            JSONObject sslJson = data.getJSONObject("ssl");
            if (this.cellConfigFile.setSSLConfig(sslJson)) {
                this.propertiesFile.setKeystoreProperties(sslJson.getString("keystore"),
                        sslJson.getString("storePassword"), sslJson.getString("managerPassword"));
                cellModified = true;
                propModified = true;
            }
        }

        if (data.has("logLevel")) {
            String logLevel = data.getString("logLevel");
            if (!this.cellConfigFile.getLogLevelAsString().equalsIgnoreCase(logLevel)) {
                this.cellConfigFile.setLogLevel(logLevel);
                cellModified = true;
            }
        }

        if (data.has("cellets")) {
            JSONArray array = data.getJSONArray("cellets");
            String[] list = new String[array.length()];
            for (int i = 0; i < array.length(); ++i) {
                list[i] = array.getString(i);
            }
            if (!this.propertiesFile.equalsCellets(list)) {
                this.propertiesFile.setCellets(list);
                propModified = true;
            }
        }

        if (data.has("directors")) {
            JSONArray array = data.getJSONArray("directors");
            List<DirectorProperties> list = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                DirectorProperties dp = new DirectorProperties(array.getJSONObject(i));
                list.add(dp);
            }

            if (this.propertiesFile.updateDirectorProperties(list)) {
                propModified = true;
            }
        }

        if (cellModified) {
            Logger.i(this.getClass(), "#updateCellConfig - modify cell config: " + this.cellConfigFile.getFullPath());
            this.cellConfigFile.save();
        }
        if (propModified) {
            Logger.i(this.getClass(), "#updateCellConfig - modify properties: " + this.propertiesFile.getFullPath());
            this.propertiesFile.save();
        }
    }

    protected void refresh() {
        this.local = this.cellConfigFile.load();

        if (!this.local) {
            this.name = this.tag + "#dispatcher#" + this.deployPath;
            return;
        }

        this.propertiesFile.load();

        this.name = NodeName.makeName(this.tag, NodeName.ROLE_DISPATCHER,
                this.cellConfigFile.getAccessPoint().getPort());

        // 检查是否正在运行
        this.refreshRunning();
    }

    /**
     * 刷新运行状态（不重新加载配置文件，供定时任务高频调用）。
     *
     * 两个信号取或：
     * 1. 控制台在存活窗口内收到过该节点提交的报告 —— 上报即心跳，不要求控制台能反向连上节点；
     * 2. 节点 SHM 访问点端口能从本机建立 TCP 连接 —— 覆盖节点刚启动、首份报告尚未到达的窗口期。
     *
     * 不再依赖 `<deployPath>/bin/tag_dispatcher` 这类由启动脚本写入的标记文件：它同时耦合了部署根目录
     * 和 `-tag` 参数名（开发态直接从模块目录启动时标记文件落在模块的 bin 目录、名字也不一定是 dispatcher），
     * 一旦不匹配就会把正在运行的节点判定为未运行。
     */
    protected void refreshRunning() {
        long now = System.currentTimeMillis();

        if (NodeHeartbeat.getInstance().isAlive(this.name, now)) {
            this.running = true;
            return;
        }

        AccessPoint ap = this.cellConfigFile.getAccessPoint();
        this.running = (null != ap) && Detector.isPortReachable(ap.getHost(), ap.getPort());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("tag", this.tag);
        json.put("deployPath", this.deployPath);
        json.put("name", this.name);
        json.put("cellConfigFile", this.cellConfigFile.getFullPath());
        json.put("propertiesFile", this.propertiesFile.getFullPath());
        json.put("running", this.running);
        json.put("server", this.cellConfigFile.getAccessPoint().toJSON());
        json.put("wsServer", this.cellConfigFile.getWSAccessPoint().toJSON());
        json.put("wssServer", this.cellConfigFile.getWSSAccessPoint().toJSON());
        json.put("http", this.propertiesFile.getHttpAccessPoint().toJSON());
        json.put("https", this.propertiesFile.getHttpsAccessPoint().toJSON());

        CellConfigFile.SSLConfig sslConfig = this.cellConfigFile.getSSLConfig();
        if (null != sslConfig) {
            json.put("ssl", sslConfig.toJSON());
        }

        json.put("logLevel", this.cellConfigFile.getLogLevelAsString());

        JSONArray cellets = new JSONArray();
        for (String cellet : this.propertiesFile.getCellets()) {
            cellets.put(cellet);
        }
        json.put("cellets", cellets);

        JSONArray directors = new JSONArray();
        for (DirectorProperties dp : this.propertiesFile.getDirectorProperties()) {
            directors.put(dp.toJSON());
        }
        json.put("directors", directors);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
