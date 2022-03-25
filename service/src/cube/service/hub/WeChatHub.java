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
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.hub.Product;
import cube.hub.data.ChannelCode;
import cube.hub.data.wechat.PlainMessage;
import cube.hub.event.*;
import cube.hub.signal.GetMessagesSignal;
import cube.hub.signal.LoginQRCodeSignal;
import cube.hub.signal.LogoutSignal;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    public synchronized Event openChannel(ChannelCode channelCode) {
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
            if (System.currentTimeMillis() - loginQRCodeEvent.getTimestamp() < 60 * 1000) {
                // 60 秒内不再更新二维码
                return loginQRCodeEvent;
            }
        }

        // 插入数据，防止重复申请
        LoginQRCodeEvent preEvent = new LoginQRCodeEvent(0, channelCode.code, null);
        this.recentLoginEventMap.put(channelCode.code, preEvent);

        // 找到最少服务数量的客户端
        int minNum = Integer.MAX_VALUE;
        Long pretenderId = null;
        Iterator<Map.Entry<Long, ReportEvent>> iter = this.reportMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, ReportEvent> entry = iter.next();
            ReportEvent report = entry.getValue();
            int num = report.getTotalAppNum() - report.getIdleAppNum();
            if (num < minNum) {
                minNum = num;
                pretenderId = entry.getKey();
            }
        }

        if (null == pretenderId) {
            Logger.w(this.getClass(), "#openChannel - Can NOT find idle seeker");
            this.recentLoginEventMap.remove(channelCode.code);
            return null;
        }

        // 获取空闲端的登录二维码文件
        SignalController signalController = this.service.getSignalController();
        // 发送信令并等待响应事件
        Event event = signalController.transmitSyncEvent(pretenderId, new LoginQRCodeSignal(channelCode.code));
        if (null == event) {
            Logger.w(this.getClass(), "#openChannel - No login QR code event");
            this.recentLoginEventMap.remove(channelCode.code);
            return null;
        }

        loginQRCodeEvent = (LoginQRCodeEvent) event;
        loginQRCodeEvent.setTimestamp(System.currentTimeMillis());
        loginQRCodeEvent.setPretenderId(pretenderId);

        if (null != loginQRCodeEvent.getFileLabel()) {
            // 有二维码文件，保存记录放置重复申请
            this.recentLoginEventMap.put(channelCode.code, loginQRCodeEvent);
        }
        else {
            // 没有获取到二维码文件
            this.recentLoginEventMap.remove(channelCode.code);
        }

        return event;
    }

    /**
     * 关闭通道。
     *
     * @param channelCode
     * @return
     */
    public synchronized Event closeChannel(ChannelCode channelCode) {
        ChannelManager channelManager = this.service.getChannelManager();
        // 校验通道码
        String accountId = channelManager.getAccountId(channelCode.code);
        if (null == accountId) {
            // 没有绑定账号
            Logger.d(this.getClass(), "#closeChannel - Not allocated account on channel: " + channelCode.code);
            return null;
        }

        ReportEvent report = null;
        Contact account = null;
        Long pretenderId = null;
        for (Map.Entry<Long, ReportEvent> entry : this.reportMap.entrySet()) {
            report = entry.getValue();
            if (report.hasChannelCode(channelCode.code)) {
                account = report.getAccount(channelCode.code);
                pretenderId = entry.getKey();
                break;
            }
        }

        if (null == pretenderId) {
            Logger.w(this.getClass(), "#closeChannel - Can NOT find seeker: " + channelCode.code);
            return null;
        }

        SignalController signalController = this.service.getSignalController();
        // 发送信令并等待响应事件
        Event event = signalController.transmitSyncEvent(pretenderId, new LogoutSignal(account, channelCode.code));
        if (null == event) {
            Logger.w(this.getClass(), "#closeChannel - No logout event");
            return null;
        }

        if (event instanceof LogoutEvent) {
            // 清空账号分配数据
            channelManager.freeAccountId(channelCode.code);
            // 删除报告里的数据
            report.removeManagedAccount(account);
            report.removeChannelCode(channelCode.code);
        }

        return event;
    }

    public void updateReport(ReportEvent reportEvent) {
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#updateReport - report from " +
                    reportEvent.getDescription().getPretender().getId() +
                    reportEvent.toString());
        }

        // TODO 进行数据对比，因为可能客户端已经退出了，但是没有发生 Logout 事件

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

        // 分配通道账号
        this.service.getChannelManager().allocAccountId(channelCode, weChatId, pretenderId);

        // 更新账号
        this.service.getChannelManager().updateAccount(account, weChatId, channelCode,
                Product.WeChat);

        // 移除临时存储变量
        this.recentLoginEventMap.remove(channelCode);
    }

    /**
     * 提交消息数据。
     *
     * @param event
     * @return
     */
    public boolean submitMessages(SubmitMessagesEvent event) {
        Contact account = event.getAccount();
        String accountId = this.getWeChatId(account);

        // 获取当前所属的通道码
        String channelCode = this.service.getChannelManager().getChannelCodeWithAccountId(accountId);
        if (null == channelCode) {
            Logger.w(this.getClass(), "Can NOT find channel code with Account ID: " + accountId);
            return false;
        }

        // 判断消息来源
        Contact partner = event.getPartner();
        Group group = event.getGroup();

        if (null != partner) {
            String partnerId = getWeChatId(partner);
            // 获取当前已存储的消息
            List<Message> messageList = this.service.getChannelManager().getMessagesByPartner(channelCode,
                    accountId, partnerId);
            // 匹配新消息
            List<Message> newMessageList = matchNewMessages(messageList, event.getMessages());

            for (Message message : newMessageList) {
                // 补充发件人 ID
                Contact sender = message.getSender();
                if (sender.getName().equals(account.getName())) {
                    setWeChatId(sender, accountId);
                }
                else {
                    setWeChatId(sender, partnerId);
                }

                this.service.getChannelManager().appendMessageByPartner(channelCode,
                        accountId, partnerId, getWeChatId(sender), message);
            }
        }
        else if (null != group) {
            // 获取当前已存储的消息
            List<Message> messageList = this.service.getChannelManager().getMessagesByGroup(channelCode,
                    accountId, group.getName());
            // 匹配新消息
            List<Message> newMessageList = matchNewMessages(messageList, event.getMessages());

            for (Message message : newMessageList) {
                this.service.getChannelManager().appendMessageByGroup(channelCode,
                        accountId, group.getName(), message.getSender().getName(), message);
            }
        }

        return true;
    }

    public SubmitMessagesEvent getMessages(GetMessagesSignal signal) {
        return null;
    }

    private String getWeChatId(Contact account) {
        JSONObject ctx = account.getContext();
        if (null == ctx) {
            return null;
        }

        return ctx.has("id") ? ctx.getString("id") : null;
    }

    private void setWeChatId(Contact contact, String id) {
        JSONObject ctx = contact.getContext();
        if (null == ctx) {
            ctx = new JSONObject();
            contact.setContext(ctx);
        }

        ctx.put("id", id);
    }

    private List<Message> matchNewMessages(List<Message> baseList, List<Message> currentList) {
        if (baseList.isEmpty()) {
            return currentList;
        }

        ArrayList<PlainMessage> basePlainList = new ArrayList<>();
        for (Message message : baseList) {
            basePlainList.add(PlainMessage.create(message));
        }

        List<Message> list = new ArrayList<>();

        for (Message message : currentList) {
            PlainMessage plainMessage = PlainMessage.create(message);
            if (!basePlainList.contains(plainMessage)) {
                // 在基础列表里没有找到该消息
                list.add(message);
            }
        }

        return list;
    }
}
