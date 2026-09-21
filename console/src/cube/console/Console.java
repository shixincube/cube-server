/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console;

import cell.util.log.LogHandle;
import cell.util.log.LogLevel;
import cell.util.log.LogManager;
import cell.util.log.Logger;
import cube.console.mgmt.DispatcherManager;
import cube.console.mgmt.DispatcherServer;
import cube.console.mgmt.NodeHeartbeat;
import cube.console.mgmt.ServiceManager;
import cube.console.mgmt.ServiceServer;
import cube.console.mgmt.StatisticDataManager;
import cube.console.mgmt.UserManager;
import cube.report.JVMReport;
import cube.report.LogLine;
import cube.report.LogReport;
import cube.report.PerformanceReport;
import cube.report.UnitReport;
import cube.util.NodeName;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 控制台数据管理类。
 */
public final class Console implements Runnable {

    private String consoleTag;

    /**
     * 日志记录。
     */
    private ConcurrentHashMap<String, List<LogLine>> serverLogMap;

    /**
     * 记录每个服务器的最大日志行数。
     */
    private int maxLogLines = 200;

    /**
     * JVM 信息记录。
     */
    private ConcurrentHashMap<String, List<JVMReport>> serverJVMMap;

    /**
     * 性能信息记录。
     */
    private ConcurrentHashMap<String, List<PerformanceReport>> serverPerfMap;

    /**
     * AI 单元能力快照：每个节点只保留最近一份。
     */
    private ConcurrentHashMap<String, UnitSnapshot> serverUnitMap;

    /**
     * 单元能力上报的有效期。
     *
     * 节点每 60 秒上报一次，取 5 倍间隔容忍抖动与短时重连；超期视为该节点已不再承载单元，
     * 避免节点停服后控制台一直显示陈旧的能力数据。
     */
    private final static long UNIT_REPORT_TTL = 5L * 60 * 1000L;

    private int maxReportNum = 30;

    /**
     * 节点自报名到控制台期望名的归一结果缓存（只缓存成功命中，避免每次上报都做集合比对）。
     */
    private ConcurrentHashMap<String, String> reporterAliases;

    /**
     * 控制台已知的服务器期望名缓存。
     */
    private final Set<String> expectedNames = new HashSet<>();

    private volatile long expectedNamesRefreshTime = 0L;

    /**
     * 期望名缓存的有效期。
     */
    private final static long EXPECTED_NAMES_TTL = 30L * 1000L;

    private ScheduledExecutorService timer;

    private ConsoleLogHandler logHandler;

    private UserManager userManager;

    private DispatcherManager dispatcherManager;

    private ServiceManager serviceManager;

    private StatisticDataManager statisticDataManager;

    public Console() {
        this.serverLogMap = new ConcurrentHashMap<>();
        this.serverJVMMap = new ConcurrentHashMap<>();
        this.serverPerfMap = new ConcurrentHashMap<>();
        this.serverUnitMap = new ConcurrentHashMap<>();
        this.reporterAliases = new ConcurrentHashMap<>();
        this.logHandler = new ConsoleLogHandler();
    }

    /**
     * 一次单元能力上报的快照。
     *
     * <code>receiveTime</code> 记控制台收到报告的时刻，而不是报告的生成时刻：节点与控制台的
     * 系统时间存在偏差时，用节点时间做有效期判断会误判为过期（与 NodeHeartbeat 同一考虑）。
     */
    private final static class UnitSnapshot {

        final UnitReport report;

        final long receiveTime;

        UnitSnapshot(UnitReport report, long receiveTime) {
            this.report = report;
            this.receiveTime = receiveTime;
        }
    }

    public String getTag() {
        return this.consoleTag;
    }

    /**
     * 将节点自报名归一为控制台的期望名。
     *
     * 控制台里的服务器名由本地推导（`&lt;tag&gt;#&lt;role&gt;#&lt;port&gt;`），节点上报时携带的是它自己算出的名字。
     * 当两者前缀不一致（跨机部署、历史版本、网卡集合变化）时，若仍按精确键存取，监控数据会静默落到另一个键上：
     * 列表显示正常但 JVM / 性能 / 日志全空且不报错。这里以「角色 + 端口」后缀唯一命中为准做一次归一，
     * 使上报数据落到控制台期望的键上。歧义（同名后缀不止一个）时拒绝猜测，保持原样并告警。
     *
     * @param reporter 节点自报名。
     * @return 归一后的名字；无法判断时返回入参。
     */
    public String resolveReporter(String reporter) {
        if (null == reporter || reporter.length() == 0) {
            return reporter;
        }

        String alias = this.reporterAliases.get(reporter);
        if (null != alias) {
            return alias;
        }

        if (null == this.dispatcherManager && null == this.serviceManager) {
            return reporter;
        }

        Set<String> names = this.expectedNames();
        NodeName.Resolution resolution = NodeName.resolve(reporter, names);

        if (resolution.ambiguous) {
            Logger.w(this.getClass(), "#resolveReporter - 节点名后缀命中多个服务器，无法归一: " + reporter);
            return reporter;
        }

        if (resolution.normalized) {
            Logger.i(this.getClass(), "#resolveReporter - 节点自报名与期望名不一致，已归一: "
                    + reporter + " -> " + resolution.name);
        }

        // 仅在期望名集合非空时缓存，否则冷启动阶段可能把本该归一的名字误缓存为精确命中
        if (null != resolution.name && !names.isEmpty()) {
            this.reporterAliases.put(reporter, resolution.name);
        }

        return resolution.name;
    }

    /**
     * 返回控制台已知的服务器期望名集合。
     *
     * 优先返回管理器内存缓存里的名字（零 IO，适合上报热路径）；只有当内存里还没有任何服务器
     * （控制台刚启动、前端尚未拉取过列表）时，才按 TTL 做一次完整加载 —— 完整加载会读数据库并
     * 触发各服务器的状态刷新（含端口探测），不能放在每份上报上。
     *
     * @return 期望名集合。
     */
    private Set<String> expectedNames() {
        Set<String> names = new HashSet<>();
        if (null != this.dispatcherManager) {
            names.addAll(this.dispatcherManager.listServerNames());
        }
        if (null != this.serviceManager) {
            names.addAll(this.serviceManager.listServerNames());
        }

        if (!names.isEmpty()) {
            return names;
        }

        long now = System.currentTimeMillis();

        if (now - this.expectedNamesRefreshTime > EXPECTED_NAMES_TTL) {
            synchronized (this.expectedNames) {
                if (now - this.expectedNamesRefreshTime > EXPECTED_NAMES_TTL) {
                    Set<String> loaded = new HashSet<>();

                    if (null != this.dispatcherManager) {
                        collectNames(this.dispatcherManager.listDispatcherServers(), loaded);
                    }
                    if (null != this.serviceManager) {
                        collectNames(this.serviceManager.listServiceServers(), loaded);
                    }

                    this.expectedNames.clear();
                    this.expectedNames.addAll(loaded);
                    this.expectedNamesRefreshTime = now;
                }
            }
        }

        synchronized (this.expectedNames) {
            names.addAll(this.expectedNames);
        }

        return names;
    }

    private void collectNames(java.util.Collection<?> servers, Set<String> output) {
        if (null == servers) {
            return;
        }

        for (Object server : servers) {
            String name = null;
            if (server instanceof DispatcherServer) {
                name = ((DispatcherServer) server).getName();
            }
            else if (server instanceof ServiceServer) {
                name = ((ServiceServer) server).getName();
            }

            if (null != name) {
                output.add(name);
            }
        }
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    public DispatcherManager getDispatcherManager() {
        return this.dispatcherManager;
    }

    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    public void launch() {
        LogManager.getInstance().addHandle(this.logHandler);

        // 生成节点标识：默认取本机主网卡派生的稳定标识，与节点侧使用同一策略
        this.consoleTag = NodeName.identity();

        this.userManager = new UserManager();
        this.dispatcherManager = new DispatcherManager(this.consoleTag);
        this.serviceManager = new ServiceManager(this.consoleTag);
        this.statisticDataManager = new StatisticDataManager();

        this.userManager.start();
        this.dispatcherManager.start();
        this.serviceManager.start();
        this.statisticDataManager.start();

        this.timer = Executors.newScheduledThreadPool(2);
        this.timer.scheduleWithFixedDelay(this, 10L, 10L, TimeUnit.SECONDS);

        Logger.i(this.getClass(), "#launch - tag: " + this.consoleTag);
    }

    public void destroy() {
        this.statisticDataManager.stop();
        this.serviceManager.stop();
        this.dispatcherManager.stop();
        this.userManager.stop();

        this.timer.shutdown();
    }

    public void appendLogReport(LogReport report) {
        // 归一节点名：自报名与控制台期望名不一致时，数据仍落到控制台期望的键上
        String name = this.resolveReporter(report.getReporter());

        // 收到上报即视为节点心跳，节点的运行状态据此判定（见 NodeHeartbeat）
        NodeHeartbeat.getInstance().trace(name);

        List<LogLine> list = this.serverLogMap.get(name);
        if (null == list) {
            list = new Vector<>();
            this.serverLogMap.put(name, list);
        }

        list.addAll(report.getLogs());

        int d = list.size() - this.maxLogLines;
        while (d > 0) {
            list.remove(0);
            --d;
        }
    }

    public List<LogLine> queryLogs(String serverName, long startTimestamp, int maxLength) {
        ArrayList<LogLine> result = new ArrayList<>();
        List<LogLine> list = this.serverLogMap.get(this.resolveReporter(serverName));
        if (null != list) {
            for (int i = 0, size = list.size(); i < size; ++i) {
                LogLine line = list.get(i);
                if (line.time > startTimestamp) {
                    result.add(line);
                    if (result.size() >= maxLength) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<LogLine> queryConsoleLogs(long startTimestamp, int maxLength) {
        ArrayList<LogLine> list = new ArrayList<>();
        synchronized (this.logHandler.logLines) {
            for (int i = 0, size = this.logHandler.logLines.size(); i < size; ++i) {
                LogLine line = this.logHandler.logLines.get(i);
                if (line.time > startTimestamp) {
                    list.add(line);
                    if (list.size() >= maxLength) {
                        break;
                    }
                }
            }
        }
        return list;
    }

    public void appendJVMReport(JVMReport report) {
        Logger.d(this.getClass(), "Received report from " + report.getReporter() + " (" + report.getName() + ")");

        // 归一节点名：自报名与控制台期望名不一致时，数据仍落到控制台期望的键上
        String name = this.resolveReporter(report.getReporter());

        // 收到上报即视为节点心跳，节点的运行状态据此判定（见 NodeHeartbeat）
        NodeHeartbeat.getInstance().trace(name);

        List<JVMReport> list = this.serverJVMMap.get(name);
        if (null == list) {
            list = new Vector<>();
            this.serverJVMMap.put(name, list);
        }

        report.scaleValue(1048576);
        list.add(report);
        if (list.size() > this.maxReportNum) {
            list.remove(0);
        }
    }

    public List<JVMReport> queryJVMReport(String reporter, int num, long time) {
        List<JVMReport> result = new ArrayList<>(num);
        List<JVMReport> list = this.serverJVMMap.get(this.resolveReporter(reporter));
        if (null == list) {
            long reportTime = time;
            for (int i = 0; i < num; ++i) {
                JVMReport empty = new JVMReport(reporter, reportTime);
                empty.dump();
                result.add(empty);
                reportTime -= 60000L;
            }
            Collections.reverse(result);
            return result;
        }

        // 节点每 60 秒上报一次，而守护任务的调度粒度是 10 秒，实际间隔落在 60~70 秒之间。
        // 锚点匹配窗口若取 60 秒，查询恰好落在两次上报之间时会匹配不到任何记录，
        // 整个结果集被填成全 0 的占位报告（前端图表因此“时有时无”）。取 2 倍间隔即可覆盖抖动。
        long scope = 2 * 60000L;
        int index = 0;

        // 找到最近的记录
        for (index = list.size() - 1; index >= 0; --index) {
            JVMReport report = list.get(index);
            if (Math.abs(time - report.getTimestamp()) < scope) {
                break;
            }
        }

        while (index >= 0) {
            JVMReport report = list.get(index);
            result.add(report);
            if (result.size() == num) {
                break;
            }
            --index;
        }

        int d = num - result.size();
        if (d > 0) {
            long reportTime = result.isEmpty() ? time : result.get(result.size() - 1).getTimestamp();
            for (int i = 0; i < d; ++i) {
                reportTime -= 60000L;
                JVMReport empty = new JVMReport(reporter, reportTime);
                empty.dump();
                result.add(empty);
            }
        }

        Collections.reverse(result);

        return result;
    }

    public void appendPerformanceReport(PerformanceReport report) {
        Logger.d(this.getClass(), "Received report from " + report.getReporter() + " (" + report.getName() + ")");

        // 归一节点名：自报名与控制台期望名不一致时，数据仍落到控制台期望的键上
        String name = this.resolveReporter(report.getReporter());

        // 收到上报即视为节点心跳，节点的运行状态据此判定（见 NodeHeartbeat）
        NodeHeartbeat.getInstance().trace(name);

        List<PerformanceReport> list = this.serverPerfMap.get(name);
        if (null == list) {
            list = new Vector<>();
            this.serverPerfMap.put(name, list);
        }

        list.add(report);
        if (list.size() > this.maxReportNum) {
            list.remove(0);
        }
    }

    public PerformanceReport queryLastPerformanceReport(String reporter) {
        List<PerformanceReport> list = this.serverPerfMap.get(this.resolveReporter(reporter));
        if (null == list) {
            return null;
        }

        return list.get(list.size() - 1);
    }

    public PerformanceReport queryPerformanceReport(String reporter, long timestamp) {
        List<PerformanceReport> list = this.serverPerfMap.get(this.resolveReporter(reporter));
        if (null == list) {
            return null;
        }

        PerformanceReport report = null;

        for (int i = list.size() - 1; i >= 0; --i) {
            PerformanceReport pr = list.get(i);
            if (Math.abs(pr.getTimestamp() - timestamp) < 5000) {
                report = pr;
                break;
            }
        }

        return report;
    }

    public StatisticDataManager getStatisticDataManager() {
        return this.statisticDataManager;
    }

    public void appendUnitReport(UnitReport report) {
        Logger.d(this.getClass(), "Received report from " + report.getReporter() + " (" + report.getName() + ")");

        // 归一节点名：自报名与控制台期望名不一致时，数据仍落到控制台期望的键上
        String name = this.resolveReporter(report.getReporter());

        // 收到上报即视为节点心跳，节点的运行状态据此判定（见 NodeHeartbeat）
        NodeHeartbeat.getInstance().trace(name);

        // 单元能力是「当前态」而非时序数据，只保留最近一份
        this.serverUnitMap.put(name, new UnitSnapshot(report, System.currentTimeMillis()));
    }

    /**
     * 汇总各节点上报的单元能力。
     *
     * <p>同一个 Contact 物理实体在节点上可以注册多个 AIGC 单元（每个单元一个能力），多台节点也
     * 可能都上报同一个实体，因此按物理实体 ID 归并、并按能力内容去重。</p>
     *
     * @param domain 指定域。
     * @return 返回 Key 为物理实体 ID 、Value 为该物理实体承载的能力 JSON 列表（按名称排序）。
     */
    public Map<Long, List<JSONObject>> queryUnitCapabilities(String domain) {
        Map<Long, List<JSONObject>> result = new HashMap<>();
        // 多个节点可能上报同一个实体的同一能力，用能力 JSON 文本去重
        Map<Long, Set<String>> seen = new HashMap<>();

        long now = System.currentTimeMillis();

        for (Map.Entry<String, UnitSnapshot> entry : this.serverUnitMap.entrySet()) {
            UnitSnapshot snapshot = entry.getValue();

            if (now - snapshot.receiveTime > UNIT_REPORT_TTL) {
                // 超期快照即时清理，避免节点停服后能力数据永久残留
                this.serverUnitMap.remove(entry.getKey(), snapshot);
                continue;
            }

            for (JSONObject item : snapshot.report.getUnits()) {
                if (!domain.equals(item.optString("domain"))) {
                    continue;
                }

                JSONObject capability = item.optJSONObject("capability");
                if (null == capability) {
                    continue;
                }

                long contactId = item.optLong("id", 0L);

                List<JSONObject> list = result.get(contactId);
                if (null == list) {
                    list = new ArrayList<>();
                    result.put(contactId, list);
                    seen.put(contactId, new HashSet<String>());
                }

                if (seen.get(contactId).add(capability.toString())) {
                    list.add(capability);
                }
            }
        }

        // 名称排序，保证同一实体每次返回的能力顺序一致，表格不会跳动
        for (List<JSONObject> list : result.values()) {
            list.sort((a, b) -> a.optString("name").compareTo(b.optString("name")));
        }

        return result;
    }

    /**
     * 最近一次收到单元能力上报的时间戳（控制台本地时钟），0 表示从未收到。
     */
    public long getUnitReportTimestamp() {
        long timestamp = 0L;
        for (UnitSnapshot snapshot : this.serverUnitMap.values()) {
            if (snapshot.receiveTime > timestamp) {
                timestamp = snapshot.receiveTime;
            }
        }
        return timestamp;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        this.dispatcherManager.tick(now);
        this.serviceManager.tick(now);
    }

    protected class ConsoleLogHandler implements LogHandle {

        protected List<LogLine> logLines;

        public ConsoleLogHandler() {
            this.logLines = new ArrayList<>();
        }

        @Override
        public String getName() {
            return "ConsoleLog";
        }

        @Override
        public void logDebug(String tag, String text) {
            this.recordLog(LogLevel.DEBUG, tag, text);
        }

        @Override
        public void logInfo(String tag, String text) {
            this.recordLog(LogLevel.INFO, tag, text);
        }

        @Override
        public void logWarning(String tag, String text) {
            this.recordLog(LogLevel.WARNING, tag, text);
        }

        @Override
        public void logError(String tag, String text) {
            this.recordLog(LogLevel.ERROR, tag, text);
        }

        private void recordLog(LogLevel level, String tag, String text) {
            LogLine log = new LogLine(level.getCode(), tag, text, System.currentTimeMillis());
            synchronized (this.logLines) {
                this.logLines.add(log);

                if (this.logLines.size() > maxLogLines) {
                    this.logLines.remove(0);
                }
            }
        }
    }
}
