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

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.FileStorageAction;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.Actions;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 放置文件标签。
 */
public class PutFileTask extends ClientTask {

    public PutFileTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject fileLabel = actionDialect.getParamAsJson("fileLabel");

        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.PutFile.name);
        notification.put("fileLabel", fileLabel);

        ActionDialect response = new ActionDialect(Actions.PutFile.name);
        copyNotifier(response);

        // 获取联系人
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            cellet.speak(talkContext, response);
            return;
        }

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("fileLabel", (JSONObject) result);
        cellet.speak(talkContext, response);
    }
}