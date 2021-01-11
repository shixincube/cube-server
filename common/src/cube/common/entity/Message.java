/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cube.common.Domain;
import cube.common.Packet;
import cube.common.UniqueKey;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 消息实体。
 */
public class Message extends Entity implements Comparable<Message> {

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
     * 副本持有人。
     */
    private Long owner = 0L;

    /**
     * 消息生成时的源时间戳。
     */
    private long localTimestamp;

    /**
     * 消息到达接入层时的时间戳。
     */
    private long remoteTimestamp;

    /**
     * 消息的文件附件。
     */
    private FileAttachment attachment;

    /**
     * 消息的来源设备。
     */
    private Device sourceDevice;

    /**
     * 消息状态。
     */
    private MessageState state = MessageState.Unknown;

    /**
     * 构造函数。
     *
     * @param packet 指定包含消息数据的数据包。
     */
    public Message(Packet packet) throws JSONException {
        super();

        this.domain = new Domain(packet.data.getString("domain"));
        this.id = packet.data.getLong("id");
        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());

        this.from = packet.data.getLong("from");
        this.to = packet.data.getLong("to");
        this.source = packet.data.getLong("source");
        this.localTimestamp = packet.data.getLong("lts");
        this.remoteTimestamp = packet.data.getLong("rts");
        this.state = MessageState.parse(packet.data.getInt("state"));

        this.payload = packet.data.getJSONObject("payload");

        if (packet.data.has("attachment")) {
            // 解析文件附件
            this.attachment = new FileAttachment(packet.data.getJSONObject("attachment"));
        }

        if (packet.data.has("device")) {
            this.sourceDevice = new Device(packet.data.getJSONObject("device"));
        }
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
            this.state = MessageState.parse(json.getInt("state"));

            if (json.has("owner")) {
                this.owner = json.getLong("owner");
            }

            if (json.has("payload")) {
                this.payload = json.getJSONObject("payload");
            }

            if (json.has("attachment")) {
                this.attachment = new FileAttachment(json.getJSONObject("attachment"));
            }

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
        super(src.id);

        this.domain = src.domain;
        this.from = src.from;
        this.to = src.to;
        this.source = src.source;
        this.owner = src.owner;
        this.localTimestamp = src.localTimestamp;
        this.remoteTimestamp = src.remoteTimestamp;
        this.state = src.state;
        this.payload = src.payload;
        this.attachment = src.attachment;
        this.sourceDevice = src.sourceDevice;

        this.uniqueKey = UniqueKey.make(this.id, this.domain.getName());
    }

    /**
     * 构造函数。
     *
     * @param domain 指定所在域。
     * @param id 指定消息 ID 。
     * @param from 指定消息发件人 ID 。
     * @param to 指定消息收件人 ID 。
     * @param source 指定消息来源群的 ID 。
     * @param owner 指定消息的副本持有人 ID 。
     * @param localTimestamp 指定消息生成时的本地时间戳。
     * @param remoteTimestamp 指定消息在服务器上被处理的时间戳。
     * @param state 指定消息状态。
     * @param sourceDevice 指定源设备。
     * @param payload 指定消息负载数据。
     * @param attachment 指定消息附件。
     */
    public Message(String domain, Long id, Long from, Long to, Long source, Long owner,
                   Long localTimestamp, Long remoteTimestamp, int state,
                   JSONObject sourceDevice, JSONObject payload, JSONObject attachment) {
        super(id, domain);
        this.from = from;
        this.to = to;
        this.source = source;
        this.owner = owner;
        this.localTimestamp = localTimestamp;
        this.remoteTimestamp = remoteTimestamp;
        this.state = MessageState.parse(state);
        this.sourceDevice = new Device(sourceDevice);

        this.payload = payload;

        if (null != attachment) {
            this.attachment = new FileAttachment(attachment);
        }
    }

    /**
     * 获取消息发件人 ID 。
     *
     * @return 返回消息发件人 ID 。
     */
    public Long getFrom() {
        return this.from;
    }

    /**
     * 获取消息收件人 ID 。
     *
     * @return 返回消息收件人 ID 。
     */
    public Long getTo() {
        return this.to;
    }

    /**
     * 设置消息收件人 ID 。
     *
     * @param to 新的收件人 ID 。
     */
    public void setTo(Long to) {
        this.to = to;
    }

    /**
     * 获取消息的来源群的 ID 。
     *
     * @return 返回消息的来源群的 ID 。
     */
    public Long getSource() {
        return this.source;
    }

    /**
     * 设置消息副本持有人。
     *
     * @param ownerId 指定持有人 ID 。
     */
    public void setOwner(Long ownerId) {
        this.owner = ownerId;
    }

    /**
     * 获取消息副本持有人。
     *
     * @return 返回消息副本持有人 ID 。
     */
    public Long getOwner() {
        return this.owner;
    }

    /**
     * 获取消息的本地时间戳。
     *
     * @return 返回消息的本地时间戳。
     */
    public long getLocalTimestamp() {
        return this.localTimestamp;
    }

    /**
     * 获取消息的服务器时间戳。
     *
     * @return 返回消息的服务器时间戳。
     */
    public long getRemoteTimestamp() {
        return this.remoteTimestamp;
    }

    /**
     * 设置消息的服务器时间戳。
     *
     * @param remoteTimestamp 指定服务器时间戳。
     */
    public void setRemoteTimestamp(long remoteTimestamp) {
        this.remoteTimestamp = remoteTimestamp;
    }

    /**
     * 获取消息负载数据。
     *
     * @return 返回 JSON 形式的负载数据。
     */
    public JSONObject getPayload() {
        return this.payload;
    }

    /**
     * 获取消息附件。
     *
     * @return 返回消息的文件附件。
     */
    public FileAttachment getAttachment() {
        return this.attachment;
    }

    /**
     * 设置消息发送时的设备。
     *
     * @param device 设备实例。
     */
    public void setSourceDevice(Device device) {
        this.sourceDevice = device;
    }

    /**
     * 获取消息发出的源设备。
     *
     * @return 返回消息发出的源设备，如果没有记录返回 {@code null} 值。
     */
    public Device getSourceDevice() {
        return this.sourceDevice;
    }

    /**
     * 设置消息状态。
     *
     * @param state 消息状态。
     */
    public void setState(MessageState state) {
        this.state = state;
    }

    /**
     * 获取消息状态。
     *
     * @return 返回消息状态。
     */
    public MessageState getState() {
        return this.state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Message) {
            Message other = (Message) object;
            if (other.id.longValue() == this.id.longValue()
                    && other.owner.longValue() == this.owner.longValue()) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("domain", this.domain.getName());
            json.put("id", this.id.longValue());
            json.put("from", this.from.longValue());
            json.put("to", this.to.longValue());
            json.put("source", this.source.longValue());
            json.put("owner", this.owner.longValue());
            json.put("lts", this.localTimestamp);
            json.put("rts", this.remoteTimestamp);
            json.put("state", this.state.getCode());

            if (null != this.payload) {
                json.put("payload", this.payload);
            }

            if (null != this.attachment) {
                json.put("attachment", this.attachment.toJSON());
            }

            if (null != this.sourceDevice) {
                json.put("device", this.sourceDevice.toCompactJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("payload")) {
            json.remove("payload");
        }
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Message other) {
        return (int)(this.remoteTimestamp - other.remoteTimestamp);
    }
}
