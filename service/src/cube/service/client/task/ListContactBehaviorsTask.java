/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.ContactBehavior;
import cube.common.notice.NoticeData;
import cube.common.state.RiskMgmtStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 批量获取联系人行为。
 */
public class ListContactBehaviorsTask extends ClientTask {

    public ListContactBehaviorsTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.ListContactBehaviors.name);
        copyNotifier(response);

        AbstractModule module = this.getRiskMgmtService();
        if (null == module) {
            response.addParam(NoticeData.CODE, RiskMgmtStateCode.Failure.code);
            this.cellet.speak(this.talkContext, response);
            return;
        }

        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);
        List<ContactBehavior> result = module.notify(notification);
        if (null == result) {
            response.addParam(NoticeData.CODE, RiskMgmtStateCode.InvalidParameter.code);
            this.cellet.speak(this.talkContext, response);
            return;
        }

        response.addParam(NoticeData.CODE, RiskMgmtStateCode.Ok.code);

        JSONArray array = new JSONArray();
        for (ContactBehavior behavior : result) {
            array.put(behavior.toCompactJSON());
        }
        notification.put("list", array);
        response.addParam(NoticeData.DATA, notification);

        this.cellet.speak(this.talkContext, response);
    }
}
