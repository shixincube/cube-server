/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 访问控制器。
 * 用于控制外部访问规则，避免高频度对 HTTP 接口访问。
 */
public class AccessController {

    /**
     * 每个 IP 的最小访问间隔，单位：毫秒。
     */
    private long eachIPInterval = 0;

    private long lastTimestamp = 0;

    private ConcurrentHashMap<String, Long> lastRequestTimeMap;

    public AccessController() {
        this.lastRequestTimeMap = new ConcurrentHashMap<>();
    }

    public void setEachIPInterval(long value) {
        this.eachIPInterval = value;
    }

    public boolean filter(HttpServletRequest request) {
        this.lastTimestamp = System.currentTimeMillis();

        if (this.eachIPInterval > 0) {
            String ip = request.getRemoteAddr();

            if (this.lastRequestTimeMap.containsKey(ip)) {
                long last = this.lastRequestTimeMap.get(ip);
                if (this.lastTimestamp - last < this.eachIPInterval) {
                    // 间隔时间太短
                    return false;
                }
            }

            this.lastRequestTimeMap.put(ip, this.lastTimestamp);
        }

        return true;
    }
}
