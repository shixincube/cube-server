/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点心跳表。
 *
 * 调度机与服务单元并不向控制台发送独立的“心跳”报文，而是周期性地提交日志、JVM 与性能报告，
 * 每份报告都携带节点名（reporter）。因此“控制台最近一次收到某节点报告的时间”本身就是节点的存活证据，
 * 且不依赖节点的工作目录、启动脚本的 tag 参数，也不要求控制台能反向连上节点的端口。
 */
public final class NodeHeartbeat {

    /**
     * 判定节点仍然存活的超时时间。
     *
     * 节点默认每 60 秒提交一次 JVM / 性能报告，这里取 3 倍间隔，可容忍连续两次上报失败。
     */
    public final static long ALIVE_TIMEOUT = 3L * 60L * 1000L;

    private final static NodeHeartbeat instance = new NodeHeartbeat();

    /**
     * 节点名 -> 最近一次收到报告的时刻。
     */
    private final Map<String, Long> timestamps;

    private NodeHeartbeat() {
        this.timestamps = new ConcurrentHashMap<>();
    }

    public final static NodeHeartbeat getInstance() {
        return NodeHeartbeat.instance;
    }

    /**
     * 记录一次节点上报。
     *
     * 记录的是控制台的接收时刻而不是报告自身的时间戳，以避免节点与控制台时钟不一致造成误判。
     *
     * @param reporter 节点名。
     */
    public void trace(String reporter) {
        if (null == reporter || reporter.length() == 0) {
            return;
        }

        this.timestamps.put(reporter, System.currentTimeMillis());
    }

    /**
     * 返回最近一次收到指定节点报告的时刻，从未收到过返回 0。
     *
     * @param reporter 节点名。
     * @return 时间戳。
     */
    public long getLastTimestamp(String reporter) {
        if (null == reporter) {
            return 0;
        }

        Long time = this.timestamps.get(reporter);
        return (null == time) ? 0 : time.longValue();
    }

    /**
     * 判断节点是否在存活窗口内上报过数据。
     *
     * @param reporter 节点名。
     * @param now 当前时间戳。
     * @return 存活返回 true。
     */
    public boolean isAlive(String reporter, long now) {
        long time = this.getLastTimestamp(reporter);
        return time > 0 && (now - time) <= ALIVE_TIMEOUT;
    }
}
