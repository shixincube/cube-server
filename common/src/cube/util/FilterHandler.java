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

package cube.util;

import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 具备基础访问过滤能力的 HTTP 处理器。
 */
public class FilterHandler extends CrossDomainHandler {

    private final static Map<String, HostRecord> sHostRecordMap = new ConcurrentHashMap<>();

    private volatile boolean completed = false;

    private long lastTimestamp = 0;

    private int minInterval = 0;

    /**
     * 单个访问IP每秒允许被访问次数。
     */
    private int numPerSecondForOneHost = 0;

    public FilterHandler() {
        super();
    }

    protected boolean isCompleted(HttpServletRequest request) {
        if (this.completed) {
            return this.completed;
        }

        Object value = request.getAttribute("_completed");
        if (null == value) {
            return false;
        }

        return ((Boolean) value).booleanValue();
    }

    public void setMinInterval(int value) {
        this.minInterval = value;
    }

    public void setNumPerSecondForOneHost(int value) {
        if (value < 0) {
            return;
        }

        this.numPerSecondForOneHost = value;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        this.completed = false;

        // 检查最小访问间隔
        if (this.checkMinInterval(response)) {
            return;
        }

        // 检查每秒访问次数
        this.checkNumPerSecondForOneHost(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        this.completed = false;

        // 检查最小访问间隔
        if (this.checkMinInterval(response)) {
            return;
        }

        // 检查每秒访问次数
        this.checkNumPerSecondForOneHost(request, response);
    }

    private boolean checkMinInterval(HttpServletResponse response) {
        if (this.minInterval > 0) {
            long now = System.currentTimeMillis();
            if (now - this.lastTimestamp < this.minInterval) {
                // 访问间隔小于最小间隔
                this.respond(response, HttpStatus.SERVICE_UNAVAILABLE_503);
                this.complete();
                this.completed = true;
            }

            this.lastTimestamp = now;
        }

        return this.completed;
    }

    private boolean checkNumPerSecondForOneHost(HttpServletRequest request, HttpServletResponse response) {
        if (0 == this.numPerSecondForOneHost) {
            return false;
        }

        long now = System.currentTimeMillis();

        String host = request.getRemoteHost();
        HostRecord hostRecord = sHostRecordMap.get(host);
        if (null == hostRecord) {
            hostRecord = new HostRecord(host);
            hostRecord.firstTimestamp = now;
            sHostRecordMap.put(host, hostRecord);
        }

        hostRecord.lastTimestamp = now;

        // 计算一秒内的访问次数
        int count = hostRecord.countLastOneSecond(now);
        if (count > this.numPerSecondForOneHost) {
            // 最近一秒内的访问次数大于限制
            this.respond(response, HttpStatus.SERVICE_UNAVAILABLE_503);
            this.complete();
            request.setAttribute("_completed", true);
            return true;
        }
        else {
            return false;
        }
    }


    private class HostRecord {

        protected String address;

        protected long firstTimestamp;

        protected long lastTimestamp;

        protected List<Long> timestampList = new LinkedList<>();

        protected HostRecord(String address) {
            this.address = address;
        }

        protected int countLastOneSecond(long now) {
            synchronized (this.timestampList) {
                if (this.timestampList.isEmpty()) {
                    return 0;
                }

                int count = 0;

                int posIndex = -1;

                for (int i = this.timestampList.size() - 1; i >= 0; --i) {
                    long time = this.timestampList.get(i);
                    if (now - time <= 1000) {
                        ++count;
                    }
                    else {
                        posIndex = i;
                        break;
                    }
                }

                if (posIndex >= 0) {
                    for (int i = 0; i < posIndex; ++i) {
                        this.timestampList.remove(0);
                    }
                }

                this.timestampList.add(now);

                return count;
            }
        }
    }
}
