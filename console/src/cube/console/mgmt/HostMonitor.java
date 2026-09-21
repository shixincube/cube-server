/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSFileStore;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机硬件与运行性能采集器。
 *
 * <p>数据来源分三层，各取所长：</p>
 * <ul>
 *   <li><b>oshi-core</b>：跨平台读取主机硬件（主板、BIOS、CPU 规格、内存条、磁盘、网卡）与系统级指标
 *       （CPU tick、负载均值、交换区、进程 / 线程数）。</li>
 *   <li><b>JNA</b>：由 oshi-core 内部经 JNA 调用平台原生接口，本类不直接依赖 JNA API。</li>
 *   <li><b>Java 原生 API</b>：JVM 自身指标（堆 / 非堆、线程、运行时长）以及网卡地址、磁盘可用空间等，
 *       这些信息无需绕道原生调用。</li>
 * </ul>
 *
 * <p>动态指标（CPU 使用率、网络 / 磁盘吞吐）必须由两次采样求差得到，因此实例内部保存上一次采样点。
 * 前端两次抓取间隔过短（小于 {@link #MIN_SAMPLE_INTERVAL}）时直接复用上一次结果，
 * 避免在极短时间窗内算出无意义的抖动值。</p>
 */
public final class HostMonitor {

    /** 静态信息缓存有效期：硬件规格几乎不变，缓存 5 分钟足够。 */
    private final static long STATIC_CACHE_TTL = 5L * 60L * 1000L;

    /** 动态采样最小间隔，同时作为动态结果的缓存有效期。 */
    private final static long MIN_SAMPLE_INTERVAL = 1000L;

    /** 网络明细最多返回的网卡数量（按瞬时速率排序）。 */
    private final static int MAX_NETWORK_DETAIL = 4;

    /** 磁盘分区明细最多返回的条数（按容量排序）。 */
    private final static int MAX_PARTITION_DETAIL = 6;

    /** 视为「真实卷」的最小容量：小于该值的挂载点通常是伪文件系统，展示使用率没有意义。 */
    private final static long MIN_MEANINGFUL_VOLUME_BYTES = 100L * 1024L * 1024L;

    private static final HostMonitor INSTANCE = new HostMonitor();

    private final HardwareAbstractionLayer hardware;

    private final OperatingSystem operatingSystem;

    /** 上一次采样的 CPU tick。 */
    private long[] lastCpuTicks;

    /** 上一次采样的累计字节数：网络收发聚合值。 */
    private long lastNetRecv;

    private long lastNetSent;

    /** 各网卡上一次采样的累计字节数，键为网卡名，值为 [recv, sent]。 */
    private final Map<String, long[]> lastInterfaceCounters = new ConcurrentHashMap<>();

    /** 各磁盘上一次采样的累计字节数，键为磁盘名，值为 [read, write]。 */
    private final Map<String, long[]> lastDiskCounters = new ConcurrentHashMap<>();

    /** 上一次采样的磁盘读写聚合值。 */
    private long lastDiskRead;

    private long lastDiskWrite;

    private long lastSampleTime;

    private JSONObject metricsCache;

    private long metricsCacheTime;

    private JSONObject staticCache;

    private long staticCacheTime;

    private HostMonitor() {
        HardwareAbstractionLayer hal = null;
        OperatingSystem os = null;
        try {
            SystemInfo info = new SystemInfo();
            hal = info.getHardware();
            os = info.getOperatingSystem();
        }
        catch (Throwable e) {
            // 部分受限容器或尚未支持的平台上 oshi 初始化会失败，
            // 此时降级为仅输出 Java 原生 API 能提供的部分信息
            Logger.w(this.getClass(), "#HostMonitor - oshi 初始化失败，降级为 Java 原生 API", e);
        }

        this.hardware = hal;
        this.operatingSystem = os;
    }

    public static HostMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * 主机静态配置信息。
     *
     * @return JSON 结构；任一项读取失败时该字段为空串或 0，保证前端始终可解析。
     */
    public JSONObject getStaticInfo() {
        long now = System.currentTimeMillis();
        JSONObject cached = this.staticCache;
        if (null != cached && (now - this.staticCacheTime) < STATIC_CACHE_TTL) {
            return cached;
        }

        JSONObject json = new JSONObject();
        json.put("sampledAt", now);

        try {
            json.put("host", this.buildHostSection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集主机信息失败", e);
            json.put("host", new JSONObject());
        }

        try {
            json.put("system", this.buildSystemSection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集整机信息失败", e);
            json.put("system", new JSONObject());
        }

        try {
            json.put("cpu", this.buildCpuSection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集 CPU 信息失败", e);
            json.put("cpu", new JSONObject());
        }

        try {
            json.put("memory", this.buildMemorySection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集内存信息失败", e);
            json.put("memory", new JSONObject());
        }

        try {
            json.put("diskDrives", this.buildDiskSection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集磁盘信息失败", e);
            json.put("diskDrives", new JSONArray());
        }

        try {
            json.put("nicList", this.buildNicSection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集网卡信息失败", e);
            json.put("nicList", new JSONArray());
        }

        try {
            json.put("runtime", this.buildRuntimeSection());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getStaticInfo - 采集运行时信息失败", e);
            json.put("runtime", new JSONObject());
        }

        this.staticCache = json;
        this.staticCacheTime = now;
        return json;
    }

    /**
     * 主机动态性能指标，返回当前时刻的快照。
     *
     * @return JSON 结构。
     */
    public JSONObject getMetrics() {
        long now = System.currentTimeMillis();
        JSONObject cached = this.metricsCache;
        if (null != cached && (now - this.metricsCacheTime) < MIN_SAMPLE_INTERVAL) {
            return cached;
        }

        JSONObject json = new JSONObject();
        json.put("timestamp", now);

        // 与上一次采样的时间差（秒）：0 表示这是首次采样，此时速率类字段缺省
        double elapsed = (this.lastSampleTime > 0L) ? (now - this.lastSampleTime) / 1000.0d : 0.0d;
        json.put("sampleIntervalSeconds", round(elapsed, 3));

        try {
            json.put("cpu", this.buildCpuMetrics());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getMetrics - 采集 CPU 指标失败", e);
        }

        try {
            json.put("memory", this.buildMemoryMetrics());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getMetrics - 采集内存指标失败", e);
        }

        try {
            json.put("jvm", this.buildJvmMetrics());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getMetrics - 采集 JVM 指标失败", e);
        }

        try {
            json.put("network", this.buildNetworkMetrics(elapsed));
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getMetrics - 采集网络指标失败", e);
        }

        try {
            json.put("disk", this.buildDiskMetrics(elapsed));
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getMetrics - 采集磁盘指标失败", e);
        }

        try {
            json.put("system", this.buildSystemMetrics());
        }
        catch (Throwable e) {
            Logger.w(this.getClass(), "#getMetrics - 采集系统指标失败", e);
        }

        // 记录本次采样点，供下次求差
        this.lastSampleTime = now;
        this.metricsCache = json;
        this.metricsCacheTime = now;
        return json;
    }

    // ------------------------------------------------------------------
    // 静态信息
    // ------------------------------------------------------------------

    private JSONObject buildHostSection() {
        JSONObject host = new JSONObject();
        host.put("hostName", resolveHostName());
        host.put("osName", systemProperty("os.name"));
        host.put("osVersion", systemProperty("os.version"));
        host.put("osArch", systemProperty("os.arch"));
        host.put("timezone", systemProperty("user.timezone"));

        if (null != this.operatingSystem) {
            host.put("osFamily", nullToEmpty(safe(this.operatingSystem::getFamily, "")));
            host.put("osManufacturer", nullToEmpty(safe(this.operatingSystem::getManufacturer, "")));
            host.put("bitness", safe(this.operatingSystem::getBitness, 0));
            host.put("bootTime", bootTimeMillis());

            OperatingSystem.OSVersionInfo versionInfo = safe(this.operatingSystem::getVersionInfo, null);
            if (null != versionInfo) {
                host.put("osVersionInfo", nullToEmpty(versionInfo.getVersion()));
                host.put("osBuildNumber", nullToEmpty(versionInfo.getBuildNumber()));
                host.put("osCodeName", nullToEmpty(versionInfo.getCodeName()));
            }
        }

        return host;
    }

    private JSONObject buildSystemSection() {
        JSONObject section = new JSONObject();

        ComputerSystem computerSystem = (null != this.hardware)
                ? safe(this.hardware::getComputerSystem, null) : null;
        if (null == computerSystem) {
            return section;
        }

        section.put("manufacturer", nullToEmpty(safe(computerSystem::getManufacturer, "")));
        section.put("model", nullToEmpty(safe(computerSystem::getModel, "")));
        section.put("serialNumber", nullToEmpty(safe(computerSystem::getSerialNumber, "")));
        section.put("uuid", nullToEmpty(safe(computerSystem::getHardwareUUID, "")));

        Firmware firmware = safe(computerSystem::getFirmware, null);
        if (null != firmware) {
            JSONObject item = new JSONObject();
            item.put("manufacturer", nullToEmpty(safe(firmware::getManufacturer, "")));
            item.put("name", nullToEmpty(safe(firmware::getName, "")));
            item.put("version", nullToEmpty(safe(firmware::getVersion, "")));
            item.put("releaseDate", nullToEmpty(safe(firmware::getReleaseDate, "")));
            section.put("firmware", item);
        }

        Baseboard baseboard = safe(computerSystem::getBaseboard, null);
        if (null != baseboard) {
            JSONObject item = new JSONObject();
            item.put("manufacturer", nullToEmpty(safe(baseboard::getManufacturer, "")));
            item.put("model", nullToEmpty(safe(baseboard::getModel, "")));
            item.put("version", nullToEmpty(safe(baseboard::getVersion, "")));
            item.put("serialNumber", nullToEmpty(safe(baseboard::getSerialNumber, "")));
            section.put("baseboard", item);
        }

        return section;
    }

    private JSONObject buildCpuSection() {
        JSONObject cpu = new JSONObject();
        if (null == this.hardware) {
            return cpu;
        }

        CentralProcessor processor = safe(this.hardware::getProcessor, null);
        if (null == processor) {
            return cpu;
        }

        CentralProcessor.ProcessorIdentifier identifier = safe(processor::getProcessorIdentifier, null);
        if (null != identifier) {
            cpu.put("name", nullToEmpty(identifier.getName()));
            cpu.put("vendor", nullToEmpty(identifier.getVendor()));
            cpu.put("family", nullToEmpty(identifier.getFamily()));
            cpu.put("model", nullToEmpty(identifier.getModel()));
            cpu.put("stepping", nullToEmpty(identifier.getStepping()));
            cpu.put("microarchitecture", nullToEmpty(identifier.getMicroarchitecture()));
            cpu.put("processorId", nullToEmpty(identifier.getProcessorID()));
            cpu.put("identifier", nullToEmpty(identifier.getIdentifier()));
            cpu.put("cpu64bit", safe(identifier::isCpu64bit, false));
        }

        cpu.put("physicalCores", safe(processor::getPhysicalProcessorCount, 0));
        cpu.put("logicalCores", safe(processor::getLogicalProcessorCount, 0));
        cpu.put("physicalPackages", safe(processor::getPhysicalPackageCount, 0));
        cpu.put("maxFrequencyHz", safe(processor::getMaxFreq, 0L));

        JSONArray caches = new JSONArray();
        List<CentralProcessor.ProcessorCache> cacheList = safe(processor::getProcessorCaches, null);
        if (null != cacheList) {
            for (CentralProcessor.ProcessorCache cache : cacheList) {
                JSONObject item = new JSONObject();
                item.put("level", cache.getLevel());
                item.put("type", nullToEmpty(String.valueOf(cache.getType())));
                item.put("sizeBytes", cache.getCacheSize());
                caches.put(item);
            }
        }
        cpu.put("caches", caches);

        return cpu;
    }

    private JSONObject buildMemorySection() {
        JSONObject memory = new JSONObject();
        if (null == this.hardware) {
            return memory;
        }

        GlobalMemory globalMemory = safe(this.hardware::getMemory, null);
        if (null == globalMemory) {
            return memory;
        }

        memory.put("totalBytes", safe(globalMemory::getTotal, 0L));
        memory.put("pageSizeBytes", safe(globalMemory::getPageSize, 0L));

        JSONArray modules = new JSONArray();
        List<PhysicalMemory> list = safe(globalMemory::getPhysicalMemory, null);
        if (null != list) {
            for (PhysicalMemory module : list) {
                JSONObject item = new JSONObject();
                item.put("bankLabel", nullToEmpty(module.getBankLabel()));
                item.put("capacityBytes", module.getCapacity());
                item.put("clockSpeedHz", module.getClockSpeed());
                item.put("manufacturer", nullToEmpty(module.getManufacturer()));
                item.put("memoryType", nullToEmpty(module.getMemoryType()));
                item.put("partNumber", nullToEmpty(module.getPartNumber()));
                modules.put(item);
            }
        }
        memory.put("modules", modules);

        return memory;
    }

    private JSONArray buildDiskSection() {
        JSONArray drives = new JSONArray();
        if (null == this.hardware) {
            return drives;
        }

        List<HWDiskStore> list = safe(this.hardware::getDiskStores, null);
        if (null == list) {
            return drives;
        }

        for (HWDiskStore disk : list) {
            JSONObject item = new JSONObject();
            item.put("name", nullToEmpty(disk.getName()));
            item.put("model", nullToEmpty(disk.getModel()));
            item.put("serial", nullToEmpty(disk.getSerial()));
            item.put("sizeBytes", safe(disk::getSize, 0L));

            JSONArray partitions = new JSONArray();
            List<HWPartition> partitionList = safe(disk::getPartitions, null);
            if (null != partitionList) {
                for (HWPartition partition : partitionList) {
                    JSONObject part = new JSONObject();
                    part.put("identification", nullToEmpty(partition.getIdentification()));
                    part.put("name", nullToEmpty(partition.getName()));
                    part.put("type", nullToEmpty(partition.getType()));
                    part.put("label", nullToEmpty(partition.getLabel()));
                    part.put("sizeBytes", partition.getSize());
                    part.put("mountPoint", nullToEmpty(partition.getMountPoint()));
                    partitions.put(part);
                }
            }
            item.put("partitions", partitions);
            drives.put(item);
        }

        return drives;
    }

    private JSONArray buildNicSection() throws Exception {
        JSONArray array = new JSONArray();

        // 网卡地址使用 Java 原生 API 读取，与 oshi 的流量统计互补
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (null != enumeration && enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (null == networkInterface) {
                continue;
            }
            if (safe(networkInterface::isLoopback, true) || safe(networkInterface::isVirtual, true)) {
                continue;
            }

            JSONArray addresses = new JSONArray();
            Enumeration<InetAddress> addressEnumeration = networkInterface.getInetAddresses();
            while (addressEnumeration.hasMoreElements()) {
                addresses.put(addressEnumeration.nextElement().getHostAddress());
            }

            JSONObject item = new JSONObject();
            item.put("name", nullToEmpty(networkInterface.getName()));
            item.put("displayName", nullToEmpty(networkInterface.getDisplayName()));
            item.put("up", safe(networkInterface::isUp, false));
            item.put("mtu", safe(networkInterface::getMTU, 0));
            item.put("mac", formatMac(safe(networkInterface::getHardwareAddress, null)));
            item.put("addresses", addresses);
            array.put(item);
        }

        return array;
    }

    private JSONObject buildRuntimeSection() {
        JSONObject runtime = new JSONObject();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        runtime.put("javaVersion", systemProperty("java.version"));
        runtime.put("javaVendor", systemProperty("java.vendor"));
        runtime.put("jvmName", nullToEmpty(runtimeMXBean.getVmName()));
        runtime.put("jvmVersion", nullToEmpty(runtimeMXBean.getVmVersion()));
        runtime.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        runtime.put("configuredMaxHeapBytes", Runtime.getRuntime().maxMemory());
        runtime.put("fileEncoding", systemProperty("file.encoding"));
        runtime.put("userName", systemProperty("user.name"));
        runtime.put("workDir", systemProperty("user.dir"));
        runtime.put("pid", parsePid(runtimeMXBean.getName()));
        runtime.put("startTime", safe(runtimeMXBean::getStartTime, 0L));

        return runtime;
    }

    // ------------------------------------------------------------------
    // 动态指标
    // ------------------------------------------------------------------

    private JSONObject buildCpuMetrics() {
        JSONObject cpu = new JSONObject();
        if (null == this.hardware) {
            return cpu;
        }

        CentralProcessor processor = safe(this.hardware::getProcessor, null);
        if (null == processor) {
            return cpu;
        }

        long[] ticks = safe(processor::getSystemCpuLoadTicks, null);
        if (null != ticks) {
            // 首次采样没有基准 tick，只能给出「无使用率」的构成占位
            if (null != this.lastCpuTicks && this.lastCpuTicks.length == ticks.length) {
                final long[] previous = this.lastCpuTicks;
                double load = safe(() -> processor.getSystemCpuLoadBetweenTicks(previous), -1.0d);
                if (load >= 0.0d) {
                    cpu.put("usage", round(load, 4));
                }
                cpu.put("breakdown", buildTickBreakdown(previous, ticks));
            }
            this.lastCpuTicks = ticks.clone();
        }

        // 负载均值（1 / 5 / 15 分钟），仅类 Unix 系统有值
        double[] loadAverage = safe(() -> processor.getSystemLoadAverage(3), null);
        if (null != loadAverage && loadAverage.length >= 3) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < 3; ++i) {
                array.put(loadAverage[i] < 0.0d ? JSONObject.NULL : (Object) round(loadAverage[i], 2));
            }
            cpu.put("loadAverage", array);
        }

        cpu.put("contextSwitches", safe(processor::getContextSwitches, 0L));
        cpu.put("interrupts", safe(processor::getInterrupts, 0L));
        cpu.put("logicalCores", safe(processor::getLogicalProcessorCount, 0));

        // 控制台进程自身的 CPU 占用，取自 Java 原生管理接口
        cpu.put("processUsage", round(readProcessCpuLoad(), 4));

        return cpu;
    }

    /**
     * 按 tick 差值拆出 CPU 时间构成，供前端绘制占比条。
     */
    private JSONObject buildTickBreakdown(long[] previous, long[] current) {
        JSONObject breakdown = new JSONObject();

        long[] delta = new long[current.length];
        long total = 0L;
        for (int i = 0; i < current.length; ++i) {
            delta[i] = Math.max(0L, current[i] - previous[i]);
            total += delta[i];
        }

        if (total <= 0L) {
            return breakdown;
        }

        putRatio(breakdown, "user", tick(delta, CentralProcessor.TickType.USER), total);
        putRatio(breakdown, "system", tick(delta, CentralProcessor.TickType.SYSTEM), total);
        putRatio(breakdown, "idle", tick(delta, CentralProcessor.TickType.IDLE), total);
        putRatio(breakdown, "iowait", tick(delta, CentralProcessor.TickType.IOWAIT), total);
        putRatio(breakdown, "irq", tick(delta, CentralProcessor.TickType.IRQ)
                + tick(delta, CentralProcessor.TickType.SOFTIRQ), total);
        putRatio(breakdown, "nice", tick(delta, CentralProcessor.TickType.NICE), total);
        putRatio(breakdown, "steal", tick(delta, CentralProcessor.TickType.STEAL), total);

        return breakdown;
    }

    private static long tick(long[] delta, CentralProcessor.TickType type) {
        int index = type.getIndex();
        return (index >= 0 && index < delta.length) ? delta[index] : 0L;
    }

    private static void putRatio(JSONObject json, String key, long value, long total) {
        json.put(key, round((double) value / (double) total, 4));
    }

    private JSONObject buildMemoryMetrics() {
        JSONObject memory = new JSONObject();
        if (null == this.hardware) {
            return memory;
        }

        GlobalMemory globalMemory = safe(this.hardware::getMemory, null);
        if (null == globalMemory) {
            return memory;
        }

        long total = safe(globalMemory::getTotal, 0L);
        long available = safe(globalMemory::getAvailable, 0L);
        long used = Math.max(0L, total - available);

        memory.put("totalBytes", total);
        memory.put("availableBytes", available);
        memory.put("usedBytes", used);
        memory.put("usage", (total <= 0L) ? 0.0d : round((double) used / (double) total, 4));

        long swapTotal = safe(() -> globalMemory.getVirtualMemory().getSwapTotal(), 0L);
        long swapUsed = safe(() -> globalMemory.getVirtualMemory().getSwapUsed(), 0L);
        memory.put("swapTotalBytes", swapTotal);
        memory.put("swapUsedBytes", swapUsed);
        memory.put("swapUsage", (swapTotal <= 0L) ? 0.0d : round((double) swapUsed / (double) swapTotal, 4));

        return memory;
    }

    private JSONObject buildJvmMetrics() {
        JSONObject jvm = new JSONObject();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        long heapMax = (heap.getMax() > 0L) ? heap.getMax() : Runtime.getRuntime().maxMemory();
        jvm.put("heapUsedBytes", heap.getUsed());
        jvm.put("heapCommittedBytes", heap.getCommitted());
        jvm.put("heapMaxBytes", heapMax);
        jvm.put("heapUsage", (heapMax <= 0L) ? 0.0d : round((double) heap.getUsed() / (double) heapMax, 4));
        jvm.put("nonHeapUsedBytes", nonHeap.getUsed());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        jvm.put("threadCount", threadMXBean.getThreadCount());
        jvm.put("peakThreadCount", threadMXBean.getPeakThreadCount());

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        jvm.put("uptimeMillis", runtimeMXBean.getUptime());
        jvm.put("startTime", runtimeMXBean.getStartTime());

        return jvm;
    }

    private JSONObject buildNetworkMetrics(double elapsedSeconds) {
        JSONObject network = new JSONObject();
        if (null == this.hardware) {
            return network;
        }

        List<NetworkIF> list = safe(this.hardware::getNetworkIFs, null);
        if (null == list) {
            return network;
        }

        long aggregateRecv = 0L;
        long aggregateSent = 0L;
        long aggregateRxPackets = 0L;
        long aggregateTxPackets = 0L;
        JSONArray interfaces = new JSONArray();
        Set<String> seenNames = new HashSet<>();

        for (NetworkIF networkIF : list) {
            try {
                networkIF.updateAttributes();

                long recv = networkIF.getBytesRecv();
                long sent = networkIF.getBytesSent();
                aggregateRecv += recv;
                aggregateSent += sent;
                aggregateRxPackets += networkIF.getPacketsRecv();
                aggregateTxPackets += networkIF.getPacketsSent();

                String name = nullToEmpty(networkIF.getName());
                seenNames.add(name);
                long[] previous = this.lastInterfaceCounters.get(name);

                JSONObject item = new JSONObject();
                item.put("name", name);
                item.put("displayName", nullToEmpty(networkIF.getDisplayName()));
                item.put("mac", nullToEmpty(networkIF.getMacaddr()));
                String[] ipv4 = networkIF.getIPv4addr();
                item.put("ipv4", (null == ipv4) ? new JSONArray() : new JSONArray(ipv4));
                item.put("mtu", networkIF.getMTU());
                item.put("linkSpeedBps", networkIF.getSpeed());
                item.put("rxBytesTotal", recv);
                item.put("txBytesTotal", sent);
                item.put("rxPacketsTotal", networkIF.getPacketsRecv());
                item.put("txPacketsTotal", networkIF.getPacketsSent());
                item.put("inErrors", networkIF.getInErrors());
                item.put("outErrors", networkIF.getOutErrors());

                if (null != previous && elapsedSeconds > 0.0d) {
                    item.put("rxBytesPerSec", rateToDouble(recv, previous[0], elapsedSeconds));
                    item.put("txBytesPerSec", rateToDouble(sent, previous[1], elapsedSeconds));
                }

                this.lastInterfaceCounters.put(name, new long[] { recv, sent });
                interfaces.put(item);
            }
            catch (Throwable e) {
                // 单块网卡读取失败不影响其他网卡
                Logger.w(this.getClass(), "#buildNetworkMetrics - 跳过网卡: " + e.getMessage());
            }
        }

        // 清理已下线的网卡采样点，避免 Map 无限增长
        this.lastInterfaceCounters.keySet().retainAll(seenNames);

        if (elapsedSeconds > 0.0d && this.lastSampleTime > 0L) {
            network.put("rxBytesPerSec", rateToDouble(aggregateRecv, this.lastNetRecv, elapsedSeconds));
            network.put("txBytesPerSec", rateToDouble(aggregateSent, this.lastNetSent, elapsedSeconds));
        }
        this.lastNetRecv = aggregateRecv;
        this.lastNetSent = aggregateSent;

        network.put("rxBytesTotal", aggregateRecv);
        network.put("txBytesTotal", aggregateSent);
        network.put("rxPacketsTotal", aggregateRxPackets);
        network.put("txPacketsTotal", aggregateTxPackets);

        // 只保留有流量的网卡，按瞬时速率排序后截断
        List<JSONObject> ordered = new ArrayList<>();
        for (int i = 0; i < interfaces.length(); ++i) {
            JSONObject item = interfaces.getJSONObject(i);
            double rate = item.optDouble("rxBytesPerSec", 0.0d) + item.optDouble("txBytesPerSec", 0.0d);
            if (rate > 0.0d) {
                ordered.add(item);
            }
        }
        Collections.sort(ordered, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right) {
                double leftRate = left.optDouble("rxBytesPerSec", 0.0d) + left.optDouble("txBytesPerSec", 0.0d);
                double rightRate = right.optDouble("rxBytesPerSec", 0.0d) + right.optDouble("txBytesPerSec", 0.0d);
                return Double.compare(rightRate, leftRate);
            }
        });

        JSONArray active = new JSONArray();
        for (int i = 0; i < ordered.size() && i < MAX_NETWORK_DETAIL; ++i) {
            active.put(ordered.get(i));
        }
        network.put("activeInterfaces", active);

        return network;
    }

    private JSONObject buildDiskMetrics(double elapsedSeconds) {
        JSONObject disk = new JSONObject();

        if (null != this.hardware) {
            List<HWDiskStore> list = safe(this.hardware::getDiskStores, null);
            if (null != list) {
                long totalRead = 0L;
                long totalWrite = 0L;
                JSONArray drives = new JSONArray();
                Set<String> seenNames = new HashSet<>();

                for (HWDiskStore diskStore : list) {
                    try {
                        diskStore.updateAttributes();

                        long read = diskStore.getReadBytes();
                        long write = diskStore.getWriteBytes();
                        totalRead += read;
                        totalWrite += write;

                        String name = nullToEmpty(diskStore.getName());
                        seenNames.add(name);
                        long[] previous = this.lastDiskCounters.get(name);

                        JSONObject item = new JSONObject();
                        item.put("name", name);
                        item.put("model", nullToEmpty(diskStore.getModel()));
                        item.put("sizeBytes", diskStore.getSize());
                        item.put("readBytesTotal", read);
                        item.put("writeBytesTotal", write);
                        item.put("readsTotal", diskStore.getReads());
                        item.put("writesTotal", diskStore.getWrites());
                        item.put("queueLength", diskStore.getCurrentQueueLength());

                        // 单盘速率必须用该盘自己的上次采样点求差，不能用聚合值
                        if (null != previous && elapsedSeconds > 0.0d) {
                            item.put("readBytesPerSec", rateToDouble(read, previous[0], elapsedSeconds));
                            item.put("writeBytesPerSec", rateToDouble(write, previous[1], elapsedSeconds));
                        }

                        this.lastDiskCounters.put(name, new long[] { read, write });
                        drives.put(item);
                    }
                    catch (Throwable e) {
                        Logger.w(this.getClass(), "#buildDiskMetrics - 跳过磁盘: " + e.getMessage());
                    }
                }

                this.lastDiskCounters.keySet().retainAll(seenNames);

                if (elapsedSeconds > 0.0d && this.lastSampleTime > 0L) {
                    disk.put("readBytesPerSec", rateToDouble(totalRead, this.lastDiskRead, elapsedSeconds));
                    disk.put("writeBytesPerSec", rateToDouble(totalWrite, this.lastDiskWrite, elapsedSeconds));
                }
                this.lastDiskRead = totalRead;
                this.lastDiskWrite = totalWrite;

                disk.put("readBytesTotal", totalRead);
                disk.put("writeBytesTotal", totalWrite);
                disk.put("drives", drives);
            }
        }

        disk.put("partitions", this.buildPartitionUsage());
        return disk;
    }

    /**
     * 分区容量与使用率。
     *
     * <p>优先用 oshi 的文件卷信息，取不到时回退到 Java 原生 {@link File}。</p>
     */
    private JSONArray buildPartitionUsage() {
        List<OSFileStore> stores = (null != this.operatingSystem)
                ? safe(() -> this.operatingSystem.getFileSystem().getFileStores(), null) : null;

        List<JSONObject> list = new ArrayList<>();

        if (null != stores) {
            for (OSFileStore store : stores) {
                try {
                    long total = store.getTotalSpace();
                    // 过滤 /dev、/sys 之类的伪文件系统与挂载点，它们容量极小且使用率无意义
                    if (total < MIN_MEANINGFUL_VOLUME_BYTES) {
                        continue;
                    }

                    JSONObject item = new JSONObject();
                    item.put("name", nullToEmpty(store.getName()));
                    item.put("mount", nullToEmpty(store.getMount()));
                    item.put("type", nullToEmpty(store.getType()));
                    item.put("local", store.isLocal());
                    item.put("totalBytes", total);
                    item.put("usableBytes", store.getUsableSpace());
                    item.put("freeBytes", store.getFreeSpace());
                    item.put("usage", round(1.0d - (double) store.getUsableSpace() / (double) total, 4));
                    list.add(item);
                }
                catch (Throwable e) {
                    Logger.w(this.getClass(), "#buildPartitionUsage - 跳过分区: " + e.getMessage());
                }
            }
        }

        if (list.isEmpty()) {
            list.add(this.buildFallbackRootPartition());
        }

        // 同一物理卷可能有多个挂载视图（如 macOS 的 APFS 卷组会把系统卷挂到多个位置，
        // 容量与可用空间完全一致），按「容量 + 可用空间 + 类型」去重并保留挂载路径最短的一条。
        Map<String, JSONObject> deduped = new LinkedHashMap<>();
        for (JSONObject item : list) {
            String key = item.optLong("totalBytes", 0L) + "|"
                    + item.optLong("usableBytes", 0L) + "|"
                    + item.optString("type", "");
            JSONObject exists = deduped.get(key);
            if (null == exists
                    || item.optString("mount", "").length() < exists.optString("mount", "").length()) {
                deduped.put(key, item);
            }
        }

        List<JSONObject> unique = new ArrayList<>(deduped.values());

        // 按容量倒序，只保留最大的若干条，避免容器里出现几十个绑定挂载
        Collections.sort(unique, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right) {
                return Long.compare(right.optLong("totalBytes", 0L), left.optLong("totalBytes", 0L));
            }
        });

        JSONArray array = new JSONArray();
        for (int i = 0; i < unique.size() && i < MAX_PARTITION_DETAIL; ++i) {
            array.put(unique.get(i));
        }
        return array;
    }

    private JSONObject buildFallbackRootPartition() {
        File root = new File("/");
        long total = root.getTotalSpace();

        JSONObject item = new JSONObject();
        item.put("name", "root");
        item.put("mount", "/");
        item.put("type", "");
        item.put("local", true);
        item.put("totalBytes", total);
        item.put("usableBytes", root.getUsableSpace());
        item.put("freeBytes", root.getFreeSpace());
        item.put("usage", (total <= 0L) ? 0.0d : round(1.0d - (double) root.getUsableSpace() / (double) total, 4));
        return item;
    }

    private JSONObject buildSystemMetrics() {
        JSONObject system = new JSONObject();

        if (null != this.operatingSystem) {
            system.put("processCount", safe(this.operatingSystem::getProcessCount, 0));
            system.put("threadCount", safe(this.operatingSystem::getThreadCount, 0));
            system.put("uptimeSeconds", safe(this.operatingSystem::getSystemUptime, 0L));
            system.put("bootTime", bootTimeMillis());

            FileSystem fileSystem = safe(() -> this.operatingSystem.getFileSystem(), null);
            if (null != fileSystem) {
                system.put("openFileDescriptors", safe(fileSystem::getOpenFileDescriptors, -1L));
                system.put("maxFileDescriptors", safe(fileSystem::getMaxFileDescriptors, -1L));
            }
        }

        return system;
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    /**
     * 主机启动时间（毫秒）。
     *
     * <p>oshi 的 {@code getSystemBootTime()} 返回的是秒级时间戳，前端统一按毫秒处理，这里换算一次。</p>
     */
    private long bootTimeMillis() {
        if (null == this.operatingSystem) {
            return 0L;
        }
        long seconds = safe(this.operatingSystem::getSystemBootTime, 0L);
        return (seconds > 0L) ? (seconds * 1000L) : 0L;
    }

    /**
     * 计算速率（单位：字节 / 秒）。
     *
     * <p>计数器回绕或进程重启后 current 可能小于 previous，此时按 0 处理。</p>
     */
    private static double rateToDouble(long current, long previous, double seconds) {
        long delta = current - previous;
        if (delta < 0L) {
            delta = 0L;
        }
        return round(delta / seconds, 2);
    }

    /**
     * 读取控制台进程的 CPU 占用。
     *
     * <p>{@code com.sun.management.OperatingSystemMXBean#getProcessCpuLoad} 在 JDK 8 与 JDK 11+
     * 上均可用；个别精简 JRE 不含该扩展接口，因此用反射调用并降级为 0。</p>
     */
    private static double readProcessCpuLoad() {
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            Method method = bean.getClass().getMethod("getProcessCpuLoad");
            Object value = method.invoke(bean);
            if (value instanceof Double) {
                double load = (Double) value;
                return (load < 0.0d) ? 0.0d : load;
            }
        }
        catch (Throwable e) {
            // 忽略，返回 0
        }
        return 0.0d;
    }

    private static double round(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        double factor = Math.pow(10.0d, scale);
        return Math.round(value * factor) / factor;
    }

    private static String systemProperty(String key) {
        String value = System.getProperty(key);
        return (null == value) ? "" : value;
    }

    private static String resolveHostName() {
        try {
            return nullToEmpty(InetAddress.getLocalHost().getHostName());
        }
        catch (Throwable e) {
            return nullToEmpty(System.getenv("HOSTNAME"));
        }
    }

    private static String formatMac(byte[] mac) {
        if (null == mac || mac.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mac.length; ++i) {
            if (i > 0) {
                builder.append(':');
            }
            int value = mac[i] & 0xFF;
            builder.append(Character.forDigit(value >>> 4, 16));
            builder.append(Character.forDigit(value & 0x0F, 16));
        }
        return builder.toString().toUpperCase();
    }

    private static long parsePid(String runtimeName) {
        if (null == runtimeName) {
            return 0L;
        }
        int index = runtimeName.indexOf('@');
        String pid = (index > 0) ? runtimeName.substring(0, index) : runtimeName;
        try {
            return Long.parseLong(pid.trim());
        }
        catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String nullToEmpty(String value) {
        return (null == value) ? "" : value.trim();
    }

    /**
     * 统一的容错执行器。
     *
     * <p>oshi 的每个取值都可能因权限或平台差异抛异常，这里逐项降级，
     * 避免单点失败导致整份报告不可用。</p>
     */
    private static <T> T safe(Getter<T> getter, T fallback) {
        try {
            T value = getter.get();
            return (null == value) ? fallback : value;
        }
        catch (Throwable e) {
            return fallback;
        }
    }

    /** 可抛异常的取值器，便于在 lambda 里直接调用带受检异常的方法。 */
    private interface Getter<T> {
        T get() throws Exception;
    }
}
