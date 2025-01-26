/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.core.talk.TalkContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIGC 服务单元。
 */
public class AIGCUnit extends Entity {

    private String key;

    private Contact contact;

    private AICapability capability;

    private TalkContext context;

    private AtomicInteger runningCounter;

    private ConcurrentLinkedQueue<Failure> failures;

    public AIGCUnit(Contact contact, AICapability capability, TalkContext context) {
        super(contact.id, contact.domain.getName());
        this.key = AIGCUnit.makeQueryKey(contact, capability);
        this.contact = contact;
        this.capability = capability;
        this.context = context;
        this.runningCounter = new AtomicInteger(0);
        this.failures = new ConcurrentLinkedQueue<>();
    }

    public AIGCUnit(JSONObject json) {
        super(json);
        this.capability = new AICapability(json.getJSONObject("capability"));
        this.runningCounter = new AtomicInteger(json.getInt("runningCounter"));
        this.failures = new ConcurrentLinkedQueue<>();
        if (json.has("failures")) {
            JSONArray array = json.getJSONArray("failures");
            for (int i = 0; i < array.length(); ++i) {
                Failure failure = new Failure(array.getJSONObject(i));
                this.failures.add(failure);
            }
        }
    }

    public String getQueryKey() {
        return this.key;
    }

    public Contact getContact() {
        return this.contact;
    }

    public AICapability getCapability() {
        return this.capability;
    }

    public TalkContext getContext() {
        return this.context;
    }

    public void setTalkContext(TalkContext context) {
        this.context = context;
    }

    public synchronized void setRunning(boolean value) {
        if (value) {
            this.runningCounter.incrementAndGet();
        }
        else {
            this.runningCounter.decrementAndGet();
        }
    }

    public boolean isRunning() {
        return this.runningCounter.get() != 0;
    }

    public void markFailure(int code, long timestamp, long contactId) {
        this.failures.add(new Failure(code, timestamp, contactId));
    }

    public int numFailure() {
        return this.failures.size();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("capability", this.capability.toJSON());
        json.put("running", this.runningCounter.get() > 0);
        json.put("runningCounter", this.runningCounter.get());

        JSONArray array = new JSONArray();
        for (Failure failure : this.failures) {
            array.put(failure.toJSON());
        }
        json.put("failures", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public static String makeQueryKey(Contact contact, AICapability capability) {
        StringBuilder buf = new StringBuilder(contact.getId().toString());
        buf.append("_");
        buf.append(contact.domain.getName());
        buf.append("_");
        buf.append(capability.getName());
        buf.append("_");
        buf.append(capability.getPrimarySubtask());
        buf.append("_");
        buf.append(capability.getDescription());
        return buf.toString();
    }

    public class Failure {

        public final int code;

        public final long timestamp;

        public long contactId;

        public Failure(int code, long timestamp, long contactId) {
            this.code = code;
            this.timestamp = timestamp;
            this.contactId = contactId;
        }

        public Failure(JSONObject json) {
            this.code = json.getInt("code");
            this.timestamp = json.getLong("timestamp");
            this.contactId = json.getLong("contactId");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("code", this.code);
            json.put("timestamp", this.timestamp);
            json.put("contactId", this.contactId);
            return json;
        }
    }
}
