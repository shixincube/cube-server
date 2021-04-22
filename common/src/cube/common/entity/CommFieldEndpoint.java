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
import cube.common.UniqueKey;
import cube.common.state.MultipointCommStateCode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 通讯场域里的媒体节点。
 */
public class CommFieldEndpoint extends Entity {

    /**
     * 节点名。
     */
    private String name;

    /**
     * 节点的联系人。
     */
    private Contact contact;

    /**
     * 节点的设备。
     */
    private Device device;

    /**
     * RTC 设备序号。
     */
    private Long rtcSN;

    /**
     * 当前节点的状态。
     */
    private MultipointCommStateCode state;

    /**
     * 最近一次修改状态时间戳。
     */
    private long lastModified = 0L;

    /**
     * 当前节点的 SDP 信息。
     */
    private JSONObject sessionDescription;

    /**
     * 当前节点的媒体约束。
     */
    private JSONObject mediaConstraint;

    /**
     * 当前节点提交的 ICE Candidate 数据。
     */
    private List<JSONObject> candidateList;

    /**
     * 视频是否启用。
     */
    private boolean videoEnabled = true;

    /**
     * 视频流是否开启。
     */
    private boolean videoStreamEnabled = true;

    /**
     * 音频是否启用。
     */
    private boolean audioEnabled = true;

    /**
     * 音频流是否开启。
     */
    private boolean audioStreamEnabled = true;

    /**
     * 节点就绪时间戳。
     */
    public long readyTimestamp = 0L;

    /**
     * 构造函数。
     *
     * @param id
     * @param contact
     * @param device
     */
    public CommFieldEndpoint(Long id, Contact contact, Device device) {
        super(id, contact.domain);

        this.contact = contact;
        this.device = device;
        this.state = MultipointCommStateCode.CallBye;
        this.lastModified = System.currentTimeMillis();

        this.name = contact.getUniqueKey() + "_" + device.getName() + "_" + device.getPlatform();
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public CommFieldEndpoint(JSONObject json) {
        super();

        this.id = json.getLong("id");
        this.domain = new Domain(json.getString("domain"));
        this.contact = new Contact(json.getJSONObject("contact"));
        this.device = new Device(json.getJSONObject("device"));
        this.name = json.getString("name");
        this.state = MultipointCommStateCode.match(json.getInt("state"));

        if (json.has("description")) {
            this.sessionDescription = json.getJSONObject("description");
        }

        if (json.has("constraint")) {
            this.mediaConstraint = json.getJSONObject("constraint");
        }

        if (json.has("video")) {
            JSONObject video = json.getJSONObject("video");
            this.videoEnabled = video.getBoolean("enabled");
            this.videoStreamEnabled = video.getBoolean("streamEnabled");
        }

        if (json.has("audio")) {
            JSONObject audio = json.getJSONObject("audio");
            this.audioEnabled = audio.getBoolean("enabled");
            this.audioStreamEnabled = audio.getBoolean("streamEnabled");
        }

        this.uniqueKey = UniqueKey.make(this.id, this.domain);
    }

    /**
     * 获取终端名称。
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取联系人实例。
     *
     * @return
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取设备实例。
     *
     * @return
     */
    public Device getDevice() {
        return this.device;
    }

    /**
     * 设置 RTC 设备的序号。
     *
     * @param rtcSN
     */
    public void setRTCSerialNumber(Long rtcSN) {
        this.rtcSN = rtcSN;
    }

    /**
     * 获取 RTC 设备的序号。
     *
     * @return
     */
    public Long getRTCSerialNumber() {
        return this.rtcSN;
    }

    /**
     * 设置 WebRTC 的 session description 。
     * @param sessionDescription
     */
    public void setSessionDescription(JSONObject sessionDescription) {
        this.sessionDescription = sessionDescription;
    }

    /**
     * 设置媒体约束。
     *
     * @param mediaConstraint
     */
    public void setMediaConstraint(JSONObject mediaConstraint) {
        this.mediaConstraint = mediaConstraint;
    }

    /**
     * 添加 ICE Candidate 。
     *
     * @param candidate
     */
    public synchronized void addCandidate(JSONObject candidate) {
        if (null == this.candidateList) {
            this.candidateList = new ArrayList<>();
        }

        this.candidateList.add(candidate);
    }

    /**
     * 获取 ICE Candidate 列表。
     *
     * @return
     */
    public List<JSONObject> getCandidates() {
        if (null == this.candidateList) {
            return new ArrayList<>();
        }

        return this.candidateList;
    }

    /**
     * 清空所有 ICE Candidate 。
     */
    public void clearCandidates() {
        if (null != this.candidateList) {
            this.candidateList.clear();
        }
    }

    /**
     * 设置状态。
     *
     * @param state
     */
    public void setState(MultipointCommStateCode state) {
        this.state = state;
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * 返回状态。
     *
     * @return
     */
    public MultipointCommStateCode getState() {
        return this.state;
    }

    /**
     * 返回最近一次修改状态的时间戳。
     *
     * @return
     */
    public long getLastModified() {
        return this.lastModified;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof CommFieldEndpoint) {
            CommFieldEndpoint other = (CommFieldEndpoint) object;
            if (other.name.equals(this.name)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("contact", this.contact.toBasicJSON());
        json.put("device", this.device.toCompactJSON());
        json.put("name", this.name);
        json.put("state", this.state.code);

        if (null != this.sessionDescription) {
            json.put("description", this.sessionDescription);
        }

        if (null != this.mediaConstraint) {
            json.put("constraint", this.mediaConstraint);
        }

        JSONObject video = new JSONObject();
        video.put("enabled", this.videoEnabled);
        video.put("streamEnabled", this.videoStreamEnabled);
        json.put("video", video);

        JSONObject audio = new JSONObject();
        audio.put("enabled", this.audioEnabled);
        audio.put("streamEnabled", this.audioStreamEnabled);
        json.put("audio", audio);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("contact", this.contact.toBasicJSON());
        json.put("device", this.device.toCompactJSON());
        json.put("name", this.name);
        json.put("state", this.state.code);

        JSONObject video = new JSONObject();
        video.put("enabled", this.videoEnabled);
        video.put("streamEnabled", this.videoStreamEnabled);
        json.put("video", video);

        JSONObject audio = new JSONObject();
        audio.put("enabled", this.audioEnabled);
        audio.put("streamEnabled", this.audioStreamEnabled);
        json.put("audio", audio);
        return json;
    }
}
