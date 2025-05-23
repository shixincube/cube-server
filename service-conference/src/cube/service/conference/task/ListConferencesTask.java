/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.conference.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Conference;
import cube.common.entity.Contact;
import cube.common.state.ConferenceStateCode;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.conference.ConferenceService;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 查询会议列表任务。
 */
public class ListConferencesTask extends ServiceTask {

    public ListConferencesTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
                    this.makeResponse(action, packet, ConferenceStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ConferenceStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        long ending = System.currentTimeMillis();
        long beginning = ending - (30L * 24L * 60L * 60L * 1000L);

        try {
            beginning = data.getLong("beginning");
            ending = data.getLong("ending");

            if (beginning >= ending) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ConferenceStateCode.SearchConditionError.code, data));
                markResponseTime();
                return;
            }
        } catch (JSONException e) {
            Logger.d(this.getClass(), e.getMessage());
        }

        ConferenceService service = (ConferenceService) this.kernel.getModule(ConferenceService.NAME);
        List<Conference> conferences = service.listConferences(contact, beginning, ending);

        JSONObject result = new JSONObject();
        result.put("beginning", beginning);
        result.put("ending", ending);

        JSONArray list = new JSONArray();
        for (Conference conference : conferences) {
            list.put(conference.toCompactJSON());
        }
        result.put("list", list);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ConferenceStateCode.Ok.code, result));
        markResponseTime();
    }
}
