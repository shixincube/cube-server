/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.hub.data.wechat.PlainMessage;
import org.json.JSONObject;

/**
 * 发送消息事件。
 */
public class SendMessageEvent extends WeChatEvent {

    public final static String NAME = "SendMessage";

    private Contact partner;

    private Group group;

    private PlainMessage plainMessage;

    public SendMessageEvent(Contact account, Contact partner, PlainMessage plainMessage) {
        super(NAME, account);
        this.partner = partner;
        this.plainMessage = plainMessage;
    }

    public SendMessageEvent(Contact account, Group group, PlainMessage plainMessage) {
        super(NAME, account);
        this.group = group;
        this.plainMessage = plainMessage;
    }

    public SendMessageEvent(JSONObject json) {
        super(json);

        if (json.has("partner")) {
            this.partner = new Contact(json.getJSONObject("partner"));
        }

        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }

        if (json.has("plainMessage")) {
            this.plainMessage = new PlainMessage(json.getJSONObject("plainMessage"));
        }
    }

    public Contact getPartner() {
        return this.partner;
    }

    public Group getGroup() {
        return this.group;
    }

    public PlainMessage getPlainMessage() {
        return this.plainMessage;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return make(json);
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        return make(json);
    }

    private JSONObject make(JSONObject json) {
        if (null != this.partner) {
            json.put("partner", this.partner.toCompactJSON());
        }

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.plainMessage) {
            json.put("plainMessage", this.plainMessage.toJSON());
        }

        return json;
    }
}
