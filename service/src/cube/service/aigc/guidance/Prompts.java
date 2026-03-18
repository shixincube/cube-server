/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Prompts {

    private static long sGuideLastModified = 0;
    private static File sGuideFile = new File("assets/prompt/guide.json");
    private static Map<String, JSONObject> sGuidePromptMap = new ConcurrentHashMap<>();

    private static long sCatalogLastModified = 0;
    private static File sCatalogFile = new File("assets/prompt/catalog.json");
    private static Map<String, CatalogItem> sCatalogMap = new ConcurrentHashMap<>();

    public static String getPrompt(String name) {
        return Prompts.getPrompt(name, "cn");
    }

    public static String getPrompt(String name, String lang) {
        if (sGuideFile.exists() && sGuideFile.lastModified() != sGuideLastModified) {
            sGuideLastModified = sGuideFile.lastModified();
            sGuidePromptMap.clear();
            JSONObject data = ConfigUtils.readJsonFile(sGuideFile.getAbsolutePath());
            for (String key : data.keySet()) {
                JSONObject value = data.getJSONObject(key);
                sGuidePromptMap.put(key, value);
            }
        }

        JSONObject data = sGuidePromptMap.get(name);
        if (null != data) {
            return new String(data.getString(lang));
        }

        // 从目录文件中获取
        return getPromptFromCatalog(name);
    }

    private static String getPromptFromCatalog(String name) {
        if (sCatalogFile.exists() && sCatalogFile.lastModified() != sCatalogLastModified) {
            sCatalogLastModified = sCatalogFile.lastModified();
            sCatalogMap.clear();
            JSONObject data = ConfigUtils.readJsonFile(sCatalogFile.getAbsolutePath());
            JSONArray array = data.getJSONArray("files");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject itemJson = array.getJSONObject(i);
                CatalogItem item = new CatalogItem(itemJson.getString("name"), itemJson.getString("file"));
                sCatalogMap.put(item.name, item);
            }
        }

        CatalogItem item = sCatalogMap.get(name);
        if (null == item) {
            return null;
        }

        File file = new File("assets/prompt/" + item.filename);
        if (file.exists() && item.lastModified != file.lastModified()) {
            item.lastModified = file.lastModified();

            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                byte[] bytes = new byte[10240];
                int length = 0;
                while ((length = fis.read(bytes)) > 0) {
                    buf.put(bytes, 0, length);
                }
                buf.flip();

                item.content = new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                Logger.e(Prompts.class, "#getPromptFromCatalog", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        return new String(item.content);
    }

    public static class CatalogItem {
        public final String name;
        public final String filename;
        public long lastModified = 0;
        public String content;

        protected CatalogItem(String name, String filename) {
            this.name = name;
            this.filename = filename;
        }
    }
}
