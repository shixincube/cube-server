/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.aigc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 提示词构建器。
 */
public class PromptBuilder {

    private final static String TOKEN_ROLE = "<|role|>";

    private final static String TOKEN_SYSTEM = "<|system|>";

    private final static String TOKEN_USER = "<|user|>";

    private final static String TOKEN_ASSISTANT = "<|assistant|>";

    private final static String TOKEN_OBSERVATION = "<|observation|>";

    private String hallucinationInhibitor = "给出的答案要避免内容重复。如果无法提供准确答案，就回答不知道。";

    public PromptBuilder() {
    }

//    public String serializePrompt(String system, String query) {
//        StringBuilder buf = new StringBuilder();
//        buf.append(TOKEN_ROLE).append(getDefaultRole()).append("\n");
//        buf.append(TOKEN_SYSTEM).append(this.filter(system)).append(this.hallucinationInhibitor).append("\n");
//        buf.append(TOKEN_USER).append(this.filter(query)).append("\n");
//        buf.append(TOKEN_ASSISTANT);
//        return buf.toString();
//    }

    public String serializePromptChaining(PromptChaining chaining) {
        StringBuilder buf = new StringBuilder();
        buf.append(TOKEN_ROLE).append(chaining.getRoleMetadata()).append("\n");
        for (Prompt prompt : chaining.getPrompts()) {
            buf.append(TOKEN_USER).append(prompt.query).append("\n");
            if (null != prompt.answer) {
                buf.append(TOKEN_ASSISTANT).append(prompt.answer).append("\n");
            }
        }
        return buf.toString();
    }

    public String serializeFromFile(File file) {
        JSONObject json = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
            StringBuilder buf = new StringBuilder();
            for (String line : lines) {
                buf.append(line);
            }
            json = new JSONObject(buf.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (!json.has("prompts")) {
            return null;
        }

        PromptChaining chaining = new PromptChaining();
        JSONArray prompts = json.getJSONArray("prompts");
        for (int i = 0; i < prompts.length(); ++i) {
            Prompt prompt = new Prompt(prompts.getJSONObject(i));
            chaining.addPrompt(prompt);
        }

        return serializePromptChaining(chaining);
    }

    public boolean deserializeToFile(PromptChaining chaining, File file) {
        return false;
    }

    private String getDefaultRole() {
        String role = "智能AI助手";
        return role;
    }

    private String filter(String input) {
        String result = input.replaceAll(TOKEN_ROLE, "");
        result = result.replaceAll(TOKEN_SYSTEM, "");
        result = result.replaceAll(TOKEN_USER, "");
        result = result.replaceAll(TOKEN_ASSISTANT, "");
        result = result.replaceAll(TOKEN_OBSERVATION, "");
        return result;
    }
}
