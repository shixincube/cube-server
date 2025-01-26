/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
