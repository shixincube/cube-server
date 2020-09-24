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
import cube.common.JSONable;
import cube.common.Packet;

/**
 * 消息实体。
 */
public class Message implements JSONable {

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
     * 消息状态。
     */
    private MessageState state = MessageState.Unknown;

    /**
     * 构造函数。
     * @param packet
     */
    public Message(Packet packet) {
        try {
            this.id = packet.data.getLong("id");
            this.from = packet.data.getLong("from");
            this.to = packet.data.getLong("to");
            this.source = packet.data.getLong("source");
            this.localTimestamp = packet.data.getLong("lts");
            this.remoteTimestamp = packet.data.getLong("rts");
            this.payload = packet.data.getJSONObject("payload");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造函数。
     * @param json
     */
    public Message(JSONObject json) {
        try {
            this.id = json.getLong("id");
            this.from = json.getLong("from");
            this.to = json.getLong("to");
            this.source = json.getLong("source");
            this.localTimestamp = json.getLong("lts");
            this.remoteTimestamp = json.getLong("rts");
            this.payload = json.getJSONObject("payload");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造函数。
     * @param src 指定复制源。
     */
    public Message(Message src) {
        this.id = src.id;
        this.from = src.from;
        this.to = src.to;
        this.source = src.source;
        this.localTimestamp = src.localTimestamp;
        this.remoteTimestamp = src.remoteTimestamp;
        this.payload = src.payload;
        this.state = src.state;
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

    public void setState(MessageState state) {

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
            json.put("id", this.id.longValue());
            json.put("from", this.from.longValue());
            json.put("to", this.to.longValue());
            json.put("source", this.source.longValue());
            json.put("lts", this.localTimestamp);
            json.put("rts", this.remoteTimestamp);
            json.put("payload", this.payload);

            if (this.state != MessageState.Unknown) {
                json.put("state", this.state.getCode());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
