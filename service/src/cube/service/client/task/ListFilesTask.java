package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.FileLabel;
import cube.common.notice.NoticeData;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

import java.util.List;

/**
 * 批量获取文件。
 */
public class ListFilesTask extends ClientTask {

    public ListFilesTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);

        ActionDialect response = new ActionDialect(ClientAction.ListFiles.name);
        JSONObject notifier = copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        List<FileLabel> fileLabels = module.notify(notification);
        if (null == fileLabels) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(talkContext, response);
            return;
        }

        notification.put("total", fileLabels.size());

        this.cellet.getExecutor().execute(() -> {
            for (FileLabel fileLabel : fileLabels) {
                ActionDialect responseData = new ActionDialect(ClientAction.ListFiles.name);
                responseData.addParam(NoticeData.ASYNC_NOTIFIER, notifier);
                responseData.addParam(NoticeData.DATA, fileLabel.toCompactJSON());
                cellet.speak(talkContext, responseData);
            }
        });

        response.addParam(NoticeData.CODE, FileStorageStateCode.Ok.code);
        response.addParam(NoticeData.DATA, notification);

        this.cellet.speak(this.talkContext, response);
    }
}
