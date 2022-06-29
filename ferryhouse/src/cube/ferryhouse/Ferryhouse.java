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

package cube.ferryhouse;

import cell.api.Nucleus;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.common.entity.Message;
import cube.ferry.BoxReport;
import cube.ferry.DomainMember;
import cube.ferry.FerryAction;
import cube.ferry.FerryPort;
import cube.ferryhouse.tool.LicenceTool;
import cube.storage.MySQLStorage;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ferry 客户端。
 */
public class Ferryhouse implements TalkListener {

    public final static String FERRY = "Ferry";

    public final static String NAME = "Ferryhouse";

    private final static Ferryhouse instance = new Ferryhouse();

    private Nucleus nucleus;

    private String address;
    private int port;

    private JSONObject licence;
    private String domain;

    private FerryStorage ferryStorage;

    private AtomicBoolean checkedIn;

    /**
     * 用于在设备接入网络后需要发送的数据。
     */
    private LinkedList<ActionDialect> preparedQueue;

    private FileManager fileManager;

    private AtomicBoolean ready;

    private Ferryhouse() {
        this.checkedIn = new AtomicBoolean(false);
        this.preparedQueue = new LinkedList<>();
        this.ready = new AtomicBoolean(false);
    }

    public static Ferryhouse getInstance() {
        return Ferryhouse.instance;
    }

    public void config(Nucleus nucleus) {
        this.nucleus = nucleus;

        Properties properties = this.loadConfig();
        if (null == properties) {
            Logger.e(this.getClass(), "#config - Can NOT find config file");
            return;
        }

        this.address = properties.getProperty("ferry.address");
        this.port = Integer.parseInt(properties.getProperty("ferry.port", "7900").trim());

        // 数据库
        JSONObject mysqlConfig = new JSONObject();
        mysqlConfig.put(MySQLStorage.CONFIG_HOST, properties.getProperty("mysql.host"));
        mysqlConfig.put(MySQLStorage.CONFIG_PORT, Integer.parseInt(properties.getProperty("mysql.port", "3306")));
        mysqlConfig.put(MySQLStorage.CONFIG_SCHEMA, properties.getProperty("mysql.schema"));
        mysqlConfig.put(MySQLStorage.CONFIG_USER, properties.getProperty("mysql.user"));
        mysqlConfig.put(MySQLStorage.CONFIG_PASSWORD, properties.getProperty("mysql.password"));

        // 读取许可证
        try {
            this.licence = LicenceTool.extractData(new File("config/licence"), "shixincube.com");
            if (null == this.licence) {
                Logger.e(this.getClass(), "#config - Licence file error");
                System.exit(0);
                return;
            }

            this.domain = this.licence.getString("domain");

            Logger.i(this.getClass(), "Domain: " + this.domain);
        } catch (IOException e) {
            Logger.e(this.getClass(), "#config - Can NOT find licence file");
            System.exit(0);
            return;
        }

        // 连接 Boat
        this.nucleus.getTalkService().addListener(this);
        this.nucleus.getTalkService().call(this.address, this.port);

        ArrayList<String> domainList = new ArrayList<>();
        domainList.add(this.domain);

        this.ferryStorage = new FerryStorage(this.domain, mysqlConfig);

        // 启动各个 Ferry
        (new Thread() {
            @Override
            public void run() {
                ferryStorage.open();
                ferryStorage.execSelfChecking(domainList);

                // 更新许可证数据
                ferryStorage.writeLicence(licence);

                fileManager = new FileManager(domain, ferryStorage);

                // 从数据库加载偏好设置
                Preferences preferences = loadPreferences();
                refreshWithPreferences(preferences);

                // 就绪
                ready.set(true);
            }
        }).start();
    }

    public FileManager getFileManager() {
        return this.fileManager;
    }

    /**
     * 生成报告。
     *
     * @return
     */
    public BoxReport generateReport() {
        BoxReport boxReport = new BoxReport(this.domain);
        boxReport.setDataSpaceSize(this.ferryStorage.queryAllTablesSize());

        this.fileManager.calcUsage(boxReport);

        // 消息总数
        boxReport.setTotalMessages(this.ferryStorage.countMessages());

        return boxReport;
    }

    public void quit() {
        ActionDialect dialect = new ActionDialect(FerryAction.CheckOut.name);
        dialect.addParam("domain", this.domain);

        this.nucleus.getTalkService().speak(FERRY, dialect);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.nucleus.getTalkService().hangup(this.address, this.port, true);

        this.ferryStorage.close();
    }

    private Properties loadConfig() {
        File file = new File("config/ferryhouse_dev.properties");
        if (!file.exists()) {
            file = new File("config/ferryhouse.properties");
        }

        try {
            return ConfigUtils.readProperties(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void writeLicence(JSONObject json) {
        File outputFile = new File("config/licence");
        try {
            LicenceTool.writeFile(json, "shixincube.com", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.ferryStorage.writeLicence(json);
        this.licence = json;
    }

    private Preferences loadPreferences() {
        Preferences preferences = new Preferences();

        // 是否从云端同步数据到本地
        String value = this.ferryStorage.readProperty(Preferences.ITEM_SYNCH_DATA);
        if (null != value) {
            preferences.synchronizeData = value.equalsIgnoreCase("true") || value.equals("1");
        }
        else {
            this.ferryStorage.writeProperty(Preferences.ITEM_SYNCH_DATA,
                    preferences.synchronizeData ? "true" : "false");
        }

        // 重启后是否清空数据
        value = this.ferryStorage.readProperty(Preferences.ITEM_CLEANUP_WHEN_REBOOT);
        if (null != value) {
            preferences.cleanupWhenReboot = value.equalsIgnoreCase("true") || value.equals("1");
        }
        else {
            this.ferryStorage.writeProperty(Preferences.ITEM_CLEANUP_WHEN_REBOOT,
                    preferences.cleanupWhenReboot ? "true" : "false");
        }

        // 最大可用存储空间
        value = this.ferryStorage.readProperty(Preferences.ITEM_MAX_STORAGE_SPACE_SIZE);
        if (null != value) {
            long size = Long.parseLong(value);
            if (size > 0) {
                preferences.maxStorageSpaceSize = size;
            }
        }
        else {
            this.ferryStorage.writeProperty(Preferences.ITEM_MAX_STORAGE_SPACE_SIZE,
                    Long.toString(preferences.maxStorageSpaceSize));
        }

        return preferences;
    }

    private void refreshWithPreferences(Preferences preferences) {
        if (preferences.synchronizeData) {
            // 发送 Synchronize
            ActionDialect actionDialect = new ActionDialect(FerryAction.Synchronize.name);
            actionDialect.addParam("domain", this.domain);
            if (this.checkedIn.get()) {
                this.nucleus.getTalkService().speak(FERRY, actionDialect);
            }
            else {
                synchronized (this.preparedQueue) {
                    this.preparedQueue.offer(actionDialect);
                }
            }
        }

        if (preferences.cleanupWhenReboot) {
            // 清空所有数据
            long now = System.currentTimeMillis();
            // 从数据库删除消息
            this.ferryStorage.deleteAllMessages(now);
            // 从数据库删除文件记录
            this.ferryStorage.deleteAllFileLabels(now);

            // 删除文件
            this.fileManager.cleanup();

            // 发送 Tenet
            ActionDialect actionDialect = new ActionDialect(FerryAction.Tenet.name);
            actionDialect.addParam("port", FerryPort.Cleanup);
            actionDialect.addParam("domain", this.domain);
            actionDialect.addParam("timestamp", now);
            if (this.checkedIn.get()) {
                this.nucleus.getTalkService().speak(FERRY, actionDialect);
            }
            else {
                synchronized (this.preparedQueue) {
                    this.preparedQueue.offer(actionDialect);
                }
            }
        }

        // 设置最大存储空间
        this.fileManager.setMaxSpaceSize(preferences.maxStorageSpaceSize);
    }

    private void processFerry(ActionDialect actionDialect) {
        String port = actionDialect.getParamAsString("port");
        if (FerryPort.WriteMessage.equals(port)) {
            Message message = new Message(actionDialect.getParamAsJson("message"));
            this.ferryStorage.writeMessage(message);
        }
        else if (FerryPort.UpdateMessage.equals(port)) {
            Message message = new Message(actionDialect.getParamAsJson("message"));
            this.ferryStorage.updateMessageState(message);
        }
        else if (FerryPort.DeleteMessage.equals(port)) {
            Message message = new Message(actionDialect.getParamAsJson("message"));
            this.ferryStorage.deleteMessage(message);
        }
        else if (FerryPort.BurnMessage.equals(port)) {
            Message message = new Message(actionDialect.getParamAsJson("message"));
            this.ferryStorage.updateMessagePayload(message);
        }
        else if (FerryPort.SaveFile.equals(port)) {
            FileLabel fileLabel = new FileLabel(actionDialect.getParamAsJson("fileLabel"));
            this.fileManager.saveFileLabel(fileLabel);
        }
        else if (FerryPort.TransferIntoMember.equals(port)) {
            this.transferIntoMember(actionDialect);
        }
        else if (FerryPort.TransferOutMember.equals(port)) {
            this.transferOutMember(actionDialect);
        }
        else if (FerryPort.ResetLicence.equals(port)) {
            Logger.i(this.getClass(), "Reset licence - " + this.domain);
            JSONObject licence = actionDialect.getParamAsJson("licence");
            this.writeLicence(licence);
        }
    }

    private void transferIntoMember(ActionDialect actionDialect) {
        DomainMember member = new DomainMember(actionDialect.getParamAsJson("member"));
        this.ferryStorage.writeDomainMember(member);

        Contact contact = new Contact(actionDialect.getParamAsJson("contact"));
        this.ferryStorage.writeContact(contact);
    }

    private void transferOutMember(ActionDialect actionDialect) {
        DomainMember member = new DomainMember(actionDialect.getParamAsJson("member"));
        // 修改状态
        member.setState(DomainMember.QUIT);
        this.ferryStorage.writeDomainMember(member);
    }

    private void processSynchronize(ActionDialect actionDialect) {
        JSONObject data = actionDialect.getParamAsJson("data");
        JSONArray memberArray = data.getJSONArray("members");
        JSONArray contactArray = data.getJSONArray("contacts");

        for (int i = 0; i < memberArray.length(); ++i) {
            DomainMember member = new DomainMember(memberArray.getJSONObject(i));
            if (!this.ferryStorage.existsDomainMember(member.getContactId())) {
                this.ferryStorage.writeDomainMember(member);
            }
        }

        for (int i = 0; i < contactArray.length(); ++i) {
            Contact contact = new Contact(contactArray.getJSONObject(i));
            if (!this.ferryStorage.existsContact(contact.getId())) {
                this.ferryStorage.writeContact(contact);
            }
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {
        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = actionDialect.getName();

        if (FerryAction.Ferry.name.equals(action)) {
            this.processFerry(actionDialect);
        }
        else if (FerryAction.Ping.name.equals(action)) {
            int sn = actionDialect.getParamAsInt("sn");
            ActionDialect response = new ActionDialect(FerryAction.PingAck.name);
            response.addParam("sn", sn);
            response.addParam("domain", this.domain);
            this.nucleus.getTalkService().speak(FERRY, response);
        }
        else if (FerryAction.Report.name.equals(action)) {
            BoxReport report = this.generateReport();

            int sn = actionDialect.getParamAsInt("sn");
            ActionDialect response = new ActionDialect(FerryAction.ReportAck.name);
            response.addParam("sn", sn);
            response.addParam("domain", this.domain);
            response.addParam("report", (null != report) ? report.toJSON() : new JSONObject());
            this.nucleus.getTalkService().speak(FERRY, response);
        }
        else if (FerryAction.Synchronize.name.equals(action)) {
            this.processSynchronize(actionDialect);
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {
        // 保存文件流
        this.fileManager.saveFileInputStream(primitiveInputStream);
    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onContacted(Speakable speakable) {
        Logger.d(this.getClass(), "#onContacted");

        (new Thread() {
            @Override
            public void run() {
                while (!ready.get()) {
                    // 等待就绪
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // 执行 Chek-In
                ActionDialect dialect = new ActionDialect(FerryAction.CheckIn.name);
                dialect.addParam("domain", domain);
                dialect.addParam("licence", licence);
                speakable.speak(FERRY, dialect);

                checkedIn.set(true);

                synchronized (preparedQueue) {
                    while (!preparedQueue.isEmpty()) {
                        ActionDialect actionDialect = preparedQueue.removeFirst();
                        if (null != actionDialect) {
                            speakable.speak(FERRY, actionDialect);
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void onQuitted(Speakable speakable) {
        Logger.d(this.getClass(), "#onQuitted");

        this.checkedIn.set(false);
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        Logger.d(this.getClass(), "onFailed - " + talkError.getErrorCode());

        this.checkedIn.set(false);
    }
}
