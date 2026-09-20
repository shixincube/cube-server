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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    /** 工作单元在线判定窗口：最近 24 小时内有活动即视为在线。 */
    private static final long UNIT_ONLINE_WINDOW = 24L * 60 * 60 * 1000L;

    /** 工作单元活跃回溯窗口：用于计算最近活动时间与近期活跃天数。 */
    private static final long UNIT_TRAIL_WINDOW = 30L * 24 * 60 * 60 * 1000L;

    /** 工作状态：在线。 */
    public static final String UNIT_STATE_ONLINE = "online";

    /** 工作状态：离线（回溯窗口内有活动，但当前不在线）。 */
    public static final String UNIT_STATE_OFFLINE = "offline";

    /** 工作状态：未激活（回溯窗口内没有任何活动）。 */
    public static final String UNIT_STATE_INACTIVE = "inactive";

    // 联系人活动事件名，与 service 侧 ContactHook 的取值保持一致
    private static final String EVENT_SIGN_IN = "SignIn";
    private static final String EVENT_SIGN_OUT = "SignOut";
    private static final String EVENT_DEVICE_TIMEOUT = "DeviceTimeout";
    private static final String EVENT_COMEBACK = "Comeback";

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

    /**
     * 查询指定域的 AIGC 工作单元概览。
     *
     * 工作单元是 ID 十进制位数大于等于 6 且小于 8 位的联系人（例如 <code>Unit-531001</code>），
     * 位数小于 6 位的联系人不是统计对象；「联系人（触点）概览」只统计 ID 位数大于等于 8 位的联系人。
     *
     * @param domain 指定域。
     * @param calendar 统计日期，按该日 00:00 ~ 24:00 统计日活动。
     * @return 返回单元概览数据。
     */
    public JSONObject queryUnitOverview(String domain, Calendar calendar) {
        if (null == this.storage) {
            return null;
        }

        Calendar day = (Calendar) calendar.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);

        long beginning = day.getTimeInMillis();
        long ending = beginning + 24L * 60 * 60 * 1000L;
        long now = System.currentTimeMillis();

        // 单元基础信息（ID 位数大于等于 6 且小于 8 位的联系人）
        List<JSONObject> contacts = this.storage.queryUnitContacts(domain);

        // 统计日内的事件
        List<DataStorage.ContactEvent> dayEvents =
                this.storage.queryContactEvents(domain, beginning, ending, true);
        // 回溯窗口内的事件，用于计算最近活动时间与工作状态
        List<DataStorage.ContactEvent> trailEvents =
                this.storage.queryContactEvents(domain, now - UNIT_TRAIL_WINDOW, now + 1000L, true);

        // 按单元归集统计日事件
        Map<Long, List<DataStorage.ContactEvent>> dayEventMap = new HashMap<>();
        for (DataStorage.ContactEvent event : dayEvents) {
            List<DataStorage.ContactEvent> list = dayEventMap.get(event.contactId);
            if (null == list) {
                list = new ArrayList<>();
                dayEventMap.put(event.contactId, list);
            }
            list.add(event);
        }

        // 回溯窗口内的最近活动时间与活跃天数
        Map<Long, Long> lastActiveMap = new HashMap<>();
        Map<Long, Set<Integer>> activeDayMap = new HashMap<>();
        for (DataStorage.ContactEvent event : trailEvents) {
            Long last = lastActiveMap.get(event.contactId);
            if (null == last || event.time > last) {
                lastActiveMap.put(event.contactId, event.time);
            }

            Set<Integer> days = activeDayMap.get(event.contactId);
            if (null == days) {
                days = new HashSet<>();
                activeDayMap.put(event.contactId, days);
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.time);
            days.add(cal.get(Calendar.DAY_OF_YEAR));
        }

        // 统计日各小时活跃单元数
        Map<Integer, Set<Long>> sliceMap = new HashMap<>();
        for (DataStorage.ContactEvent event : dayEvents) {
            int slice = (int) ((event.time - beginning) / (60L * 60 * 1000L));
            Set<Long> units = sliceMap.get(slice);
            if (null == units) {
                units = new HashSet<>();
                sliceMap.put(slice, units);
            }
            units.add(event.contactId);
        }

        JSONArray timeline = new JSONArray();
        JSONObject peak = null;
        for (int i = 0; i < 24; ++i) {
            Set<Long> units = sliceMap.get(i);
            int num = (null == units) ? 0 : units.size();

            JSONObject slice = new JSONObject();
            slice.put("slice", i);
            slice.put("beginning", beginning + i * 60L * 60 * 1000L);
            slice.put("ending", beginning + (i + 1) * 60L * 60 * 1000L);
            slice.put("numUnits", num);
            timeline.put(slice);

            if (num > 0 && (null == peak || num > peak.getInt("numUnits"))) {
                peak = slice;
            }
        }

        // 逐单元汇总
        JSONArray units = new JSONArray();
        int activeCount = 0;
        int onlineCount = 0;
        int offlineCount = 0;
        int inactiveCount = 0;
        double totalDuration = 0;
        int numActiveUnits = 0;

        for (JSONObject contact : contacts) {
            long id = contact.getLong("id");

            List<DataStorage.ContactEvent> events = dayEventMap.get(id);
            int numEvents = (null == events) ? 0 : events.size();
            long durationMillis = (null == events) ? 0 : calcOnlineDuration(events, ending);
            if (numEvents > 0) {
                ++numActiveUnits;
                totalDuration += durationMillis;
            }

            Long lastActive = lastActiveMap.get(id);
            long lastActiveTime = (null == lastActive) ? 0 : lastActive;

            String state;
            if (lastActiveTime > 0 && (now - lastActiveTime) <= UNIT_ONLINE_WINDOW) {
                state = UNIT_STATE_ONLINE;
                ++onlineCount;
            }
            else if (lastActiveTime > 0) {
                state = UNIT_STATE_OFFLINE;
                ++offlineCount;
            }
            else {
                state = UNIT_STATE_INACTIVE;
                ++inactiveCount;
            }

            if (numEvents > 0) {
                ++activeCount;
            }

            Set<Integer> days = activeDayMap.get(id);

            JSONObject unit = new JSONObject();
            unit.put("id", id);
            unit.put("name", contact.getString("name"));
            unit.put("device", contact.getString("device"));
            unit.put("platform", contact.getString("platform"));
            unit.put("state", state);
            unit.put("activeCount", numEvents);
            unit.put("duration", round2(durationMillis / (60.0 * 60.0 * 1000.0)));
            unit.put("lastActiveTime", lastActiveTime);
            unit.put("firstActiveTime", (numEvents > 0) ? events.get(0).time : 0L);
            unit.put("activeDays", (null == days) ? 0 : days.size());
            units.put(unit);
        }

        // 在线优先、最近活动时间降序、ID 升序
        List<JSONObject> sorted = new ArrayList<>(units.length());
        for (int i = 0; i < units.length(); ++i) {
            sorted.add(units.getJSONObject(i));
        }
        sorted.sort((a, b) -> {
            int sa = stateWeight(a.getString("state"));
            int sb = stateWeight(b.getString("state"));
            if (sa != sb) {
                return sa - sb;
            }
            long la = a.getLong("lastActiveTime");
            long lb = b.getLong("lastActiveTime");
            if (la != lb) {
                return (la > lb) ? -1 : 1;
            }
            long ia = a.getLong("id");
            long ib = b.getLong("id");
            return (ia < ib) ? -1 : ((ia == ib) ? 0 : 1);
        });

        JSONArray sortedUnits = new JSONArray();
        for (JSONObject unit : sorted) {
            sortedUnits.put(unit);
        }

        JSONObject data = new JSONObject();
        data.put("domain", domain);
        data.put("year", day.get(Calendar.YEAR));
        data.put("month", day.get(Calendar.MONTH) + 1);
        data.put("date", day.get(Calendar.DATE));
        data.put("beginning", beginning);
        data.put("ending", ending);
        data.put("total", contacts.size());
        data.put("activeCount", activeCount);
        data.put("onlineCount", onlineCount);
        data.put("offlineCount", offlineCount);
        data.put("inactiveCount", inactiveCount);
        data.put("totalDuration", round2(totalDuration / (60.0 * 60.0 * 1000.0)));
        data.put("avgDuration", (numActiveUnits > 0)
                ? round2(totalDuration / (60.0 * 60.0 * 1000.0) / numActiveUnits) : 0.0);
        data.put("peak", (null == peak) ? JSONObject.NULL : peak);
        data.put("timeline", timeline);
        data.put("units", sortedUnits);
        return data;
    }

    /**
     * 计算单个单元在给定区间内的在线时长。
     *
     * 口径与 service 侧 {@code StatisticsSystem#calcAOT} 一致：SignIn / Comeback 视为会话开始，
     * SignOut 结束会话，DeviceTimeout 结束会话并扣除 15 秒心跳余量；区间结束时仍未结束的会话
     * 按到区间结束计算。
     *
     * @param events 按时间升序排列的事件列表。
     * @param ending 区间结束时间戳。
     * @return 返回在线时长，单位：毫秒。
     */
    private static long calcOnlineDuration(List<DataStorage.ContactEvent> events, long ending) {
        long startTime = 0;
        long duration = 0;

        for (DataStorage.ContactEvent event : events) {
            String name = event.event;
            long time = event.time;

            if (EVENT_SIGN_IN.equals(name) || EVENT_COMEBACK.equals(name)) {
                if (startTime == 0) {
                    startTime = time;
                }
                continue;
            }

            if (EVENT_DEVICE_TIMEOUT.equals(name)) {
                if (startTime > 0) {
                    duration += (time - startTime) - 15000L;
                }
                startTime = 0;
            }
            else if (EVENT_SIGN_OUT.equals(name)) {
                if (startTime > 0) {
                    duration += time - startTime;
                }
                startTime = 0;
            }
        }

        if (duration == 0 && startTime != 0) {
            duration = ending - startTime;
        }

        return duration;
    }

    /**
     * 工作状态排序权重，在线优先。
     */
    private static int stateWeight(String state) {
        if (UNIT_STATE_ONLINE.equals(state)) {
            return 0;
        }
        if (UNIT_STATE_OFFLINE.equals(state)) {
            return 1;
        }
        return 2;
    }

    /**
     * 保留两位小数。
     */
    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
