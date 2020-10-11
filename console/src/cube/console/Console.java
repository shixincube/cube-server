/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.console;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.util.ConfigUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * 控制台数据管理类。
 */
public final class Console {

    private Properties servers;

    public Console() {
    }

    public void launch() {
        try {
            this.servers = ConfigUtils.readConsoleServers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getDispatcherServers() {
        if (null == this.servers) {
            return null;
        }

        JSONArray array = new JSONArray();

        int num = Integer.parseInt(this.servers.getProperty("dispatcher.num"));
        for (int i = 1; i <= num; ++i) {
            String address = this.servers.getProperty("dispatcher." + i + ".address");
            int port = Integer.parseInt(this.servers.getProperty("dispatcher." + i + ".port"));
            JSONObject json = new JSONObject();
            try {
                json.put("address", address);
                json.put("port", port);
                array.put(json);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return array;
    }

    public JSONArray getServiceServers() {
        if (null == this.servers) {
            return null;
        }

        JSONArray array = new JSONArray();

        int num = Integer.parseInt(this.servers.getProperty("service.num"));
        for (int i = 1; i <= num; ++i) {
            String address = this.servers.getProperty("service." + i + ".address");
            int port = Integer.parseInt(this.servers.getProperty("service." + i + ".port"));
            JSONObject json = new JSONObject();
            try {
                json.put("address", address);
                json.put("port", port);
                array.put(json);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return array;
    }
}
