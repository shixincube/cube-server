/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 主描述内容文件。
 */
public class PrimaryContentFile {

    private JSONObject content;

    public PrimaryContentFile(String path) {
        Path file = Paths.get(path);
        try {
            byte[] data = Files.readAllBytes(file);
            this.content = new JSONObject(new String(data, Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回指定域的主描述内容。
     *
     * @param domainName
     * @return
     */
    public JSONObject getContent(String domainName) {
        if (null == this.content) {
            return null;
        }

        JSONObject json = null;
        try {
            json = this.content.getJSONObject(domainName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
