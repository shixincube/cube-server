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

package cube.service.riskmgmt;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.action.RiskManagementAction;
import cube.common.entity.*;
import cube.common.notice.ListContactBehaviors;
import cube.common.notice.NoticeData;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.auth.AuthServicePluginSystem;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactManagerListener;
import cube.service.contact.ContactPluginSystem;
import cube.service.filestorage.FileStorageHook;
import cube.service.filestorage.FileStoragePluginSystem;
import cube.service.filestorage.FileStorageService;
import cube.service.messaging.MessagingHook;
import cube.service.messaging.MessagingPluginSystem;
import cube.service.messaging.MessagingService;
import cube.service.riskmgmt.plugin.*;
import cube.service.riskmgmt.util.SensitiveWord;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 风险管理。
 */
public class RiskManagement extends AbstractModule implements ContactManagerListener {

    /**
     * 服务单元名。
     */
    public final static String NAME = "RiskMgmt";

    private ExecutorService executor;

    private MainStorage mainStorage;

    private HashMap<String, List<SensitiveWord>> sensitiveWordsMap;

    private ModifyContactNamePlugin modifyContactNamePlugin;

    public RiskManagement() {
        super();
        this.sensitiveWordsMap = new HashMap<>();
    }

    @Override
    public void start() {
        this.executor = Executors.newCachedThreadPool();

        ContactManager.getInstance().addListener(this);

        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(RiskManagement.NAME)) {
            config = config.getJSONObject(RiskManagement.NAME);

            if (config.getString("type").equalsIgnoreCase("MySQL")) {
                this.mainStorage = new MainStorage(this.executor, StorageType.MySQL, config);
            }
            else {
                this.mainStorage = new MainStorage(this.executor, StorageType.SQLite, config);
            }

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    mainStorage.open();

                    AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                    mainStorage.execSelfChecking(authService.getDomainList());

                    loadSensitiveWordToMemory(authService.getDomainList());

                    initPlugin();
                }
            });
        }
    }

    @Override
    public void stop() {
        ContactManager.getInstance().removeListener(this);

        if (null != this.mainStorage) {
            this.mainStorage.close();
        }

        this.executor.shutdownNow();
    }

    @Override
    public PluginSystem getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
    }

    @Override
    public void onStarted(ContactManager manager) {
        this.modifyContactNamePlugin = new ModifyContactNamePlugin(this);
        manager.getPluginSystem().register(ContactHook.ModifyContactName, this.modifyContactNamePlugin);
    }

    @Override
    public void onStopped(ContactManager manager) {
        manager.getPluginSystem().deregister(ContactHook.ModifyContactName, this.modifyContactNamePlugin);
        this.modifyContactNamePlugin = null;
    }

    public void refreshDomain(AuthDomain authDomain) {
        List<String> list = new ArrayList<>();
        list.add(authDomain.domainName);
        this.mainStorage.execSelfChecking(list);
    }

    /**
     * 记录联系人行为。
     *
     * @param behavior
     */
    public void recordContactBehavior(ContactBehavior behavior) {
        this.mainStorage.writeContactBehavior(behavior);
    }

    /**
     * 获取指定联系人在起止时间段内的行为记录列表。
     *
     * @param domain
     * @param contactId
     * @param beginTime
     * @param endTime
     * @param behavior
     * @return
     */
    public List<ContactBehavior> listContactBehaviors(String domain, long contactId, long beginTime, long endTime,
                                                      String behavior) {
        return this.mainStorage.readContactBehaviors(domain, contactId, beginTime, endTime, behavior);
    }

    public boolean hasSensitiveWord(String domain, String text) {
        List<SensitiveWord> list = this.sensitiveWordsMap.get(domain);
        if (null == list) {
            return false;
        }

        String upperCase = text.toUpperCase();
        for (SensitiveWord sensitiveWord : list) {
            if (upperCase.contains(sensitiveWord.word)) {
                return true;
            }
        }
        return false;
    }

    public void addFileChainNode(String event, Message message, Device device) {
        final String domain = message.getDomain().getName();

        this.executor.execute(() -> {
            FileAttachment attachment = message.getAttachment();
            FileLabel fileLabel = attachment.getFileLabel(0);
            // 文件 SHA1 码
            String track1 = fileLabel.getSHA1Code();
            // 文件 MD5 码
            String track2 = fileLabel.getMD5Code();

            Contact contact = ContactManager.getInstance().getContact(domain, message.getFrom());

            AbstractContact target = null;
            if (message.isFromGroup()) {
                target = ContactManager.getInstance().getGroup(message.getSource(), domain);
            }
            else {
                target = ContactManager.getInstance().getContact(domain, message.getTo());
            }

            // 创建节点
            ChainNode node = new ChainNode(Utils.generateSerialNumber(),
                    domain, event, contact, fileLabel, message.getRemoteTimestamp());
            // 设置 Track
            if (null != track1)
                node.addTrack(track1);
            if (null != track2)
                node.addTrack(track2);

            // 设置传输的方法
            TransmissionMethod method = new TransmissionMethod(message, target, device);
            node.setMethod(method);

            // 写入数据库
            this.mainStorage.addTransmissionChainNode(node);
        });
    }

    public void addFileChainNode(SharingTag sharingTag) {
        final String domain = sharingTag.getDomain().getName();
        this.executor.execute(() -> {
            String event = TraceEvent.Share;

            FileLabel fileLabel = sharingTag.getConfig().getFileLabel();
            // 文件 SHA1 码
            String track1 = fileLabel.getSHA1Code();
            // 文件 MD5 码
            String track2 = fileLabel.getMD5Code();

            // 创建节点
            ChainNode node = new ChainNode(Utils.generateSerialNumber(),
                    domain, event, sharingTag.getConfig().getContact(), fileLabel,
                    sharingTag.getTimestamp());
            // 设置 Track
            if (null != track1)
                node.addTrack(track1);
            if (null != track2)
                node.addTrack(track2);

            // 设置传输的方法
            TransmissionMethod method = new TransmissionMethod(sharingTag);
            node.setMethod(method);

            // 写入数据库
            this.mainStorage.addTransmissionChainNode(node);
        });
    }

    @Override
    public Object notify(Object data) {
        if (data instanceof JSONObject) {
            JSONObject jsonData = (JSONObject) data;
            String action = jsonData.getString(NoticeData.ACTION);

            if (RiskManagementAction.ListContactBehaviors.name.equals(action)) {
                String domain = jsonData.getString(ListContactBehaviors.DOMAIN);
                long contactId = jsonData.getLong(ListContactBehaviors.CONTACT_ID);
                long beginTime = jsonData.getLong(ListContactBehaviors.BEGIN_TIME);
                long endTime = jsonData.getLong(ListContactBehaviors.END_TIME);
                String behavior = jsonData.has(ListContactBehaviors.BEHAVIOR) ?
                        jsonData.getString(ListContactBehaviors.BEHAVIOR) : null;
                return this.listContactBehaviors(domain, contactId, beginTime, endTime, behavior);
            }
        }

        return null;
    }

    private void loadSensitiveWordToMemory(List<String> domainList) {
        for (String domain : domainList) {
            List<SensitiveWord> list = this.mainStorage.readAllSensitiveWords(domain);
            this.sensitiveWordsMap.put(domain, list);
        }
    }

    private void initPlugin() {
        // 授权服务
        AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
        AuthServicePluginSystem authPluginSystem = authService.getPluginSystem();
        while (null == authPluginSystem) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            authPluginSystem = authService.getPluginSystem();
        }
        authPluginSystem.register(AuthServiceHook.CreateDomainApp,
                new CreateDomainAppPlugin(this));

        // 联系人服务
        while (!ContactManager.getInstance().isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ContactPluginSystem contactPluginSystem = ContactManager.getInstance().getPluginSystem();
        ContactPlugin contactPlugin = new ContactPlugin(this);
        contactPluginSystem.register(ContactHook.SignIn, contactPlugin);
        contactPluginSystem.register(ContactHook.SignOut, contactPlugin);
        contactPluginSystem.register(ContactHook.Comeback, contactPlugin);
        contactPluginSystem.register(ContactHook.DeviceTimeout, contactPlugin);

        // 文件存储服务
        FileStorageService fileStorage = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        while (!fileStorage.isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        FileStoragePluginSystem filePluginSystem = fileStorage.getPluginSystem();
        FileStoragePlugin fileStoragePlugin = new FileStoragePlugin(this);
        filePluginSystem.register(FileStorageHook.NewFile, fileStoragePlugin);
        filePluginSystem.register(FileStorageHook.DeleteFile, fileStoragePlugin);
        filePluginSystem.register(FileStorageHook.CreateSharingTag, new FileCreateSharingTagPlugin(this));

        // 消息服务
        MessagingService messagingService = (MessagingService) this.getKernel().getModule(MessagingService.NAME);
        while (!messagingService.isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        MessagingPluginSystem messagingPluginSystem = messagingService.getPluginSystem();
        messagingPluginSystem.register(MessagingHook.PrePush, new MessagingPrePushPlugin(this));
        // 消息发送
        messagingPluginSystem.register(MessagingHook.SendMessage, new MessagingSendPlugin(this));
        // 消息转发
        messagingPluginSystem.register(MessagingHook.ForwardMessage, new MessagingForwardPlugin(this));
        // 消息删除
        messagingPluginSystem.register(MessagingHook.DeleteMessage, new MessagingDeletePlugin(this));

        Logger.i(this.getClass(), "#initPlugin - Registers all plugin");
    }
}
