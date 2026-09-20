/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.tool;

import cell.util.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务检测工具。
 *
 * 只做 TCP 层连通性判断：完整走一次 Talk 握手虽然能确认“对端确实是一个 Cell 节点”，
 * 但单次最长要等 11 秒，无法在列表接口与秒级定时任务里使用；而“节点是否活着”这一判断
 * 由节点自身的上报心跳（{@link cube.console.mgmt.NodeHeartbeat}）负责，端口探测只用于
 * 覆盖节点刚启动、首份报告尚未到达的窗口期。
 */
public final class Detector {

    /**
     * 端口连通性探测的连接超时时间，单位：毫秒。
     */
    private final static int PROBE_TIMEOUT = 1500;

    /**
     * 端口连通性探测结果的缓存时长，单位：毫秒。
     */
    private final static long PROBE_CACHE_TIMEOUT = 3000L;

    /**
     * 端口连通性探测结果缓存，键为 “地址:端口”。
     */
    private final static Map<String, ProbeResult> probeCache = new ConcurrentHashMap<>();

    private Detector() {
    }

    /**
     * 探测指定地址的端口是否能建立 TCP 连接。
     *
     * 结果会按 {@link #PROBE_CACHE_TIMEOUT} 缓存，避免同一节点在短时间内被反复探测。
     *
     * @param host 目标地址。
     * @param port 目标端口。
     * @return 端口可连接返回 true。
     */
    public static boolean isPortReachable(String host, int port) {
        if (null == host || host.length() == 0 || port <= 0 || port > 65535) {
            return false;
        }

        // "0.0.0.0" / "*" / "::" 表示监听全部网卡，从本机探测时换成回环地址
        String address = host;
        if (address.equals("0.0.0.0") || address.equals("*") || address.equals("::")) {
            address = "127.0.0.1";
        }

        String key = address + ":" + port;

        long now = System.currentTimeMillis();
        ProbeResult cached = probeCache.get(key);
        if (null != cached && (now - cached.timestamp) < PROBE_CACHE_TIMEOUT) {
            return cached.reachable;
        }

        boolean reachable = false;
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), PROBE_TIMEOUT);
            reachable = socket.isConnected();
        } catch (IOException e) {
            // 节点未监听或不可达
            Logger.d(Detector.class, "#isPortReachable - " + key + " is not reachable");
        } catch (SecurityException e) {
            // 安全管理器拒绝连接
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Nothing
            }
        }

        probeCache.put(key, new ProbeResult(now, reachable));

        return reachable;
    }

    /**
     * 端口连通性探测结果。
     */
    private final static class ProbeResult {

        public final long timestamp;

        public final boolean reachable;

        public ProbeResult(long timestamp, boolean reachable) {
            this.timestamp = timestamp;
            this.reachable = reachable;
        }
    }
}
