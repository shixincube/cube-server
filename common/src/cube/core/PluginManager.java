/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cell.util.log.Logger;
import cube.plugin.Plugin;
import cube.plugin.PluginSystem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件管理器。
 */
public class PluginManager {

    private Kernel kernel;

    private JSONObject config;

    private volatile boolean started = false;

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
                        if (desc.isValid()) {
                            list.add(desc);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // 注册插件
            for (PluginDesc desc : list) {
                Module module = this.kernel.getModule(desc.module);
                PluginSystem ps = module.getPluginSystem();
                if (null == ps) {
                    continue;
                }

                for (Map.Entry<String, Plugin> e : desc.pluginMap.entrySet()) {
                    ps.register(e.getKey(), e.getValue());
                    Logger.i(this.getClass(), "Register plugin : #" + desc.module + " [" + e.getKey() + "] - " + e.getValue().getClass().getName());
                }
            }

            list.clear();

            this.started = true;
        }
    }

    public void stop() {
        if (!this.started) {
            return;
        }

        this.started = false;

        for (AbstractModule module : this.kernel.getModules()) {
            PluginSystem pluginSystem = module.getPluginSystem();
            if (null == pluginSystem) {
                // 跳过没有插件系统的模块
                continue;
            }

            List<Plugin> plugins = pluginSystem.getPlugins();
            for (Plugin plugin : plugins) {
                plugin.teardown();
            }
        }
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

    /**
     * 插件信息描述。
     */
    protected class PluginDesc {

        protected String module = null;

        protected Map<String, Plugin> pluginMap;

        protected PluginDesc(JSONObject json) {
            try {
                String filepath = json.getString("file");
                File file = new File(filepath);
                if (file.exists()) {
                    // 读取数据
                    this.module = json.getString("module");
                    this.pluginMap = new HashMap<>();

                    URLClassLoader loader = new URLClassLoader(new URL[]{ file.toURI().toURL() });

                    // 读取插件列表
                    JSONArray list = json.getJSONArray("plugins");
                    for (int i = 0, len = list.length(); i < len; ++i) {
                        JSONObject pluginJson = list.getJSONObject(i);
                        String hook = pluginJson.getString("hook");
                        String className = pluginJson.getString("class");

                        try {
                            Class clazz = loader.loadClass(className);
                            Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
                            this.pluginMap.put(hook, plugin);
                        } catch (ClassNotFoundException e) {
                            Logger.w(this.getClass(), "#PluginDesc", e);
                            continue;
                        } catch (IllegalAccessException e) {
                            Logger.w(this.getClass(), "#PluginDesc", e);
                            continue;
                        } catch (InstantiationException e) {
                            Logger.w(this.getClass(), "#PluginDesc", e);
                            continue;
                        } catch (Exception e) {
                            Logger.w(this.getClass(), "#PluginDesc", e);
                            continue;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        protected boolean isValid() {
            return (null != this.module);
        }
    }
}
