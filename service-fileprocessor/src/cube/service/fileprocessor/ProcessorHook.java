/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileProcessorAction;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.FileLabel;
import cube.common.state.FileProcessorStateCode;
import cube.file.OperationWorkflow;
import cube.plugin.Hook;
import cube.plugin.HookResult;
import cube.plugin.PluginContext;
import cube.service.Director;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 处理器钩子。
 */
public class ProcessorHook extends Hook {

    /**
     * 工作流已开始执行。
     */
    public final static String WorkflowStarted = "WorkflowStarted";

    /**
     * 工作流已停止执行。
     */
    public final static String WorkflowStopped = "WorkflowStopped";

    /**
     * 操作工作已开始。
     */
    public final static String WorkBegun = "WorkBegun";

    /**
     * 操作工作已结束。
     */
    public final static String WorkEnded = "WorkEnded";

    private FileProcessorServiceCellet cellet;

    /**
     * 构造函数。
     *
     * @param key
     */
    public ProcessorHook(String key, FileProcessorServiceCellet cellet) {
        super(key);
        this.cellet = cellet;
    }

    @Override
    public HookResult apply(PluginContext context) {
        HookResult result = super.apply(context);

        WorkflowPluginContext ctx = (WorkflowPluginContext) context;

        OperationWorkflow workflow = ctx.getWorkflow();
        Long contactId = workflow.getContactId();
        if (null == contactId || workflow.getClientId() > 0) {
            // 没有指定联系人
            return result;
        }

        Contact contact = ContactManager.getInstance().getOnlineContact(workflow.getDomain(), workflow.getContactId());
        if (null == contact) {
            // 没有找到对应的联系人
            return result;
        }

        if (ProcessorHook.WorkflowStopped.equals(this.getKey())) {
            // 将结果文件写入存储服务
            FileLabel fileLabel = this.cellet.getService().writeFileToStorageService(contactId, contact.getDomain().getName(),
                    ctx.getResultFile());
            if (null != fileLabel) {
                workflow.setResultFileLabel(fileLabel);
            }
            else {
                Logger.e(this.getClass(), "Write file to file storage failed: " + contactId);
            }
        }

        for (Device device : contact.getDeviceList()) {
            TalkContext talkContext = device.getTalkContext();
            if (null == talkContext) {
                continue;
            }

            WorkflowOperatingEvent event = new WorkflowOperatingEvent(this.getKey(), workflow);
            event.setWork(ctx.getWork());

            JSONObject payload = new JSONObject();
            payload.put("code", FileProcessorStateCode.Ok.code);
            payload.put("data", event.toJSON());

            Packet packet = new Packet(FileProcessorAction.WorkflowOperating.name, payload);
            ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                    contactId.longValue(), contact.getDomain().getName());
            this.cellet.speak(talkContext, dialect);
        }

        return result;
    }
}
