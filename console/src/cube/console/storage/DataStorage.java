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
