/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.hub;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 控制器，用于控制访问请求。
 */
public class Controller {

    private ConcurrentHashMap<String, VisitLog> visitLogMap;

    public Controller() {
        this.visitLogMap = new ConcurrentHashMap<>();
    }

    public boolean verify(String channelCode, String path, long coolingTime) {
        String key = channelCode + "_" + path;

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
