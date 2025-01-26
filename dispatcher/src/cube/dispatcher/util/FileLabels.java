/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.util;

import cell.core.net.Endpoint;
import org.json.JSONObject;

public class FileLabels {

    private FileLabels() {
    }

    public static JSONObject reviseFileLabel(JSONObject json, String token, Endpoint httpEndpoint, Endpoint httpsEndpoint) {
        String fileCode = json.getString("fileCode");

        if (json.has("fileURL") && null != httpEndpoint) {
            json.put("fileURL", "http://" + httpEndpoint.toString() + "/filestorage/file/?fc=" +
                    fileCode + "&token=" + token);
        }

        if (json.has("fileSecureURL") && null != httpsEndpoint) {
            json.put("fileSecureURL", "https://" + httpsEndpoint.toString() + "/filestorage/file/?fc=" +
                    fileCode + "&token=" + token);
        }

        if (json.has("directURL")) {
            json.remove("directURL");
        }

        return json;
    }
}
