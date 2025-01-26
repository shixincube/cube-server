/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.ContactZone;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 按照指定参数列出联系人分区清单。
 */
public class ListContactZonesTask extends ServiceTask {

    public ListContactZonesTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        long timestamp = System.currentTimeMillis() - (3600L * 1000L);
        int limit = -1;
        boolean compact = false;

        try {
            if (data.has("timestamp")) {
                timestamp = data.getLong("timestamp");
            }
            if (data.has("limit")) {
                limit = data.getInt("limit");
            }
            if (data.has("compact")) {
                compact = data.getBoolean("compact");
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        JSONArray zoneArray = new JSONArray();

        // 获取分区清单
        List<ContactZone> zoneList = ContactManager.getInstance().listContactZones(contact, timestamp, limit);
        for (ContactZone zone : zoneList) {
            zoneArray.put(compact ? zone.toCompactJSON() : zone.toJSON());
        }

        JSONObject resultData = new JSONObject();
        resultData.put("timestamp", timestamp);
        resultData.put("limit", limit);
        resultData.put("compact", compact);
        resultData.put("list", zoneArray);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, resultData));
        markResponseTime();
    }
}
