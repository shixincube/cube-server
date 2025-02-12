/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.data;

import cell.util.Base64;
import cube.common.entity.Contact;
import cube.common.entity.FileAttachment;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.hub.data.wechat.PlainMessage;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * 辅助函数。
 */
public class DataHelper {

    /**
     * 默认的所有人 ID 。
     */
    public final static Long DEFAULT_OWNER_ID = 1124L;

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
        message.setTimestampPrecision(plainMessage.getDatePrecision());

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
        message.setTimestampPrecision(plainMessage.getDatePrecision());

        return message;
    }

    private static JSONObject packMessagePayload(PlainMessage plainMessage) {
        JSONObject payload = null;

        if (plainMessage.isTextType()) {
            payload = new JSONObject();
            payload.put("type", "text");
            // 如果使用 Base64 格式，则标记 base64 为 true
            payload.put("base64", true);
            payload.put("content", Base64.encodeBytes(
                    plainMessage.getText().getBytes(StandardCharsets.UTF_8)));
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
     * 构建联系人实例。
     *
     * @param name
     * @param accountId
     * @return
     */
    public static Contact makeContact(String name, String accountId) {
        Contact contact = new Contact(accountId);
        contact.setName(name);
        return contact;
    }

    /**
     * 过滤联系人头像数据的文件标签中的直接连接地址信息。
     *
     * @param contact
     * @return 返回经过过滤的联系人的 JSON 数据。
     */
    public static JSONObject filterContactAvatarFileLabel(Contact contact) {
        JSONObject ctx = contact.getContext();
        if (null == ctx) {
            return contact.toCompactJSON();
        }

        if (!ctx.has("avatarFileLabel")) {
            return contact.toCompactJSON();
        }

        JSONObject json = ctx.getJSONObject("avatarFileLabel");
        if (!json.has("directURL")) {
            return contact.toCompactJSON();
        }

        JSONObject ctxCopy = new JSONObject(ctx.toMap());
        ctxCopy.getJSONObject("avatarFileLabel").remove("directURL");

        // 使用副本替换
        JSONObject result = contact.toCompactJSON();
        result.remove("context");
        result.put("context", ctxCopy);

        return result;
    }

    /**
     * 过滤联系人头像数据的文件标签中的直接连接地址信息。
     *
     * @param contactJSON
     * @return
     */
    public static JSONObject filterContactAvatarFileLabel(JSONObject contactJSON) {
        if (!contactJSON.has("context")) {
            return contactJSON;
        }

        JSONObject ctx = contactJSON.getJSONObject("context");
        if (!ctx.has("avatarFileLabel")) {
            return contactJSON;
        }

        JSONObject json = ctx.getJSONObject("avatarFileLabel");
        if (!json.has("directURL")) {
            return contactJSON;
        }

        json.remove("directURL");
        return contactJSON;
    }
}
