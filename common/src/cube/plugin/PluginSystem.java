/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.plugin;

import cell.util.log.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件系统。
 *
 * @param <T>
 */
public class PluginSystem<T extends Hook> {

    /**
     * 是否使用 Lua 支持。
     */
    public static boolean useLua = false;

    /**
     * 钩子列表。
     */
    private ConcurrentHashMap<String, Hook> hooks;

    /**
     * 插件列表。
     */
    private ConcurrentHashMap<String, List<Plugin>> plugins;

    public PluginSystem() {
        this.hooks = new ConcurrentHashMap<>();
        this.plugins = new ConcurrentHashMap<>();
    }

    /**
     * 加载插件系统。
     */
    public static void load() {
        Logger.i("PluginSystem", "Start plugin system");

        if (PluginSystem.useLua) {
            try {
                System.loadLibrary("luajava-1.1");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Native code library failed to load.\n" + e);
                Logger.e("PluginSystem", "Native code library failed to load.", e);
            }
        }
    }

    /**
     * 卸载插件系统。
     */
    public static void unlaod() {
        Logger.i("PluginSystem", "Stop plugin system");
    }

    /**
     * 添加 Hook 。
     *
     * @param hook
     */
    public void addHook(T hook) {
        this.hooks.put(hook.getKey(), hook);
        hook.system = this;
    }

    /**
     * 移除 Hook 。
     *
     * @param hook
     */
    public void removeHook(T hook) {
        this.hooks.remove(hook.getKey());
        hook.system = null;
    }

    /**
     * 返回指定 Hook 。
     *
     * @param key
     * @return
     */
    public T getHook(String key) {
        return (T) this.hooks.get(key);
    }

    /**
     * 注册指定键的插件。
     *
     * @param key
     * @param plugin
     */
    public void register(String key, Plugin plugin) {
        List<Plugin> list = this.plugins.get(key);
        if (null == list) {
            list = new Vector<>();
            this.plugins.put(key, list);
        }

        if (list.contains(plugin)) {
            return;
        }

        (new Thread() {
            @Override
            public void run() {
                // 调用 setup 进行准备
                plugin.setup();
            }
        }).start();

        list.add(plugin);
    }

    /**
     * 注册指定键的插件。
     *
     * @param key
     * @param className
     */
    public void register(String key, String className) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> clazz = loader.loadClass(className);
            Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();

            Logger.i(this.getClass(), "New & register plugin : " + className);

            this.register(key, plugin);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注销指定插件。
     *
     * @param key
     * @param plugin
     */
    public void deregister(String key, Plugin plugin) {
        List<Plugin> list = this.plugins.get(key);
        if (null == list) {
            return;
        }

        if (list.remove(plugin)) {
            (new Thread() {
                @Override
                public void run() {
                    plugin.teardown();
                }
            }).start();
        }
    }

    protected HookResult apply(String key, PluginContext context) {
        HookResult result = new HookResult();

        List<Plugin> list = this.plugins.get(key);
        if (null == list) {
            return result;
        }

        for (Plugin plugin : list) {
            HookResult hr = plugin.launch(context);
            if (null != hr) {
                result.add(hr);
            }
        }
        return result;
    }

    public List<Plugin> getPlugins() {
        List<Plugin> list = new ArrayList<>();
        for (List<Plugin> plugins : this.plugins.values()) {
            list.addAll(plugins);
        }
        return list;
    }

    /**
     * 从配置文件加载插件。
     *
     * @param configFilename
     */
    /*public void loadPlugin(String configFilename) {
        // 读取配置文件
        JSONObject config = this.readConfig(configFilename);
        if (null == config) {
            Logger.w(this.getClass(), "Load plugin config file failed: " + configFilename);
            return;
        }

        try {
            // 进行配置
            if (config.has("plugins")) {
                JSONArray array = config.getJSONArray("plugins");
                for (int i = 0, size = array.length(); i < size; ++i) {
                    JSONObject cfg = array.getJSONObject(i);
                    this.register(cfg.getString("key"), cfg.getString("class"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }*/

    /*private JSONObject readConfig(String pathname) {
        File file = new File(pathname);
        if (!file.exists()) {
            return null;
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
    }*/
}
