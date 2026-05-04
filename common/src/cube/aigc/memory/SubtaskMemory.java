/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.memory;

import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 子任务记忆。
 */
public class SubtaskMemory {

    private List<GeneratingRecord> records;

    public SubtaskMemory() {
        this.records = new ArrayList<>();
    }

    public void record(GeneratingRecord generatingRecord) {
        this.records.add(generatingRecord);
    }

    public GeneratingRecord getRecent() {
        if (this.records.isEmpty()) {
            return null;
        }
        return this.records.get(this.records.size() - 1);
    }

    public FileLabel getRecentFile() {
        for (int i = this.records.size() - 1; i >= 0; --i) {
            GeneratingRecord record = this.records.get(i);
            if (null != record.queryFileLabels && !record.queryFileLabels.isEmpty()) {
                return record.queryFileLabels.get(0);
            }
        }
        return null;
    }

    public String toMarkdown() {
        StringBuilder buf = new StringBuilder();
        for (GeneratingRecord record : this.records) {
            buf.append("用户：").append(record.query.trim()).append("\n\n");
            buf.append("云宝：").append(record.answer.trim()).append("\n\n");
        }
        return buf.toString();
    }
}
