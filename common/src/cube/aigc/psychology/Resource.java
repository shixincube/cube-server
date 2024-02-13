/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.aigc.psychology;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源。
 */
public class Resource {

    private File commentDescriptionFile = new File("assets/psychology/interpretation.json");
    private long commentDescriptionLastModified = 0;
    private List<CommentInterpretation> commentInterpretations;

    private File themeFile = new File("assets/psychology/theme.json");
    private long themeLastModified = 0;
    private Map<String, ThemeTemplate> themeTemplates;

    private final static Resource instance = new Resource();

    private Resource() {
        this.commentInterpretations = new ArrayList<>();
        this.themeTemplates = new ConcurrentHashMap<>();
    }

    public static Resource getInstance() {
        return Resource.instance;
    }

    public List<CommentInterpretation> getCommentInterpretations() {
        if (this.commentDescriptionFile.exists()) {
            if (this.commentDescriptionFile.lastModified() != this.commentDescriptionLastModified) {
                this.commentDescriptionLastModified = this.commentDescriptionFile.lastModified();
                this.commentInterpretations.clear();

                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.commentDescriptionFile.getAbsolutePath()));
                    JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
                    for (int i = 0; i < array.length(); ++i) {
                        CommentInterpretation cd = new CommentInterpretation(array.getJSONObject(i));
                        this.commentInterpretations.add(cd);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return this.commentInterpretations;
    }

    public CommentInterpretation getCommentInterpretation(Comment comment) {
        if (this.commentInterpretations.isEmpty()) {
            this.getCommentInterpretations();
        }

        for (CommentInterpretation interpretation : this.commentInterpretations) {
            if (interpretation.getComment() == comment) {
                return interpretation;
            }
        }

        return null;
    }

    public ThemeTemplate getThemeTemplate(String name) {
        if (this.themeFile.exists()) {
            if (this.themeFile.lastModified() != this.themeLastModified) {
                this.themeLastModified = this.themeFile.lastModified();
                this.themeTemplates.clear();

                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.themeFile.getAbsolutePath()));
                    JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));
                    for (String key : json.keySet()) {
                        ThemeTemplate themeTemplate = new ThemeTemplate(key, json.getJSONObject(key));
                        this.themeTemplates.put(key, themeTemplate);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return this.themeTemplates.get(name);
    }
}
