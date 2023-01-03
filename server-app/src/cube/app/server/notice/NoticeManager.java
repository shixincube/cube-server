/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.app.server.notice;

import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 通知管理器。
 */
public class NoticeManager {

    public final static String GLOBAL_DOMAIN = "*";

    private final static NoticeManager instance = new NoticeManager();

    private NoticeStorage storage;

    private NoticeManager() {
    }

    public static NoticeManager getInstance() {
        return NoticeManager.instance;
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

    public List<Notice> getNotices(String domainName) {
        List<Notice> result = new ArrayList<>();
        List<Notice> list = this.storage.readNotices(domainName);
        result.addAll(list);
        list = this.storage.readNotices(GLOBAL_DOMAIN);
        result.addAll(list);
        return result;
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
                this.storage = new NoticeStorage(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
