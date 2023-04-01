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
