/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.common.entity;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.Packet;
import cube.common.UniqueKey;

/**
 * 消息实体。
 */
public class Message extends Entity implements Comparable<Message> {

    /**
     * 消息 ID 。
     */
    private Long id;

    /**
     * 消息负载 JSON 数据。
     */
    private JSONObject payload;

    /**
     * 消息来源的 ID 。
     */
    private Long from = 0L;

    /**
     * 消息投送目标的 ID 。
     */
    private Long to = 0L;

    /**
     * 消息转副本之后的源 ID 。
     */
    private Long source = 0L;

    /**
     * 消息生成时的源时间戳。
     */
    private long localTimestamp;

    /**
     * 消息到达接入层时的时间戳。
     */
    private long remoteTimestamp;

    /**
     * 消息的来源设备。
     */
    private Device sourceDevice;

    /**
     * 消息状态。
     */
    private MessageState state = MessageState.Unknown;

    /**
     * 消息的唯一键。
     */
    private String uniqueKey;

    /**
     * 构造函数。
     *
     * @param packet
     */
    public Message(Packet packet) throws JSONException {
        super();

        this.domain = new Domain(packet.data.getString("domain"));
        this.id = packet.data.getLong("id");
        this.from = packet.data.getLong("from");
        this.to = packet.data.getLong("to");
        this.source = packet.data.getLong("source");
        this.localTimestamp = packet.data.getLong("lts");
        this.remoteTimestamp = packet.data.getLong("rts");
        this.payload = packet.data.getJSONObject("payload");

        if (packet.data.has("device")) {
            this.sourceDevice = new Device(packet.data.getJSONObject("device"));
        }

        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
    }

    /**
     * 构造函数。
     *
     * @param json 由 JSON 描述的消息。
     */
    public Message(JSONObject json) {
        super();

        try {
            this.domain = new Domain(json.getString("domain"));
            this.id = json.getLong("id");
            this.from = json.getLong("from");
            this.to = json.getLong("to");
            this.source = json.getLong("source");
            this.localTimestamp = json.getLong("lts");
            this.remoteTimestamp = json.getLong("rts");
            this.payload = json.getJSONObject("payload");
            if (json.has("device")) {
                this.sourceDevice = new Device(json.getJSONObject("device"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
    }

    /**
     * 构造函数。
     *
     * @param src 指定复制源。
     */
    public Message(Message src) {
        this.domain = src.domain;
        this.id = src.id;
        this.from = src.from;
        this.to = src.to;
        this.source = src.source;
        this.localTimestamp = src.localTimestamp;
        this.remoteTimestamp = src.remoteTimestamp;
        this.payload = src.payload;
        this.sourceDevice = src.sourceDevice;
        this.state = src.state;

        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
    }

    /**
     * 构造函数。
     *
     * @param domain
     * @param id
     * @param from
     * @param to
     * @param source
     * @param localTimestamp
     * @param remoteTimestamp
     * @param payload
     */
    public Message(String domain, Long id, Long from, Long to, Long source,
                   Long localTimestamp, Long remoteTimestamp, JSONObject sourceDevice, JSONObject payload) {
        super(domain);
        this.id = id;
        this.from = from;
        this.to = to;
        this.source = source;
        this.localTimestamp = localTimestamp;
        this.remoteTimestamp = remoteTimestamp;
        this.payload = payload;
        this.sourceDevice = new Device(sourceDevice);

        this.state = MessageState.Sent;
        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
    }

    public String getUniqueKey() {
        return this.uniqueKey;
    }

    public Long getId() {
        return this.id;
    }

    public Long getFrom() {
        return this.from;
    }

    public Long getTo() {
        return this.to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public Long getSource() {
        return this.source;
    }

    public long getLocalTimestamp() {
        return this.localTimestamp;
    }

    public long getRemoteTimestamp() {
        return this.remoteTimestamp;
    }

    public void setRemoteTimestamp(long remoteTimestamp) {
        this.remoteTimestamp = remoteTimestamp;
    }

    public JSONObject getPayload() {
        return this.payload;
    }

    public void setSourceDevice(Device device) {
        this.sourceDevice = device;
    }

    public Device getSourceDevice() {
        return this.sourceDevice;
    }

    public void setState(MessageState state) {
        this.state = state;
    }

    public MessageState getState() {
        return this.state;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Message) {
            Message other = (Message) object;
            if (other.id.longValue() == this.id.longValue() && other.from.longValue() == this.from.longValue()
                && other.to.longValue() == this.to.longValue() && other.source.longValue() == this.source.longValue()) {
                return true;
            }
        }

        return false;
    }

    public JSONObject toJSON(boolean withPayload) {
        JSONObject json = this.toJSON();
        if (!withPayload) {
            json.remove("payload");
        }
        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("domain", this.domain.getName());
            json.put("id", this.id.longValue());
            json.put("from", this.from.longValue());
            json.put("to", this.to.longValue());
            json.put("source", this.source.longValue());
            json.put("lts", this.localTimestamp);
            json.put("rts", this.remoteTimestamp);
            json.put("payload", this.payload);

            if (null != this.sourceDevice) {
                json.put("device", this.sourceDevice.toJSON());
            }

            if (this.state != MessageState.Unknown) {
                json.put("state", this.state.getCode());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public int compareTo(Message other) {
        return (int)(this.remoteTimestamp - other.remoteTimestamp);
    }
}
