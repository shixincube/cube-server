/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    // Statistics version：统计口径版本
    public final String ITEM_SV = "SV";

    /**
     * 当前统计口径版本。
     *
     * 口径发生变化（例如调整用户判定规则、修改时长单位）时必须 <b>递增</b> 该值：
     * 每日统计会比对已入库的 {@link #ITEM_SV} 值，不一致时删除该日旧口径数据并重新统计，
     * 否则历史日期会一直保留旧口径的结果。
     */
    public static final int STATISTICS_VERSION = 2;

    private final String contactEventTablePrefix = "contact_event_log_";

    private final String contactStatisticsTablePrefix = "contact_statistics_";

    /**
     * 联系人表名前缀，与 {@code ContactStorage} 保持一致。
     *
     * 统计存储器与联系人存储器使用同一份配置，因此可以直接查询联系人表。
     */
    private final String contactTablePrefix = "contact_";

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

    /**
     * 每日统计的天数（含昨日）。
     *
     * 统计口径升级后需要回补最近的日期，否则「昨日与前一日的差值」类指标（例如 DNU）
     * 会在口径切换当天出现跳变。已按当前口径统计过的日期会被直接跳过，因此多检查几天无额外开销。
     */
    private static final int COLLECT_DAYS = 3;

    private void collect() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 1; i <= COLLECT_DAYS; ++i) {
            Calendar day = (Calendar) cal.clone();
            day.add(Calendar.DAY_OF_MONTH, -i);
            this.collectDay(day);
        }
    }

    private void collectDay(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int date = cal.get(Calendar.DATE);

        long beginning = cal.getTimeInMillis();
        long ending = beginning + (24L * 60 * 60 * 1000L);

        for (String domain : this.domainNameList) {
            String statisticTable = this.statisticsTableNameMap.get(domain);

            // 查询当日已有记录，用于判断是否需要统计
            List<StorageField[]> existRows = this.storage.executeQuery(statisticTable, new StorageField[] {
                    new StorageField("item", LiteralBase.STRING),
                    new StorageField("data", LiteralBase.STRING)
            }, new Conditional[] {
                    Conditional.createEqualTo("year", LiteralBase.INT, year),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("month", LiteralBase.INT, month),
                    Conditional.createAnd(),
                    Conditional.createEqualTo("date", LiteralBase.INT, date)
            });

            boolean collected = false;
            for (StorageField[] row : existRows) {
                if (ITEM_SV.equals(row[0].getString())) {
                    collected = (STATISTICS_VERSION == toStatisticsVersion(row[1].getString()));
                    break;
                }
            }

            if (collected) {
                // 已按当前统计口径统计过，不重复统计
                continue;
            }

            if (!existRows.isEmpty()) {
                // 统计口径已升级，清除当日旧口径数据后重新统计
                this.storage.execute("DELETE FROM " + statisticTable
                        + " WHERE `year`=" + year + " AND `month`=" + month + " AND `date`=" + date);
                Logger.i(this.getClass(), "Re-collect statistics of domain '" + domain
                        + "' for " + year + "-" + month + "-" + date
                        + " (version " + STATISTICS_VERSION + ")");
            }

            // 日用户总数：仅统计 ID 位数大于等于 8 位的用户，位数小于 8 位的是 AIGC 工作单元
            int total = this.countUserContacts(domain);
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

            // 平均在线时长（单位：小时）
            double aot = this.calcAOT(domain, beginning, ending);
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

            // 记录本日数据使用的统计口径版本
            this.storage.executeInsert(statisticTable, new StorageField[] {
                    new StorageField("item", LiteralBase.STRING, ITEM_SV),
                    new StorageField("data", LiteralBase.STRING, String.valueOf(STATISTICS_VERSION)),
                    new StorageField("year", LiteralBase.INT, year),
                    new StorageField("month", LiteralBase.INT, month),
                    new StorageField("date", LiteralBase.INT, date),
                    new StorageField("timestamp", LiteralBase.LONG, System.currentTimeMillis())
            });
        }
    }

    /**
     * 统计指定域里的用户总数。
     *
     * 仅统计 ID 十进制位数大于等于 8 位的联系人，位数小于 8 位的是 AIGC 工作单元节点。
     *
     * @param domain 指定域。
     * @return 返回用户总数。
     */
    private int countUserContacts(String domain) {
        String table = SQLUtils.correctTableName(this.contactTablePrefix + domain);

        StringBuilder sql = new StringBuilder("SELECT COUNT(`id`) FROM ");
        sql.append(table);
        sql.append(" WHERE `id`>=").append(Contact.MIN_USER_CONTACT_ID);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].isNullValue() ? 0 : result.get(0)[0].getInt();
    }

    /**
     * 解析统计口径版本字符串。
     *
     * @param value 已入库的版本字符串。
     * @return 返回版本号，无法解析时返回 0 。
     */
    private static int toStatisticsVersion(String value) {
        if (null == value) {
            return 0;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int calcDAU(String domain, long beginning, long ending) {
        String eventTable = this.eventTableNameMap.get(domain);

        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT `contact_id`) FROM ");
        sql.append(eventTable);
        sql.append(" WHERE `event`='SignIn'");
        // 仅统计用户，排除 AIGC 工作单元
        sql.append(" AND `contact_id`>=").append(Contact.MIN_USER_CONTACT_ID);
        sql.append(" AND `time`>=").append(beginning);
        sql.append(" AND `time`<").append(ending);

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return 0;
        }

        return result.get(0)[0].getInt();
    }

    /**
     * 计算指定时间区间内的用户平均在线时长。
     *
     * @param domain 指定域。
     * @param beginning 起始时间戳。
     * @param ending 结束时间戳。
     * @return 返回平均在线时长，单位：<b>小时</b>，保留两位小数。
     */
    private double calcAOT(String domain, long beginning, long ending) {
        String eventTable = this.eventTableNameMap.get(domain);

        // 查询所有登录的用户 ID
        StringBuilder sql = new StringBuilder("SELECT DISTINCT `contact_id` FROM ");
        sql.append(eventTable);
        sql.append(" WHERE `event`='SignIn'");
        // 仅统计用户，排除 AIGC 工作单元
        sql.append(" AND `contact_id`>=").append(Contact.MIN_USER_CONTACT_ID);
        sql.append(" AND `time`>=").append(beginning);
        sql.append(" AND `time`<").append(ending);

        List<StorageField[]> contactIdList = this.storage.executeQuery(sql.toString());

        if (contactIdList.isEmpty()) {
            return 0.0;
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
                    if (startTime > 0) {
                        duration += (time - startTime) - 15000L;
                    }
                    startTime = 0;
                } else if (name.equals(ContactHook.SignOut)) {
                    if (startTime > 0) {
                        duration += time - startTime;
                    }
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
            return 0.0;
        }

        double value = (double) total / (double) durationMap.size();

        // 毫秒换算为小时，保留两位小数
        double hours = value / (60.0 * 60.0 * 1000.0);
        return Math.round(hours * 100) / 100.0;
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
            // 仅统计用户，排除 AIGC 工作单元
            sql.append(" AND `contact_id`>=").append(Contact.MIN_USER_CONTACT_ID);
            sql.append(" AND `time`>=").append(beginningTime);
            sql.append(" AND `time`<").append(endingTime);

            List<StorageField[]> result = this.storage.executeQuery(sql.toString());
            if (result.isEmpty()) {
                // 当前时间段没有数据
                continue;
            }

            TimeSlice timeSlice = new TimeSlice(i, beginningTime, endingTime);
            int numContacts = 0;

            for (StorageField[] row : result) {
                Map<String, StorageField> map = StorageFields.get(row);
                String jsonString = map.get("event_data").getString();
                JSONObject json = new JSONObject(jsonString);

                // 读取联系人信息
                JSONObject contactJson = json.getJSONObject("contact");
                contactJson.remove("context");      // 删除 context 数据

                // 二次校验：跳过 AIGC 工作单元
                if (!Contact.isUserContactId(contactJson.optLong("id", 0L))) {
                    continue;
                }

                Contact contact = new Contact(contactJson);

                // 读取设备信息
                Device device = new Device(json.getJSONObject("device"));

                timeSlice.addContact(contact, device);
                ++numContacts;
            }

            if (numContacts > 0) {
                list.add(timeSlice);
            }
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
