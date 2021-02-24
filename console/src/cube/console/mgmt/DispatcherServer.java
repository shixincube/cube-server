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

/**
 * 调度服务器描述。
 */
public class DispatcherServer implements JSONable {

    public final String tag;

    public final String deployPath;

    private CellConfigFile cellConfigFile;

    private DispatcherProperties propertiesFile;

    private boolean running = false;

    public DispatcherServer(String tag, String deployPath, String cellConfigFile, String propertiesFile) {
        this.tag = tag;
        this.deployPath = deployPath;
        this.cellConfigFile = new CellConfigFile(cellConfigFile);
        this.propertiesFile = new DispatcherProperties(propertiesFile);
    }

    public void refresh() {
        this.cellConfigFile.refresh();
        this.propertiesFile.refresh();

        // 检查是否正在运行
        File tagFile = new File(this.deployPath + File.separator + "bin/tag_dispatcher");
        if (tagFile.exists()) {
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
        json.put("cellConfigFile", this.cellConfigFile.getFullPath());
        json.put("propertiesFile", this.propertiesFile);
        json.put("running", this.running);
        json.put("server", this.cellConfigFile.getAccessPoint().toJSON());
        json.put("wsServer", this.cellConfigFile.getWSAccessPoint().toJSON());
        json.put("wssServer", this.cellConfigFile.getWSSAccessPoint().toJSON());
        json.put("http", this.propertiesFile.getHttpAccessPoint().toJSON());
        json.put("https", this.propertiesFile.getHttpsAccessPoint().toJSON());

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
