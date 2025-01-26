/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 传输方式。
 */
public class TransmissionMethod extends Entity {

    /**
     * 消息实体。
     */
    private Message message;

    /**
     * 传输目标。
     */
    private AbstractContact target;

    /**
     * 分享标签。
     */
    private SharingTag sharingTag;

    /**
     * 操作时使用的设备。
     */
    private Device device;

    public TransmissionMethod(Message message, AbstractContact target, Device device) {
        super();
        this.message = message;
        this.target = target;
        this.device = device;
    }

    public TransmissionMethod(SharingTag sharingTag) {
        super();
        this.sharingTag = sharingTag;
    }

    public TransmissionMethod(JSONObject json) {
        super();

        if (json.has("message")) {
            this.message = new Message(json.getJSONObject("message"));
        }

        if (json.has("target")) {
            JSONObject data = json.getJSONObject("target");
            this.target = ContactHelper.create(data);
        }

        if (json.has("device")) {
            this.device = new Device(json.getJSONObject("device"));
        }

        if (json.has("sharingTag")) {
            this.sharingTag = new SharingTag(json.getJSONObject("sharingTag"));
        }
    }

    public Message getMessage() {
        return this.message;
    }

    public AbstractContact getTarget() {
        return this.target;
    }

    public Device getDevice() {
        return this.device;
    }

    public SharingTag getSharingTag() {
        return this.sharingTag;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.message) {
            json.put("message", this.message.toJSON());
        }
        if (null != this.target) {
            json.put("target", this.target.toJSON());
        }
        if (null != this.device) {
            json.put("device", this.device.toJSON());
        }
        if (null != this.sharingTag) {
            json.put("sharingTag", this.sharingTag.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
