/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * AIGC 分配给用户的通道。
 */
public class AIGCChannel extends Entity {

    private String participant;

    private long creationTime;

    private String code;

    // 倒序存储历史记录
    private LinkedList<Record> history;

    public AIGCChannel(String participant) {
        this.participant = participant;
        this.creationTime = System.currentTimeMillis();
        this.code = Utils.randomString(16);
        this.history = new LinkedList<>();
    }

    public String getCode() {
        return this.code;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public String getParticipant() {
        return this.participant;
    }

    public Record appendHistory(String participant, String content) {
        Record record = new Record(participant, content, System.currentTimeMillis());
        this.history.addFirst(record);
        return record;
    }

    public List<Record> getLastHistory(int num) {
        List<Record> result = new ArrayList<>();

        for (int i = 0; i < this.history.size(); ++i) {
            result.add(this.history.get(i));
            if (result.size() >= num) {
                break;
            }
        }

        return result;
    }

    public JSONArray getLastParticipantHistory(int max) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < this.history.size(); ++i) {
            Record record = this.history.get(i);
            if (record.participant.equals(this.participant)) {
                list.add(record.content);
                if (list.size() >= max) {
                    break;
                }
            }
        }

        Collections.reverse(list);
        JSONArray result = new JSONArray();
        for (String content : list) {
            result.put(content);
        }
        return result;
    }


    public class Record implements JSONable {

        public String participant;

        public String content;

        public long timestamp;

        protected Record(String participant, String content, long timestamp) {
            this.participant = participant;
            this.content = content;
            this.timestamp = timestamp;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("participant", this.participant);
            json.put("content", this.content);
            json.put("timestamp", this.timestamp);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
