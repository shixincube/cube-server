/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;
import cube.service.aigc.AIGCService;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GuideFlow {

    private long loadTimestamp;

    private String name;

    private String displayName;

    private String instruction;

    private List<GuidanceSection> sectionList;

    public GuideFlow(File file) {
        this.sectionList = new ArrayList<>();
        this.load(file);
    }

    public long getLoadTimestamp() {
        return this.loadTimestamp;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getInstruction() {
        return this.instruction;
    }

    public void start(AIGCService service) {

    }

    private void load(File file) {
        JSONObject jsonData = ConfigUtils.readJsonFile(file.getAbsolutePath());
        if (null == jsonData) {
            Logger.e(this.getClass(), "#load - Read file failed: " + file.getAbsolutePath());
            return;
        }

        this.name = jsonData.getString("name");
        this.displayName = jsonData.getString("displayName");
        this.instruction = jsonData.getString("instruction");
        JSONArray array = jsonData.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sectionList.add(new GuidanceSection(array.getJSONObject(i)));
        }
    }
}
