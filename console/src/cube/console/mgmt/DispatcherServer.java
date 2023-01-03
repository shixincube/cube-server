/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.common.JSONable;
import cube.console.tool.Detector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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

        this.name = this.tag + "#dispatcher#" + this.cellConfigFile.getAccessPoint().getPort();

        // 检查是否正在运行
        File tagFile = new File(this.deployPath + File.separator + "bin" + File.separator + "tag_dispatcher");
        if (tagFile.exists() && tagFile.length() < 40) {
            // 尝试检测服务是否能连通
            AccessPoint ap = this.cellConfigFile.getAccessPoint();
            this.running = Detector.detectCellServer(ap.getHost(), ap.getPort());
        }
        else {
            this.running = false;
        }
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
