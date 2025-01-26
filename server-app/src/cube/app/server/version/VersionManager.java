/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.version;

import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * 版本管理器。
 */
public class VersionManager {

    private final static VersionManager instance = new VersionManager();

    private VersionStorage storage;

    private VersionManager() {
    }

    public static VersionManager getInstance() {
        return VersionManager.instance;
    }

    public void start() {
        this.loadConfig();

        (new Thread() {
            @Override
            public void run() {
                if (null != storage) {
                    storage.open();
                }
            }
        }).start();
    }

    public void destroy() {
        if (null != this.storage) {
            this.storage.close();
        }
    }

    public AppVersion getVersion(String device) {
        return this.storage.getVersion(device);
    }

    private void loadConfig() {
        String[] configFiles = new String[] {
                "server_dev.properties",
                "server.properties"
        };

        String configFile = null;
        for (String filename : configFiles) {
            File file = new File(filename);
            if (file.exists()) {
                configFile = filename;
                break;
            }
        }

        if (null != configFile) {
            try {
                Properties properties = ConfigUtils.readProperties(configFile);
                this.storage = new VersionStorage(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
