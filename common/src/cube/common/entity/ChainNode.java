/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import cell.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 链条节点。
 */
public class ChainNode extends Entity {

    /**
     * 操作事件。
     */
    private String event;

    /**
     * 操作人。
     */
    private Entity who;

    /**
     * 被操作的实体。
     */
    private Entity what;

    /**
     * 操作时间。
     */
    private long when;

    /**
     * 做了什么操作方式。
     */
    private TransmissionMethod method;

    /**
     * 追踪轨道。
     */
    private List<String> tracks;

    private ChainNode previous;

    private ChainNode next;

    public ChainNode(String domain, String event, Entity who, Entity what, long when) {
        super(Utils.generateSerialNumber(), domain);
        this.event = event;
        this.who = who;
        this.what = what;
        this.when = when;
        this.tracks = new ArrayList<>();
    }

    public String getEvent() {
        return this.event;
    }

    public void addTrack(String track) {
        if (this.tracks.contains(track)) {
            return;
        }

        this.tracks.add(track);
    }

    public List<String> getTracks() {
        return this.tracks;
    }

    public TransmissionMethod getMethod() {
        return this.method;
    }

    public void setMethod(TransmissionMethod method) {
        this.method = method;
    }
}
