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
import cube.common.entity.Contact;
import cube.hub.dao.ChannelCode;
import cube.hub.event.Event;
import cube.hub.event.LoginQRCodeEvent;
import cube.hub.event.ReportEvent;
import cube.hub.signal.LoginQRCodeSignal;
import org.json.JSONObject;

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

    /**
     * 最近请求登录的通道码映射。
     */
    private Map<String, LoginQRCodeEvent> recentLoginEventMap;

    private WeChatHub() {
        this.reportMap = new ConcurrentHashMap<>();
        this.recentLoginEventMap = new ConcurrentHashMap<>();

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

    /**
     * 获取报告
     *
     * @param pretenderId
     * @return
     */
    public ReportEvent getReport(Long pretenderId) {
        return this.reportMap.get(pretenderId);
    }

    /**
     * 开启通道。
     *
     * @param channelCode
     * @return
     */
    public Event openChannel(ChannelCode channelCode) {
        ChannelManager channelManager = this.service.getChannelManager();
        // 校验通道码
        String accountId = channelManager.getAccountId(channelCode.code);
        if (null != accountId) {
            // 已经绑定了账号
            Logger.d(this.getClass(), "#openChannel - Allocated account on channel: " + channelCode.code);
            return null;
        }

        LoginQRCodeEvent loginQRCodeEvent = this.recentLoginEventMap.get(channelCode.code);
        if (null != loginQRCodeEvent) {
            if (System.currentTimeMillis() - loginQRCodeEvent.getTimestamp() < 30 * 1000) {
                // 30 秒内不再更新二维码
                return loginQRCodeEvent;
            }

            this.recentLoginEventMap.remove(channelCode.code);
        }

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
            Logger.w(this.getClass(), "#openChannel - Can NOT find idle app");
            return null;
        }

        // 获取空闲端的登录二维码文件
        SignalController signalController = this.service.getSignalController();
        // 发送信令并等待响应事件
        Event event = signalController.transmitSyncEvent(id, new LoginQRCodeSignal(channelCode.code));
        if (null == event) {
            Logger.w(this.getClass(), "#openChannel - No login QR code event");
            return null;
        }

        loginQRCodeEvent = (LoginQRCodeEvent) event;
        loginQRCodeEvent.setTimestamp(System.currentTimeMillis());
        loginQRCodeEvent.setPretenderId(id);

        if (null != loginQRCodeEvent.getFileLabel()) {
            // 有二维码文件，保存记录以便扫描确认
            this.recentLoginEventMap.put(channelCode.code, loginQRCodeEvent);
        }

        return event;
    }

    /**
     * 关闭通道。
     *
     * @param channelManager
     * @param channelCode
     * @return
     */
    public Contact closeChannel(ChannelManager channelManager, ChannelCode channelCode) {
        // 校验通道码
        String accountId = channelManager.getAccountId(channelCode.code);
        if (null == accountId) {
            // 没有绑定账号
            Logger.d(this.getClass(), "#closeChannel - Not allocated account on channel: " + channelCode.code);
            return null;
        }

        for (Map.Entry<Long, ReportEvent> entry : this.reportMap.entrySet()) {

        }

        return null;
    }

    public void updateReport(ReportEvent reportEvent) {
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#updateReport - report from " +
                    reportEvent.getDescription().getPretender().getId() +
                    reportEvent.toString());
        }

        this.reportMap.put(reportEvent.getDescription().getPretender().getId(),
                reportEvent);
    }

    /**
     * 报告已分配账号。
     *
     * @param pretenderId
     * @param channelCode
     * @param account
     */
    public void reportAlloc(Long pretenderId, String channelCode, Contact account) {
        String weChatId = this.getWeChatId(account);

        ReportEvent reportEvent = this.reportMap.get(pretenderId);
        if (null != reportEvent) {
            reportEvent.putChannelCode(channelCode, account);
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#reportAlloc" + reportEvent.toString());
            }
        }

        this.service.getChannelManager().setAccountId(channelCode, weChatId, pretenderId);
    }

    private String getWeChatId(Contact account) {
        JSONObject ctx = account.getContext();
        if (null == ctx) {
            return null;
        }

        return ctx.has("id") ? ctx.getString("id") : null;
    }
}
