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
import org.json.JSONObject;

import java.io.File;

/**
 * 服务单元服务器。
 */
public class ServiceServer implements JSONable {

    public final String tag;

    public final String deployPath;

    public final String configPath;

    public final String celletsPath;

    private CellConfigFile cellConfigFile;

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
        this.cellConfigFile = new CellConfigFile(this.configPath + File.separator + "service.xml");
        this.cellConfigFile.refresh();


    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("tag", this.tag);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
