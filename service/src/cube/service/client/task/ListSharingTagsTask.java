/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cube.common.action.ClientAction;
import cube.common.entity.SharingTag;
import cube.common.notice.CountSharingTags;
import cube.common.notice.ListSharingTags;
import cube.common.notice.NoticeData;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 批量获取分享标签。
 */
public class ListSharingTagsTask extends ClientTask {

    public ListSharingTagsTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);

        ActionDialect response = new ActionDialect(ClientAction.ListSharingTags.name);
        JSONObject notifier = copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        List<SharingTag> sharingTagList = module.notify(notification);
        if (null == sharingTagList) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(talkContext, response);
            return;
        }

        // 查询的是否是有效期内的
        boolean valid = notification.getBoolean(ListSharingTags.VALID);

        if (notification.has(ListSharingTags.BEGIN) && notification.has(ListSharingTags.END)) {
            // 获取总数量
            int total = 0;
            CountSharingTags countNotice = new CountSharingTags(notification.getString(ListSharingTags.DOMAIN),
                    notification.getLong(ListSharingTags.CONTACT_ID));
            Object numResult = module.notify(countNotice);
            if (null != numResult) {
                JSONObject numJson = (JSONObject) numResult;
                total = valid ? CountSharingTags.parseValidNumber(numJson) : CountSharingTags.parseInvalidNumber(numJson);
            }

            JSONObject data = new JSONObject();
            JSONArray array = new JSONArray();
            for (SharingTag tag : sharingTagList) {
                array.put(tag.toCompactJSON());
            }
            data.put("list", array);
            data.put("total", total);
            data.put("begin", notification.getInt(ListSharingTags.BEGIN));
            data.put("end", notification.getInt(ListSharingTags.END));
            data.put("valid", valid);

            response.addParam("code", FileStorageStateCode.Ok.code);
            response.addParam("data", data);
        }
        else {
            notification.put("total", sharingTagList.size());

            // 按照时间查询的数据进行异步发送
            this.cellet.getExecutor().execute(() -> {
                for (SharingTag tag : sharingTagList) {
                    ActionDialect responseData = new ActionDialect(ClientAction.ListSharingTags.name);
                    responseData.addParam(NoticeData.ASYNC_NOTIFIER, notifier);
                    responseData.addParam("data", tag.toCompactJSON());
                    this.cellet.speak(this.talkContext, responseData);
                }
            });

            response.addParam("code", FileStorageStateCode.Ok.code);
            response.addParam("data", notification);
        }

        this.cellet.speak(this.talkContext, response);
    }
}
