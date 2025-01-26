/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import cell.util.Base64;
import cube.common.entity.Contact;
import cube.common.entity.ConversationType;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 发送消息信令。
 */
public class SendMessageSignal extends Signal {

    public final static String NAME = "SendMessage";

    private Contact account;

    private ConversationType conversationType;

    private String partnerId;

    private String groupName;

    private String text;

    private String fileCode;

    public SendMessageSignal(String channelCode,
                             ConversationType conversationType, String idOrName) {
        super(NAME);
        setCode(channelCode);

        if (conversationType == ConversationType.Contact) {
            this.partnerId = idOrName;
        }
        else if (conversationType == ConversationType.Group) {
            this.groupName = idOrName;
        }
    }

    public SendMessageSignal(JSONObject json) {
        super(json);

        if (json.has("account")) {
            this.account = new Contact(json.getJSONObject("account"));
        }

        if (json.has("partnerId")) {
            this.partnerId = json.getString("partnerId");
            this.conversationType = ConversationType.Contact;
        }

        if (json.has("groupName")) {
            this.groupName = json.getString("groupName");
            this.conversationType = ConversationType.Group;
        }

        if (json.has("text")) {
            try {
                byte[] bytes = Base64.decode(json.getString("text"));
                this.text = new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (json.has("fileCode")) {
            this.fileCode = json.getString("fileCode");
        }
    }

    public Contact getAccount() {
        return this.account;
    }

    public void setAccount(Contact account) {
        this.account = account;
    }

    public ConversationType getConversationType() {
        return this.conversationType;
    }

    public String getPartnerId() {
        return this.partnerId;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void setFileCode(String fileCode) {
        this.fileCode = fileCode;
    }

    public String getFileCode() {
        return this.fileCode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        if (null != this.account) {
            json.put("account", this.account.toCompactJSON());
        }

        if (null != this.partnerId) {
            json.put("partnerId", this.partnerId);
        }

        if (null != this.groupName) {
            json.put("groupName", this.groupName);
        }

        if (null != this.text) {
            String base64 = Base64.encodeBytes(this.text.getBytes(StandardCharsets.UTF_8));
            json.put("text", base64);
        }

        if (null != this.fileCode) {
            json.put("fileCode", this.fileCode);
        }

        return json;
    }
}
