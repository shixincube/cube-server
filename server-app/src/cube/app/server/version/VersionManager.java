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
