/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

import cube.aigc.psychology.Role;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 陪练每一步产生互动记录表。
 */
public class CopilotSheet {

    private List<Record> records;

    public CopilotSheet() {
        this.records = new ArrayList<>();
    }

    public CopilotSheet(JSONObject json) {
        this.records = new ArrayList<>();
        JSONArray array = json.getJSONArray("records");
        for (int i = 0; i < array.length(); ++i) {
            Record record = new Record(array.getJSONObject(i));
            this.records.add(record);
        }
    }

    public List<Record> getRecords() {
        return this.records;
    }

    public void addRecords(CopilotSheet sheet) {
        this.records.addAll(sheet.getRecords());
    }

    public String getRecordsAsMarkdown() {
        StringBuilder buf = new StringBuilder();
        for (Record record : this.records) {
            buf.append(record.role == Role.Counselor ? "咨询师：" : "来访者：");
            buf.append(record.content);
            buf.append("\n");
        }
        return buf.toString();
    }

    public class Record {

        public Role role;

        public String content;

        public long timestamp;

        public Record(JSONObject json) {
            this.role = Role.parse(json.getString("role"));
            this.content = json.getString("content");
            this.timestamp = json.getLong("timestamp");
        }
    }
}
