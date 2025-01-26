/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
