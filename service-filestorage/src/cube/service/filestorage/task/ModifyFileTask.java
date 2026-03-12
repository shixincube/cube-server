/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.filestorage.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import org.json.JSONObject;

/**
 * 修改文件信息。
 */
public class ModifyFileTask extends ServiceTask {

    public ModifyFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                          Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
        AuthService authService = (AuthService) this.kernel.getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        if (null == authToken) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        // 域
        String domain = authToken.getDomain();
        // 联系人 ID
        long contactId = authToken.getContactId();

        try {
            if (packet.data.has("fileCode") && packet.data.has("context")) {
                String fileCode = packet.data.getString("fileCode");
                JSONObject context = packet.data.getJSONObject("context");
                // 获取服务
                FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

                // 修改上下文
                FileLabel fileLabel = service.modifyFileContext(domain, contactId, fileCode, context);
                if (null == fileLabel) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
                    markResponseTime();
                }
                else {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(action, packet, FileStorageStateCode.Ok.code, fileLabel.toCompactJSON()));
                    markResponseTime();
                }
            }
            else {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.InvalidParameter.code, packet.data));
                markResponseTime();
            }
        } catch (Exception e) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
    }
}
