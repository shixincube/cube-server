/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.command;

import org.json.JSONObject;

/**
 * 搜索命令。
 */
public class SearchCommand extends Command {

    private String keyword;

    public SearchCommand(String keyword) {
        super("Search");
        this.keyword = keyword;
    }

    public String getKeyword() {
        return this.keyword;
    }

    @Override
    public void run() {

    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }
}
