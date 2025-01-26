/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.console.storage.DataStorage;
import cube.console.tool.DeployTool;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

/**
 * 统计数据管理器。
 */
public class StatisticDataManager {

    // Total number of users
    public final String ITEM_TNU = "TNU";

    // Daily Active User
    public final String ITEM_DAU = "DAU";

    // Average online time
    public final String ITEM_AOT = "AOT";

    // Time Distribution
    public final String ITEM_TD = "TD";

    private DataStorage storage;

    public StatisticDataManager() {
    }

    public void start() {
        String filepath = null;
        for (String path : DeployTool.CONSOLE_PROP_FILES) {
            File file = new File(path);
            if (file.exists()) {
                filepath = path;
                break;
            }
        }

        try {
            Properties properties = ConfigUtils.readProperties(filepath);
            this.storage = new DataStorage(properties);
            this.storage.open();
        } catch (IOException e) {
            Logger.w(this.getClass(), "#start", e);
        }
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }
    }

    public JSONObject queryStatisticData(String domain, Calendar calendar) {
        JSONObject data = this.storage.queryContactStatistics(domain, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
        return data;
    }

    public JSONObject queryStatisticData(String domain, int year, int month, int date) {
        JSONObject data = this.storage.queryContactStatistics(domain, year, month, date);
        return data;
    }

    public List<String> getDomains() {
        return this.storage.queryAllDomains();
    }
}
