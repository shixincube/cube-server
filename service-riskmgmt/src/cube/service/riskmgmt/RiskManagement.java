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

import cube.common.entity.ChainNode;
import cube.common.entity.Message;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactManagerListener;
import cube.service.messaging.MessagingHook;
import cube.service.messaging.MessagingService;
import cube.service.riskmgmt.plugin.MessagingPostPushPlugin;
import cube.service.riskmgmt.plugin.MessagingPrePushPlugin;
import cube.service.riskmgmt.plugin.ModifyContactNamePlugin;
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
        this.executor = Executors.newFixedThreadPool(4);

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
    public PluginSystem<?> getPluginSystem() {
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

    public void addFileChainNode(Message message) {
        this.executor.execute(() -> {

        });
    }

    private void loadSensitiveWordToMemory(List<String> domainList) {
        for (String domain : domainList) {
            List<SensitiveWord> list = this.mainStorage.readAllSensitiveWords(domain);
            this.sensitiveWordsMap.put(domain, list);
        }
    }

    private void initPlugin() {
        MessagingService messagingService = (MessagingService) this.getKernel().getModule("Messaging");
        PluginSystem<?> pluginSystem = messagingService.getPluginSystem();
        while (null == pluginSystem) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            pluginSystem = messagingService.getPluginSystem();
        }

        pluginSystem.register(MessagingHook.PrePush, new MessagingPrePushPlugin(this));
        pluginSystem.register(MessagingHook.PostPush, new MessagingPostPushPlugin(this));
    }
}
