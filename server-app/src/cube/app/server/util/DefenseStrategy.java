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

package cube.app.server.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 防御策略辅助函数。
 */
public class DefenseStrategy {

    private long coolingTime;

    private ConcurrentHashMap<String, VisitLog> visitLogMap;

    public DefenseStrategy(long coolingTime) {
        this.coolingTime = coolingTime;
        this.visitLogMap = new ConcurrentHashMap<>();
    }

    public boolean verify(String code) {
        String key = code;

        VisitLog visitLog = this.visitLogMap.get(key);
        if (null == visitLog) {
            visitLog = new VisitLog();
            this.visitLogMap.put(key, visitLog);
            return true;
        }

        return visitLog.timing(coolingTime);
    }

    protected class VisitLog {

        private long timingPoint1;

        private long timingPoint2;

        protected VisitLog() {
            this.timingPoint1 = System.currentTimeMillis();
        }

        protected synchronized boolean timing(long coolingTime) {
            this.timingPoint2 = this.timingPoint1;
            this.timingPoint1 = System.currentTimeMillis();

            if (this.timingPoint1 - this.timingPoint2 < coolingTime) {
                return false;
            }
            else {
                return true;
            }
        }
    }
}
