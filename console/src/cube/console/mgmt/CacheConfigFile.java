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

package cube.console.mgmt;

import cell.core.net.Endpoint;
import cell.util.log.Logger;
import cube.common.JSONable;
import cube.util.ConfigUtils;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * 缓存配置文件。
 */
public class CacheConfigFile implements JSONable {

    private String fullPath;

    private Properties properties;

    public CacheConfigFile(String fullPath) {
        this.fullPath = fullPath;
    }

    public void load() {
        try {
            this.properties = ConfigUtils.readProperties(this.fullPath);
        } catch (IOException e) {
            Logger.e(this.getClass(), "#load", e);
        }
    }

    public void save() {
        // 备份原数据
        this.backup();



        try {
            this.properties.store(new FileWriter(new File(this.fullPath)), " SharedMemory config file");
        } catch (IOException e) {
            Logger.e(this.getClass(), "#save", e);
        }
    }

    private void backup() {
        File file = new File(this.fullPath);
        String filename = FileUtils.extractFileName(file.getName());

        String backupPath = file.getParent() + "/backup";
        File bp = new File(backupPath);
        if (!bp.exists()) {
            bp.mkdirs();
        }

        Path source = Paths.get(this.fullPath);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Path target = Paths.get(backupPath, filename + "_" + dateFormat.format(new Date()) + ".properties");

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("host", this.properties.getProperty("host"));
        json.put("port", Integer.parseInt(this.properties.getProperty("port")));
        json.put("capacity", Integer.parseInt(this.properties.getProperty("capacity")));
        json.put("expiry", Long.parseLong(this.properties.getProperty("expiry")));
        json.put("threshold", Long.parseLong(this.properties.getProperty("threshold")));
        json.put("blocking", Integer.parseInt(this.properties.getProperty("blocking")));

        json.put("storage", this.properties.getProperty("storage"));
        json.put("routetable", this.properties.getProperty("routetable"));

        JSONArray clusterNodes = new JSONArray();
        for (int i = 1; i <= 10; ++i) {
            if (!this.properties.containsKey("endpoint." + i + ".host")) {
                continue;
            }

            JSONObject ep = new JSONObject();
            ep.put("host", this.properties.getProperty("endpoint." + i + ".host"));
            ep.put("port", Integer.parseInt(this.properties.getProperty("endpoint." + i + ".port")));
            clusterNodes.put(ep);
        }
        json.put("clusterNodes", clusterNodes);

        if (this.properties.containsKey("pedestal.host")) {
            json.put("pedestal", (new Endpoint(this.properties.getProperty("pedestal.host"),
                    Integer.parseInt(this.properties.getProperty("pedestal.port")))).toJSON());
        }

        if (this.properties.containsKey("pedestal.backup.host")) {
            json.put("backupPedestal", (new Endpoint(this.properties.getProperty("pedestal.backup.host"),
                    Integer.parseInt(this.properties.getProperty("pedestal.backup.port")))).toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
