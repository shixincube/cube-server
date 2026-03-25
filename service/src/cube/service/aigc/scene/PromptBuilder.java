/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.service.aigc.guidance.Prompts;
import cube.util.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromptBuilder {

    private final String path = "assets/prompt/";

    public final String templateName;

    private String prompt;

    private Map<String, String> tagDataMap = new HashMap<>();

    public PromptBuilder(String templateName) {
        this.templateName = templateName;
        this.prompt = Prompts.getPrompt(templateName);
    }

    public void put(String key, String value) {
        this.tagDataMap.put(key, value);
    }

    public void putMap(Map<String, String> map) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            this.tagDataMap.put(e.getKey(), e.getValue());
        }
    }

    public String build() {
        if (null == this.prompt) {
            return null;
        }

        String result = this.prompt.toString();
        List<String> tags = TextUtils.extractPromptTemplateTag(this.prompt);
        for (String tag : tags) {
            String tagName = tag.replace("{{", "").replace("}}", "");
            if (!this.tagDataMap.containsKey(tagName)) {
                // 没有对应的数据
                // 加入特定数据
                if (tagName.contains("recommended_books")) {
                    String books = this.readMarkdown("books");
                    result = result.replace(tag, books);
                }
                continue;
            }

            result = result.replace(tag, this.tagDataMap.get(tagName));
        }
        return result;
    }

    private String readMarkdown(String name) {
        String filename = name;
        if (!name.endsWith(".md")) {
            filename = name + ".md";
        }

        FileInputStream fis = null;
        try {
            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            fis = new FileInputStream(new File(this.path, filename));
            byte[] bytes = new byte[10240];
            int length = 0;
            while ((length = fis.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }
            buf.flip();
            return new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readMarkdown", e);
            return null;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }
}
