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

package cube.core;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件管理器。
 */
public class PluginManager {

    private Kernel kernel;

    private JSONObject config;

    public PluginManager(Kernel kernel) {
        this.kernel = kernel;
    }

    public void start() {
        this.config = this.readConfig();
        if (null != this.config) {
            List<PluginDesc> list = new ArrayList<>();

            if (this.config.has("deploy")) {
                try {
                    JSONArray array = this.config.getJSONArray("deploy");
                    for (int i = 0, len = array.length(); i < len; ++i) {
                        JSONObject plugin = array.getJSONObject(i);
                        PluginDesc desc = new PluginDesc(plugin);
                        list.add(desc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // 注册插件
            for (PluginDesc desc : list) {
                Module module = this.kernel.getModule(desc.module);

            }
        }
    }

    public void stop() {

    }

    private JSONObject readConfig() {
        File file = new File("config/plugin.json");
        if (!file.exists()) {
            file = new File("plugin.json");
            if (!file.exists()) {
                return null;
            }
        }

        JSONObject json = null;

        StringBuilder buf = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            json = new JSONObject(buf.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    private void loadJar(String filepath) {
        File file = new File(filepath);
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{ file.toURI().toURL() });

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 插件信息描述。
     */
    protected class PluginDesc {

        protected String module;

        protected PluginDesc(JSONObject json) {
            try {
                String filepath = json.getString("file");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        protected Plugin[] load() {
            return null;
        }
    }
}
