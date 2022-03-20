/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.hub;

import cell.util.log.Logger;
import cube.hub.event.Event;
import cube.hub.event.ReportEvent;
import cube.hub.signal.LoginQRCodeSignal;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WeChat HUB
 */
public class WeChatHub {

    private final static WeChatHub instance = new WeChatHub();

    private File workPath = new File("hub/");

    private HubService service;

    private Map<Long, ReportEvent> reportMap;

    private WeChatHub() {
        this.reportMap = new ConcurrentHashMap<>();
        if (!this.workPath.exists()) {
            this.workPath.mkdirs();
        }
    }

    public final static WeChatHub getInstance() {
        return WeChatHub.instance;
    }

    public void setService(HubService service) {
        this.service = service;
    }

    public Event openChannel() {
        // 找到最少服务数量的客户端
        int minNum = Integer.MAX_VALUE;
        Long id = null;
        Iterator<Map.Entry<Long, ReportEvent>> iter = this.reportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, ReportEvent> entry = iter.next();
            ReportEvent report = entry.getValue();
            int num = report.getTotalAppNum() - report.getIdleAppNum();
            if (num < minNum) {
                minNum = num;
                id = entry.getKey();
            }
        }

        if (null == id) {
            Logger.w(this.getClass(), "Can NOT find idle app");
            return null;
        }

        // 获取空闲端的登录二维码文件
        SignalController signalController = this.service.getSignalController();
        // 发送信令并等待响应事件
        Event event = signalController.transmitSyncEvent(id, new LoginQRCodeSignal());
        if (null == event) {
            Logger.w(this.getClass(), "No login QR code event");
            return null;
        }

        return event;
    }

    public void updateReport(ReportEvent reportEvent) {
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#updateReport - report from " +
                    reportEvent.getDescription().getPretender().getId());
        }

        this.reportMap.put(reportEvent.getDescription().getPretender().getId(),
                reportEvent);
    }
}
