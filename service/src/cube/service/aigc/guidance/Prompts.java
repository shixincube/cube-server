/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Prompts {

    private static long sLastModified = 0;
    private static File sFile = new File("assets/prompt/guide.json");

    private static Map<String, JSONObject> sPromptMap = new HashMap<>();

    public static String getPrompt(String name) {
        return Prompts.getPrompt(name, "cn");
    }

    public static String getPrompt(String name, String lang) {
        if (sFile.exists() && sFile.lastModified() != sLastModified) {
            sLastModified = sFile.lastModified();
            JSONObject data = ConfigUtils.readJsonFile(sFile.getAbsolutePath());
            for (String key : data.keySet()) {
                JSONObject value = data.getJSONObject(key);
                sPromptMap.put(key, value);
            }
        }

        JSONObject data = sPromptMap.get(name);
        if (null == data) {
            return null;
        }
        return data.getString(lang);
    }
}
