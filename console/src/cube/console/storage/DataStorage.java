/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.storage;

import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 联系人数据存储器。
 */
public class DataStorage {

    // Total number of users
    public final String ITEM_TNU = "TNU";

    // Daily Active User
    public final String ITEM_DAU = "DAU";

    // Average online time
    public final String ITEM_AOT = "AOT";

    // Time Distribution
    public final String ITEM_TD = "TD";

    private final String contactStatisticsTablePrefix = "contact_statistics_";

    private Storage authStorage;

    private Storage statisticStorage;

    public DataStorage(Properties properties) {
        JSONObject config = new JSONObject();
        config.put(StorageFactory.MYSQL_HOST, properties.getProperty("auth.mysql.host"));
        config.put(StorageFactory.MYSQL_PORT, properties.getProperty("auth.mysql.port"));
        config.put(StorageFactory.MYSQL_SCHEMA, properties.getProperty("auth.mysql.schema"));
        config.put(StorageFactory.MYSQL_USER, properties.getProperty("auth.mysql.user"));
        config.put(StorageFactory.MYSQL_PASSWORD, properties.getProperty("auth.mysql.password"));
        this.authStorage = StorageFactory.getInstance().createStorage(StorageType.MySQL, "authData", config);

        config = new JSONObject();
        config.put(StorageFactory.MYSQL_HOST, properties.getProperty("statistic.mysql.host"));
        config.put(StorageFactory.MYSQL_PORT, properties.getProperty("statistic.mysql.port"));
        config.put(StorageFactory.MYSQL_SCHEMA, properties.getProperty("statistic.mysql.schema"));
        config.put(StorageFactory.MYSQL_USER, properties.getProperty("statistic.mysql.user"));
        config.put(StorageFactory.MYSQL_PASSWORD, properties.getProperty("statistic.mysql.password"));
        this.statisticStorage = StorageFactory.getInstance().createStorage(StorageType.MySQL, "StatisticData", config);
    }

    public void open() {
        this.authStorage.open();
        this.statisticStorage.open();
    }

    public void close() {
        this.authStorage.close();
        this.statisticStorage.close();
    }

    public List<String> queryAllDomains() {
        ArrayList<String> list = new ArrayList<>();

        List<StorageField[]> result = this.authStorage.executeQuery("SELECT DISTINCT `domain` FROM `auth_domain`");

        for (StorageField[] row : result) {
            list.add(row[0].getString());
        }

        return list;
    }

    public JSONObject queryContactStatistics(String domain, int year, int month, int date) {
        String table = this.contactStatisticsTablePrefix + domain;
        table = SQLUtils.correctTableName(table);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT `item`,`data` FROM ").append(table);
        sql.append(" WHERE `year`=").append(year);
        sql.append(" AND `month`=").append(month);
        sql.append(" AND `date`=").append(date);

        List<StorageField[]> result = this.statisticStorage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return null;
        }

        JSONObject json = new JSONObject();

        for (StorageField[] row : result) {
            String item = row[0].getString();
            String data = row[1].getString();

            if (item.equals(ITEM_TNU)) {
                json.put(item, Integer.parseInt(data));
            }
            else if (item.equals(ITEM_DAU)) {
                json.put(item, Integer.parseInt(data));
            }
            else if (item.equals(ITEM_AOT)) {
                json.put(item, Long.parseLong(data));
            }
            else if (item.equals(ITEM_TD)) {
                json.put(item, new JSONArray(data));
            }
        }

        return json;
    }
}
