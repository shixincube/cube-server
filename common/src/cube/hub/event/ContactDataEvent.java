/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 联系人数据。
 */
public class ContactDataEvent extends WeChatEvent {

    public final static String NAME = "ContactData";

    private Contact contact;

    public ContactDataEvent(Contact account, Contact contact) {
        super(NAME, account);
        this.contact = contact;
    }

    public ContactDataEvent(JSONObject json) {
        super(json);
        this.contact = new Contact(json.getJSONObject("contact"));
    }

    public Contact getContact() {
        return this.contact;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contact", this.contact.toJSON());
        return json;
    }
}
