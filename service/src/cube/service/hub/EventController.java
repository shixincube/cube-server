/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.hub;

import cell.util.log.Logger;
import cube.common.action.FileStorageAction;
import cube.common.entity.ClientDescription;
import cube.common.entity.Contact;
import cube.core.AbstractModule;
import cube.hub.Product;
import cube.hub.event.*;
import cube.util.TimeUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件控制器。
 */
public class EventController {

    private HubService service;

    /**
     * 文件保存路径。
     */
    private Path screenshotFilePath;

    /**
     * 每个客户端保存的最大截图数量。
     */
    private int maxScreenshotEachClient = 10;

    public EventController(HubService service, Path filePath) {
        this.service = service;

        this.screenshotFilePath = Paths.get(filePath.toString(),
                "screenshots");
        if (!Files.exists(this.screenshotFilePath)) {
            try {
                Files.createDirectories(this.screenshotFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void receive(Event event) {
        if (event instanceof ScreenshotEvent) {
            this.process((ScreenshotEvent) event);
            return;
        }

        if (event.getProduct() == Product.WeChat) {
            // WeChat 事件
            ClientDescription clientDescription = event.getDescription();

            if (SubmitMessagesEvent.NAME.equals(event.getName())) {
                SubmitMessagesEvent submitMessagesEvent = (SubmitMessagesEvent) event;
                // 记录消息
                WeChatHub.getInstance().submitMessages(submitMessagesEvent);
            }
            else if (ContactDataEvent.NAME.equals(event.getName())) {
                WeChatHub.getInstance().updateContactBook((ContactDataEvent) event);
            }
            else if (AccountEvent.NAME.equals(event.getName())) {
                WeChatHub.getInstance().updateAccount((AccountEvent) event);
            }
            else if (ReportEvent.NAME.equals(event.getName())) {
                WeChatHub.getInstance().updateReport((ReportEvent) event);
            }
            else if (GroupDataEvent.NAME.equals(event.getName())) {
                WeChatHub.getInstance().updateGroup((GroupDataEvent) event);
            }
            else if (SendMessageEvent.NAME.equals(event.getName())) {
                WeChatHub.getInstance().markMessageSent((SendMessageEvent) event);
            }
            else if (AllocatedEvent.NAME.equals(event.getName())) {
                AllocatedEvent allocatedEvent = (AllocatedEvent) event;
                // 已分配账号
                WeChatHub.getInstance().reportAlloc(allocatedEvent.getPretenderId(),
                        allocatedEvent.getCode(), allocatedEvent.getAccount());
            }
            else {
                Logger.d(this.getClass(), "#receive - Ignored : " + event.getName());
            }
        }
    }

    private void process(ScreenshotEvent screenshotEvent) {
        File path = new File(this.screenshotFilePath.toFile(),
                screenshotEvent.getDescription().getPretender().getId().toString());
        if (!path.exists()) {
            path.mkdirs();
        }
        else {
            File[] files = path.listFiles();
            if (null != files && files.length > this.maxScreenshotEachClient) {
                File oldFile = null;
                long time = System.currentTimeMillis();
                for (File file : files) {
                    if (file.lastModified() < time) {
                        time = file.lastModified();
                        oldFile = file;
                    }
                }

                if (null != oldFile) {
                    oldFile.delete();
                }
            }
        }

        String filename = screenshotEvent.getProduct().name + "_" +
                TimeUtils.formatDateForPathSymbol(System.currentTimeMillis()) + "." +
                        screenshotEvent.getFileLabel().getFileType().getPreferredExtension();

        AbstractModule module = this.service.getKernel().getModule("FileStorage");
        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.LoadFile.name);
        notification.put("domain", screenshotEvent.getDescription().getPretender().getDomain().getName());
        notification.put("fileCode", screenshotEvent.getFileLabel().getFileCode());
        String fullPath = (String) module.notify(notification);
        if (null == fullPath) {
            return;
        }

        try {
            Files.copy(Paths.get(fullPath), Paths.get(path.getAbsolutePath(), filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}