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

package cube.service.contact;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.TimeSlice;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.SQLUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * 统计系统。
 */
public final class StatisticsSystem {

    // Total number of users
    public final String ITEM_TNU = "TNU";

    // Daily Active User
    public final String ITEM_DAU = "DAU";

    // Average online time
    public final String ITEM_AOT = "AOT";

    // Time Distribution
    public final String ITEM_TD = "TD";

    private final String contactEventTablePrefix = "contact_event_log_";

    private final String contactStatisticsTablePrefix = "contact_statistics_";

    /**
     * 事件表。
     */
    private final StorageField[] eventTableFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 事件名
            new StorageField("event", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 联系人 ID
            new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 事件时间戳
            new StorageField("time", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 事件数据
            new StorageField("event_data", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            })
    };

    /**
     * 统计表。
     */
    private final StorageField[] statisticsTableFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 统计项目
            new StorageField("item", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 数据
            new StorageField("data", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 统计时间记录：年
            new StorageField("year", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 统计时间记录：月
            new StorageField("month", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 统计时间记录：日
            new StorageField("date", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            // 统计时间记录：时
            new StorageField("hour", LiteralBase.INT, new Constraint[] {
                    Constraint.DEFAULT_0
            }),
            // 入库时间
            new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private ExecutorService executor;

    private Storage storage;

    private Timer timer;

    private Map<String, String> eventTableNameMap;

    private Map<String, String> statisticsTableNameMap;

    private List<String> domainNameList;

    public StatisticsSystem(ExecutorService executor) {
        this.executor = executor;
        this.eventTableNameMap = new HashMap<>();
        this.statisticsTableNameMap = new HashMap<>();
    }

    public void start(StorageType type, JSONObject storageConfig, List<String> domainList) {
        this.domainNameList = domainList;

        // 存储器
        this.storage = StorageFactory.getInstance().createStorage(type, "Statistics", storageConfig);
        this.storage.open();

        PluginSystem pluginSystem = ContactManager.getInstance().getPluginSystem();
        pluginSystem.register(ContactHook.SignIn, new SignInPlugin());
        pluginSystem.register(ContactHook.SignOut, new SignOutPlugin());
        pluginSystem.register(ContactHook.DeviceTimeout, new DeviceTimeoutPlugin());
        pluginSystem.register(ContactHook.Comeback, new ComebackPlugin());

        this.execSelfChecking(domainList);

        // 每天凌晨 2 点执行，随机时长 5 分钟
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 2);
        cal.set(Calendar.MINUTE, Utils.randomInt(0, 4));
        cal.set(Calendar.SECOND, Utils.randomInt(0, 59));
        Date date = cal.getTime();
        if (date.before(new Date())) {
            // 执行时间已过，调整至一天后
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();
        }

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collect();
            }
        }, date, 24L * 60 * 60 * 1000);

        // 启动时尝试统计数据
        (new Thread() {
            @Override
            public void run() {
                collect();
            }
        }).start();
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    private void execSelfChecking(List<String> domainNameList) {
        for (String domain : domainNameList) {
            String table = this.contactEventTablePrefix + domain;
            table = SQLUtils.correctTableName(table);
            this.eventTableNameMap.put(domain, table);

            if (!this.storage.exist(table)) {
                if (this.storage.executeCreate(table, this.eventTableFields)) {
                    Logger.i(this.getClass(), "Created table '" + table + "' successfully");
                }
            }
        }

        for (String domain : domainNameList) {
            String table = this.contactStatisticsTablePrefix + domain;

            table = SQLUtils.correctTableName(table);
            this.statisticsTableNameMap.put(domain, table);

            if (!this.storage.exist(table)) {
                if (this.storage.executeCreate(table, this.statisticsTableFields)) {
                    Logger.i(this.getClass(), "Created table '" + table + "' successfully");
                }
            }
        }
    }

    private void collect() {
        // 统计昨天的数据
        Calendar cal = Calendar.getInstance();
        // 昨天
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) - 1);

        // 时间复位
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int date = cal.get(Calendar.DATE);

        long beginning = cal.getTimeInMillis();
        long ending = beginning + (24L * 60 * 60 * 1000L);

        for (String domain : this.domainNameList) {
            // 查询是否有记录
            String statisticTable = this.statisticsTableNameMap.get(domain);

            List<StorageField[]> result = this.storage.executeQuery(statisticTable, new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG)
            }, new Conditional[] {
                    Conditional.createEqualTo("item", LiteralBase.STRING, ITEM_TNU),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("year", LiteralBase.INT, year),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("month", LiteralBase.INT, month),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("date", LiteralBase.INT, date)
            });

            if (!result.isEmpty()) {
                // 有数据，不统计
                continue;
            }

            // 日用户总数
            int total = ContactManager.getInstance().countContacts(domain);
            this.storage.executeInsert(statisticTable, new StorageField[] {
                    new StorageField("item", LiteralBase.STRING, ITEM_TNU),
                    new StorageField("data", LiteralBase.STRING, String.valueOf(total)),
                    new StorageField("year", LiteralBase.INT, year),
                    new StorageField("month", LiteralBase.INT, month),
                    new StorageField("date", LiteralBase.INT, date),
                    new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
            });

            // 计算 DAU
            int dau = this.calcDAU(domain, beginning, ending);
            this.storage.executeInsert(statisticTable, new StorageField[] {
                    new StorageField("item", LiteralBase.STRING, ITEM_DAU),
                    new StorageField("data", LiteralBase.STRING, String.valueOf(dau)),
                    new StorageField("year", LiteralBase.INT, year),
                    new StorageField("month", LiteralBase.INT, month),
                    new StorageField("date", LiteralBase.INT, date),
                    new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
            });

            // 平均在线时长
            long aot = this.calcAOT(domain, beginning, ending);
            this.storage.executeInsert(statisticTable, new StorageField[] {
                    new StorageField("item", LiteralBase.STRING, ITEM_AOT),
                    new StorageField("data", LiteralBase.STRING, String.valueOf(aot)),
                    new StorageField("year", LiteralBase.INT, year),
                    new StorageField("month", LiteralBase.INT, month),
                    new StorageField("date", LiteralBase.INT, date),
                    new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
            });

            // 按照时间段进行统计
            List<TimeSlice> timeSlices = this.calcTimeDistribution(domain, beginning, ending);
            JSONArray tdArray = new JSONArray();
            for (TimeSlice timeSlice : timeSlices) {
                tdArray.put(timeSlice.toCompactJSON());
            }
            this.storage.executeInsert(statisticTable, new StorageField[] {
                    new StorageField("item", LiteralBase.STRING, ITEM_TD),
                    new StorageField("data", LiteralBase.STRING, tdArray.toString()),
                    new StorageField("year", LiteralBase.INT, year),
                    new StorageField("month", LiteralBase.INT, month),
                    new StorageField("date", LiteralBase.INT, date),
                    new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
            });
        }
    }

    private int calcDAU(String domain, long beginning, long ending) {
        String eventTable = this.eventTableNameMap.get(domain);

        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT `contact_id`) FROM ");
        sql.append(eventTable);
        sql.append(" WHERE `event`='SignIn'");
        sql.append(" AND `time`>=").append(beginning);
        sql.append(" AND `time`<").append(ending);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        return result.get(0)[0].getInt();
    }

    private long calcAOT(String domain, long beginning, long ending) {
        String eventTable = this.eventTableNameMap.get(domain);

        // 查询所有登录的用户 ID
        StringBuilder sql = new StringBuilder("SELECT DISTINCT `contact_id` FROM ");
        sql.append(eventTable);
        sql.append(" WHERE `event`='SignIn'");
        sql.append(" AND `time`>=").append(beginning);
        sql.append(" AND `time`<").append(ending);

        List<StorageField[]> contactIdList = this.storage.executeQuery(sql.toString());

        if (contactIdList.isEmpty()) {
            return 0;
        }

        long total = 0;
        HashMap<Long, Long> durationMap = new HashMap<>();

        for (StorageField[] row : contactIdList) {
            Long contactId = row[0].getLong();
            sql.setLength(0);

            sql.append("SELECT `event`,`time` FROM ").append(eventTable);
            sql.append(" WHERE `contact_id`=").append(contactId);
            sql.append(" AND `time`>=").append(beginning);
            sql.append(" AND `time`<").append(ending);
            sql.append(" ORDER BY `time`");

            long startTime = 0;
            long duration = 0;

            List<StorageField[]> result = this.storage.executeQuery(sql.toString());
            for (StorageField[] event : result) {
                String name = event[0].getString();
                long time = event[1].getLong();

                if (startTime == 0 &&
                        (name.equals(ContactHook.SignIn) || name.equals(ContactHook.Comeback))) {
                    startTime = time;
                    continue;
                }

                if (name.equals(ContactHook.SignIn) || name.equals(ContactHook.Comeback)) {
                    // 跳过 SignIn
                    continue;
                } else if (name.equals(ContactHook.DeviceTimeout)) {
                    duration += (time - startTime) - 15000L;
                    startTime = 0;
                } else if (name.equals(ContactHook.SignOut)) {
                    duration += time - startTime;
                    startTime = 0;
                }
            }

            if (duration == 0 && startTime != 0) {
                duration = ending - startTime;
            }

            durationMap.put(contactId, duration);
            // 累加总时长
            total += duration;
        }

        if (durationMap.isEmpty()) {
            return 0;
        }

        double value = (double) total / (double) durationMap.size();
        return Math.round(value);
    }

    private List<TimeSlice> calcTimeDistribution(String domain, long beginning, long ending) {
        ArrayList<TimeSlice> list = new ArrayList<>();

        String eventTable = this.eventTableNameMap.get(domain);

        StringBuilder sql = new StringBuilder();

        long beginningTime = 0;
        long endingTime = beginning;

        for (int i = 0; i < 24; ++i) {
            sql.setLength(0);

            beginningTime = endingTime;
            endingTime = beginningTime + (60 * 60 * 1000);

            sql.append("SELECT * FROM ").append(eventTable);
            sql.append(" WHERE `event`='SignIn'");
            sql.append(" AND `time`>=").append(beginningTime);
            sql.append(" AND `time`<").append(endingTime);

            List<StorageField[]> result = this.storage.executeQuery(sql.toString());
            if (result.isEmpty()) {
                // 当前时间段没有数据
                continue;
            }

            TimeSlice timeSlice = new TimeSlice(i, beginningTime, endingTime);

            for (StorageField[] row : result) {
                Map<String, StorageField> map = StorageFields.get(row);
                String jsonString = map.get("event_data").getString();
                JSONObject json = new JSONObject(jsonString);

                // 读取联系人信息
                JSONObject contactJson = json.getJSONObject("contact");
                contactJson.remove("context");      // 删除 context 数据
                Contact contact = new Contact(contactJson);

                // 读取设备信息
                Device device = new Device(json.getJSONObject("device"));

                timeSlice.addContact(contact, device);
            }

            list.add(timeSlice);
        }

        return list;
    }

    protected class SignInPlugin implements Plugin {

        protected SignInPlugin() {
        }

        @Override
        public void setup() {
        }

        @Override
        public void teardown() {
        }

        @Override
        public HookResult launch(PluginContext context) {
            final ContactPluginContext ctx = (ContactPluginContext) context;

            final Contact contact = ctx.getContact();
            final long time = System.currentTimeMillis();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String domain = contact.getDomain().getName();
                    String table = eventTableNameMap.get(domain);

                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("event", LiteralBase.STRING, ctx.getKey()),
                            new StorageField("contact_id", LiteralBase.LONG, contact.getId()),
                            new StorageField("time", LiteralBase.LONG, time),
                            new StorageField("event_data", LiteralBase.STRING, ctx.toCompactJSON()),
                    });
                }
            });

            return null;
        }
    }

    protected class SignOutPlugin implements Plugin {

        protected SignOutPlugin() {
        }

        @Override
        public void setup() {
        }

        @Override
        public void teardown() {
        }

        @Override
        public HookResult launch(PluginContext context) {
            final ContactPluginContext ctx = (ContactPluginContext) context;

            final Contact contact = ctx.getContact();
            final long time = System.currentTimeMillis();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String domain = contact.getDomain().getName();
                    String table = eventTableNameMap.get(domain);

                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("event", LiteralBase.STRING, ctx.getKey()),
                            new StorageField("contact_id", LiteralBase.LONG, contact.getId()),
                            new StorageField("time", LiteralBase.LONG, time),
                            new StorageField("event_data", LiteralBase.STRING, ctx.toCompactJSON()),
                    });
                }
            });

            return null;
        }
    }

    protected class DeviceTimeoutPlugin implements Plugin {

        protected DeviceTimeoutPlugin() {
        }

        @Override
        public void setup() {
        }

        @Override
        public void teardown() {
        }

        @Override
        public HookResult launch(PluginContext context) {
            final ContactPluginContext ctx = (ContactPluginContext) context;

            final Contact contact = ctx.getContact();
            final long time = System.currentTimeMillis();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String domain = contact.getDomain().getName();
                    String table = eventTableNameMap.get(domain);

                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("event", LiteralBase.STRING, ctx.getKey()),
                            new StorageField("contact_id", LiteralBase.LONG, contact.getId()),
                            new StorageField("time", LiteralBase.LONG, time),
                            new StorageField("event_data", LiteralBase.STRING, ctx.toCompactJSON()),
                    });
                }
            });

            return null;
        }
    }

    protected class ComebackPlugin implements Plugin {

        protected ComebackPlugin() {
        }

        @Override
        public void setup() {
        }

        @Override
        public void teardown() {
        }

        @Override
        public HookResult launch(PluginContext context) {
            final ContactPluginContext ctx = (ContactPluginContext) context;

            final Contact contact = ctx.getContact();
            final long time = System.currentTimeMillis();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String domain = contact.getDomain().getName();
                    String table = eventTableNameMap.get(domain);

                    storage.executeInsert(table, new StorageField[] {
                            new StorageField("event", LiteralBase.STRING, ctx.getKey()),
                            new StorageField("contact_id", LiteralBase.LONG, contact.getId()),
                            new StorageField("time", LiteralBase.LONG, time),
                            new StorageField("event_data", LiteralBase.STRING, ctx.toCompactJSON()),
                    });
                }
            });

            return null;
        }
    }
}
