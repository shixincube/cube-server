/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.i10n;

import cell.util.log.Logger;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class I10n {

    public final static String CN = "cn";

    public final static String EN = "en";

    private static JSONObject sCNData;
    private static JSONObject sENData;

    private I10n() {
    }

    public static String getApp(String language, String item) {
        try {
            JSONObject data = getLanguageData(language);
            return data.getJSONObject("App").getString(item);
        } catch (Exception e) {
            Logger.e(I10n.class, "#getApp", e);
            return null;
        }
    }

    private static JSONObject getLanguageData(String language) {
        JSONObject data = null;
        if (CN.equalsIgnoreCase(language)) {
            if (null == sCNData) {
                sCNData = load("cn.json");
            }
            data = sCNData;
        }
        else if (EN.equalsIgnoreCase(language)) {
            if (null == sENData) {
                sENData = load("en.json");
            }
            data = sENData;
        }
        return data;
    }

    private static JSONObject load(String filename) {
        Path path = Paths.get("assets/i10n/", filename);
        try {
            byte[] data = Files.readAllBytes(path);
            return new JSONObject(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(I10n.class, "#load", e);
            return null;
        }
    }
}
