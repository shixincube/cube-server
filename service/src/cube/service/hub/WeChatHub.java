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
import cube.common.entity.*;
import cube.hub.Product;
import cube.hub.data.ChannelCode;
import cube.hub.data.DataHelper;
import cube.hub.data.wechat.PlainMessage;
import cube.hub.event.*;
import cube.hub.signal.Signal;
import cube.hub.signal.*;

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

    private HubService service;

    private Map<Long, ReportEvent> reportMap;

    /**
     * 最近请求登录的通道码映射。
     */
    private Map<String, LoginQRCodeEvent> recentLoginEventMap;

    private WeChatHub() {
        this.reportMap = new ConcurrentHashMap<>();
        this.recentLoginEventMap = new ConcurrentHashMap<>();
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

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 更新账号
                for (Contact account : reportEvent.getManagedAccounts()) {
                    String channelCode = service.getChannelManager().getChannelCodeWithAccountId(account.getExternalId());
                    if (null == channelCode) {
                        continue;
                    }

                    service.getChannelManager().updateAccount(channelCode, account, reportEvent.getProduct());
                }
            }
        });
    }

    /**
     * 报告已分配账号。
     *
     * @param pretenderId
     * @param channelCode
     * @param account
     */
    public void reportAlloc(Long pretenderId, String channelCode, Contact account) {
        String weChatId = account.getExternalId();

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
        this.service.getChannelManager().updateAccount(channelCode, account, Product.WeChat);

        // 移除临时存储变量
        this.recentLoginEventMap.remove(channelCode);
    }

    /**
     * 更新群组数据。
     *
     * @param event
     */
    public void updateGroup(GroupDataEvent event) {
        Contact account = event.getAccount();
        Group group = event.getGroup();
        this.service.getChannelManager().updateGroup(account, group, event.getProduct());
    }

    /**
     * 提交消息数据。
     *
     * @param event
     * @return
     */
    public boolean submitMessages(SubmitMessagesEvent event) {
        Contact account = event.getAccount();
        String accountId = account.getExternalId();

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
            String partnerId = partner.getExternalId();

            // 更新通讯录信息
            this.service.getChannelManager().updateContactBook(accountId, event.getProduct(), partner, false);

            // 获取当前已存储的消息
            List<Message> messageList = this.service.getChannelManager().getMessagesByPartner(channelCode,
                    accountId, partnerId, 0, 20);
            // 匹配新消息
            List<Message> newMessageList = matchNewMessages(messageList, event.getMessages());

            for (Message message : newMessageList) {
                // 补充发件人 ID
                Contact sender = message.getSender();
                if (sender.getName().equals(account.getName())) {
                    sender.setExternalId(accountId);
                }
                else {
                    sender.setExternalId(partnerId);
                }

                this.service.getChannelManager().appendMessageByPartner(channelCode,
                        accountId, partnerId, sender.getExternalId(), message);
            }
        }
        else if (null != group) {
            // 获取当前已存储的消息
            List<Message> messageList = this.service.getChannelManager().getMessagesByGroup(channelCode,
                    accountId, group.getName(), 0, 20);
            // 匹配新消息
            List<Message> newMessageList = matchNewMessages(messageList, event.getMessages());

            for (Message message : newMessageList) {
                this.service.getChannelManager().appendMessageByGroup(channelCode,
                        accountId, group.getName(), message.getSender().getName(), message);
            }
        }

        return true;
    }

    /**
     * 更新账号数据。
     *
     * @param accountEvent
     */
    public void updateAccount(AccountEvent accountEvent) {
        String channelCode = this.service.getChannelManager().getChannelCodeWithAccountId(accountEvent.getAccount().getExternalId());
        if (null == channelCode) {
            return;
        }

        this.service.getChannelManager().updateAccount(channelCode, accountEvent.getAccount(), accountEvent.getProduct());
    }

    /**
     * 获取账号数据。
     *
     * @param channelCode
     * @return
     */
    public AccountEvent getAccount(ChannelCode channelCode) {
        String accountId = this.service.getChannelManager().getAccountId(channelCode.code);
        if (null == accountId) {
            return null;
        }

        Contact account = this.service.getChannelManager().queryAccount(accountId, channelCode.product);
        return new AccountEvent(account);
    }

    /**
     * 更新通讯录数据。
     *
     * @param contactDataEvent
     */
    public void updateContactBook(ContactDataEvent contactDataEvent) {
        this.service.getChannelManager().updateContactBook(contactDataEvent.getAccount().getExternalId(),
                contactDataEvent.getProduct(), contactDataEvent.getContact(), false);
    }

    /**
     * 获取指定的通讯录。
     *
     * @param signal
     * @return
     */
    public ContactZoneEvent getContactBook(ChannelCode channelCode, GetContactZoneSignal signal) {
        String accountId = this.service.getChannelManager().getAccountId(channelCode.code);
        if (null == accountId) {
            return null;
        }

        if (signal.getParticipantType() == ContactZoneParticipantType.Contact) {
            ContactZone contactZone = new ContactZone("contacts");
            contactZone.displayName = "通讯录";

            // 总数
            int totalSize = this.service.getChannelManager().countContactBook(accountId, channelCode.product);
            if (totalSize > 0) {
                List<Contact> list = this.service.getChannelManager().queryContactBook(accountId,
                        channelCode.product, signal.getBeginIndex(), signal.getEndIndex());
                for (Contact contact : list) {
                    ContactZoneParticipant participant = new ContactZoneParticipant(contact);
                    contactZone.addParticipant(participant);
                }
            }

            return new ContactZoneEvent(contactZone, signal.getBeginIndex(), signal.getEndIndex(), totalSize);
        }
        else {
            return null;
        }
    }

    /**
     * 获取指定通道的最近会话列表。
     *
     * @param channelCode
     * @param signal
     * @return
     */
    public ConversationsEvent getRecentConversations(ChannelCode channelCode, GetConversationsSignal signal) {
        // 获取账号 ID
        String accountId = this.service.getChannelManager().getAccountId(channelCode.code);

        List<Conversation> conversations = this.service.getChannelManager().queryRecentConversations(channelCode,
                accountId, signal.getNumConversations(), signal.getNumRecentMessages());

        for (Conversation conversation : conversations) {
            // 按照时间正序插入消息的统一格式
            List<Message> messages = new ArrayList<>(conversation.getRecentMessages());
            conversation.getRecentMessages().clear();

            for (int i = messages.size() - 1; i >= 0; --i) {
                Message message = messages.get(i);
                if (conversation.getType() == ConversationType.Contact) {
                    // 将 Message 的负载还原，然后转为统一的 Message 格式
                    conversation.addRecentMessage(DataHelper.convertMessage((Contact) conversation.getPivotalEntity(),
                            PlainMessage.create(message)));
                }
                else if (conversation.getType() == ConversationType.Group) {
                    // 将 Message 的负载还原，然后转为统一的 Message 格式
                    conversation.addRecentMessage(DataHelper.convertMessage((Group) conversation.getPivotalEntity(),
                            PlainMessage.create(message)));
                }
            }
        }

        ConversationsEvent event = new ConversationsEvent(conversations);
        return event;
    }

    /**
     * 获取指定通道的消息列表。
     *
     * @param channelCode
     * @param signal
     * @return
     */
    public MessagesEvent getMessages(ChannelCode channelCode, GetMessagesSignal signal) {
        // 获取账号 ID
        String accountId = this.service.getChannelManager().getAccountId(channelCode.code);
        // 查询账号
        Contact account = this.service.getChannelManager().queryAccount(accountId, channelCode.product);

        int beginIndex = signal.getBeginIndex();
        int endIndex = signal.getEndIndex();

        if (null != signal.getGroupName()) {
            // 原始消息列表
            List<Message> rawList = this.service.getChannelManager().getMessagesByGroup(channelCode.code,
                    accountId, signal.getGroupName(), beginIndex, (endIndex - beginIndex + 1));
            if (rawList.isEmpty()) {
                // 没有数据
                return null;
            }

            Group group = new Group();
            group.setName(signal.getGroupName());

            List<Message> messages = new ArrayList<>(rawList.size());
            for (Message message : rawList) {
                // 将 Message 的负载还原，然后转为统一的 Message 格式
                messages.add(DataHelper.convertMessage(group, PlainMessage.create(message)));
            }

            MessagesEvent event = new MessagesEvent(group, beginIndex, endIndex, messages);
            return event;
        }
        else if (null != signal.getPartnerId()) {
            // 原始消息列表
            List<Message> rawList = this.service.getChannelManager().getMessagesByPartner(channelCode.code,
                    accountId, signal.getPartnerId(), beginIndex, (endIndex - beginIndex + 1));
            if (rawList.isEmpty()) {
                // 没有数据
                return null;
            }

            // 查找账号
            Contact partner = this.service.getChannelManager().queryPartnerFromBook(
                    account, signal.getPartnerId(), channelCode.product);

            List<Message> messages = new ArrayList<>(rawList.size());
            for (Message message : rawList) {
                // 将 Message 的负载还原，然后转为统一的 Message 格式
                messages.add(DataHelper.convertMessage(partner, PlainMessage.create(message)));
            }

            MessagesEvent event = new MessagesEvent(partner, beginIndex, endIndex, messages);
            return event;
        }

        return null;
    }

    /**
     * 获取指定群组数据。
     *
     * @param channelCode
     * @param signal
     * @return
     */
    public GroupDataEvent getGroupData(ChannelCode channelCode, GetGroupSignal signal) {
        // 获取账号 ID
        String accountId = this.service.getChannelManager().getAccountId(channelCode.code);
        if (null == accountId) {
            return null;
        }

        // 查询账号
        Contact account = this.service.getChannelManager().queryAccount(accountId, channelCode.product);
        if (null == account) {
            return null;
        }

        Group group = this.service.getChannelManager().queryGroup(account, signal.getGroupName(),
                channelCode.product);
        if (null == group) {
            return null;
        }

        return new GroupDataEvent(account, group);
    }

    /**
     * 标记客户端发送了新消息。
     *
     * @param event
     */
    public void markMessageSent(SendMessageEvent event) {
        Contact account = event.getAccount();
        String accountId = account.getExternalId();

        // 获取当前所属的通道码
        String channelCode = this.service.getChannelManager().getChannelCodeWithAccountId(accountId);
        if (null == channelCode) {
            Logger.w(this.getClass(), "#markMessageSent - Can NOT find channel code with Account ID: " + accountId);
            return;
        }

        Contact partner = event.getPartner();
        Group group = event.getGroup();

        PlainMessage plainMessage = event.getPlainMessage();

        if (null != partner) {
            // 创建 Message
            Message message = new Message(plainMessage.getId(), account, partner,
                    plainMessage.getTimestamp(), plainMessage.toJSON());

            this.service.getChannelManager().appendMessageByPartner(channelCode,
                    accountId, partner.getExternalId(), accountId, message);
        }
        else if (null != group) {
            // 创建 Message
            Message message = new Message(plainMessage.getId(), account, group,
                    plainMessage.getTimestamp(), plainMessage.toJSON());

            this.service.getChannelManager().appendMessageByGroup(channelCode,
                    accountId, group.getName(), account.getName(), message);
        }
    }

    /**
     * 发送指定信令到通道。
     *
     * @param channelCode
     * @param signal
     * @return
     */
    public Event transportSignal(ChannelCode channelCode, Signal signal) {
        // 获取账号 ID
        String accountId = this.service.getChannelManager().getAccountId(channelCode.code);
        if (null == accountId) {
            Logger.w(this.getClass(), "#transportSignal - Not find account id in channel " + channelCode.code);
            return null;
        }

        // 查询账号
        Contact account = this.service.getChannelManager().queryAccount(accountId, channelCode.product);
        if (null == account) {
            Logger.w(this.getClass(), "#transportSignal - Not find account in channel " + channelCode.code);
            return null;
        }

        // 获取听风者 ID
        Long pretenderId = this.service.getChannelManager().getPretenderId(channelCode.code);
        if (null == pretenderId) {
            Logger.w(this.getClass(), "#transportSignal - Not find pretender in channel " + channelCode.code);
            return null;
        }

        if (signal instanceof SendMessageSignal) {
            // 设置账号数据
            ((SendMessageSignal) signal).setAccount(account);
        }
        else if (signal instanceof AddFriendSignal) {
            // 设置账号数据
            ((AddFriendSignal) signal).setAccount(account);
        }

        if (this.service.getSignalController().transmit(pretenderId, signal)) {
            return new AckEvent(channelCode, signal.getName());
        }
        else {
            Logger.e(this.getClass(), "#transportSignal - send data to \"" + channelCode.code + "\" failed");
            return null;
        }
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
