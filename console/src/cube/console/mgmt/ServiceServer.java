/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cube.common.JSONable;
import cube.console.tool.Detector;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 服务单元服务器。
 */
public class ServiceServer implements JSONable {

    public final String tag;

    public final String deployPath;

    public final String configPath;

    public final String celletsPath;

    private CellConfigFile cellConfigFile;

    private String storageJsonFile;

    private boolean running = false;

    public ServiceServer(String tag, String deployPath, String configPath, String celletsPath) {
        this.tag = tag;
        this.deployPath = deployPath;
        this.configPath = configPath;
        this.celletsPath = celletsPath;
    }

    public boolean isRunning() {
        return this.running;
    }

    protected void refresh() {
        try {
            this.cellConfigFile = new CellConfigFile(this.configPath + File.separator + "service.xml");
            this.cellConfigFile.refresh();

            // 检查是否正在运行
            File tagFile = new File(this.deployPath + File.separator + "bin/tag_service");
            if (tagFile.exists() && tagFile.length() < 40) {
                // 尝试检测服务是否能连通
                AccessPoint ap = this.cellConfigFile.getAccessPoint();
                this.running = Detector.detectCellServer(ap.getHost(), ap.getPort());
            }
            else {
                this.running = false;
            }

            // 检查对应的 Cellet 文件
            List<CellConfigFile.CelletConfig> celletConfigs = this.cellConfigFile.getCelletConfigList();
            for (CellConfigFile.CelletConfig cc : celletConfigs) {
                String jarFilePath = cc.getJarFilePath();
                if (null != jarFilePath) {
                    Path path = Paths.get(this.deployPath, jarFilePath);
                    File file = path.toAbsolutePath().toFile();
                    if (file.exists()) {
                        cc.setJarFile(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("tag", this.tag);
        json.put("deployPath", this.deployPath);
        json.put("configPath", this.configPath);
        json.put("celletsPath", this.celletsPath);

        json.put("running", this.running);

        json.put("server", this.cellConfigFile.getAccessPoint().toJSON());

        List<CellConfigFile.CelletConfig> list = this.cellConfigFile.getCelletConfigList();
        JSONArray array = new JSONArray();
        for (CellConfigFile.CelletConfig config : list) {
            array.put(config.toJSON());
        }
        json.put("cellets", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
