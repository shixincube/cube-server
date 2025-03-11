/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.core.net.Endpoint;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

public class FileLabels {

    private FileLabels() {
    }

    public static JSONObject reviseFileLabel(JSONObject json, String token, Endpoint httpEndpoint, Endpoint httpsEndpoint) {
        String fileCode = json.getString("fileCode");
        String type = json.getString("fileType");

        if (json.has("fileURL") && null != httpEndpoint) {
            json.put("fileURL", "http://" + httpEndpoint.toString() + "/filestorage/file/?fc=" +
                    fileCode + "&token=" + token + "&type=" + type.toLowerCase());
        }

        if (json.has("fileSecureURL") && null != httpsEndpoint) {
            json.put("fileSecureURL", "https://" + httpsEndpoint.toString() + "/filestorage/file/?fc=" +
                    fileCode + "&token=" + token + "&type=" + type.toLowerCase());
        }

        if (json.has("directURL")) {
            json.remove("directURL");
        }

        return json;
    }

    public static String makeFileHttpsURL(FileLabel fileLabel, String token, Endpoint endpoint) {
        return "https://" + endpoint.toString() + "/filestorage/file/?fc=" +
                fileLabel.getFileCode() + "&token=" + token + "&type=" +
                fileLabel.getFileType().getPreferredExtension().toLowerCase();
    }
}
