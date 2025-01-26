/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.service.filestorage.hierarchy.FileSearcher;
import cube.service.filestorage.hierarchy.SearchFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 搜索指定条件的文件。
 */
public class SearchFileTask extends ServiceTask {

    public SearchFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
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
                    this.makeResponse(action, packet, FileStorageStateCode.InvalidDomain.code, packet.data));
            markResponseTime();
            return;
        }

        // 域
        String domain = authToken.getDomain();

        // 读取参数
        if (!packet.data.has("root") || !packet.data.has("filter")) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        Long rootId = packet.data.getLong("root");
        JSONObject filter = packet.data.getJSONObject("filter");

        // 获取服务
        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        // 获取指定 ROOT ID 对应的文件层级描述
        FileHierarchy fileHierarchy = service.getFileHierarchy(domain, rootId);
        if (null == fileHierarchy) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 创建搜索过滤器
        SearchFilter searchFilter = new SearchFilter(filter);

        List<FileSearcher.IndexingItem> result = fileHierarchy.getFileSearcher().search(searchFilter);
        if (null == result) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.SearchConditionError.code, packet.data));
            markResponseTime();
            return;
        }

        // 将结果打包
        JSONArray resultArray = new JSONArray();
        for (int i = 0; i < result.size(); ++i) {
            FileSearcher.IndexingItem item = result.get(i);
            resultArray.put(item.toJSON());
        }

        JSONObject response = new JSONObject();
        response.put("filter", filter);
        response.put("result", resultArray);

        // 成功
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, response));
        markResponseTime();
    }
}
