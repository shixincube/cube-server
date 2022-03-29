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

package cube.hub.data;

import cube.common.entity.Contact;
import cube.common.entity.FileAttachment;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.hub.data.wechat.PlainMessage;
import org.json.JSONObject;

/**
 * 辅助函数。
 */
public class DataHelper {

    private DataHelper() {
    }

    /**
     * 源消息转统一格式。
     *
     * @param group
     * @param plainMessage
     * @return
     */
    public static Message convertMessage(Group group, PlainMessage plainMessage) {
        JSONObject payload = packMessagePayload(plainMessage);
        FileAttachment fileAttachment = null;

        if (null != plainMessage.getFileLabel()) {
            fileAttachment = new FileAttachment(plainMessage.getFileLabel());
        }

        Message message = new Message(plainMessage.getId(), plainMessage.getSender(),
                group, plainMessage.getDate(), payload);
        message.setAttachment(fileAttachment);

        return message;
    }

    /**
     * 源消息转统一格式。
     *
     * @param partner
     * @param plainMessage
     * @return
     */
    public static Message convertMessage(Contact partner, PlainMessage plainMessage) {
        JSONObject payload = packMessagePayload(plainMessage);
        FileAttachment fileAttachment = null;

        if (null != plainMessage.getFileLabel()) {
            fileAttachment = new FileAttachment(plainMessage.getFileLabel());
        }

        Message message = new Message(plainMessage.getId(), plainMessage.getSender(),
                partner, plainMessage.getDate(), payload);
        message.setAttachment(fileAttachment);

        return message;
    }

    private static JSONObject packMessagePayload(PlainMessage plainMessage) {
        JSONObject payload = null;

        if (plainMessage.isTextType()) {
            payload = new JSONObject();
            payload.put("type", "text");
            payload.put("content", plainMessage.getText());
        }
        else if (plainMessage.isImageType()) {
            payload = new JSONObject();
            payload.put("type", "image");
        }
        else if (plainMessage.isFileType()) {
            payload = new JSONObject();
            payload.put("type", "file");
        }

        return payload;
    }

    /**
     * 提取联系人 ID 。
     *
     * @param contact
     * @return
     */
    public static String extractAccountId(Contact contact) {
        JSONObject ctx = contact.getContext();
        if (null == ctx) {
            return null;
        }

        return ctx.has("id") ? ctx.getString("id") : null;
    }

    /**
     * 构建联系人实例。
     *
     * @param name
     * @param accountId
     * @return
     */
    public static Contact makeContact(String name, String accountId) {
        Contact contact = new Contact();
        contact.setName(name);

        JSONObject ctx = new JSONObject();
        ctx.put("id", accountId);
        contact.setContext(ctx);

        return contact;
    }
}
