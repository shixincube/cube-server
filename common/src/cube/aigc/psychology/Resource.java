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

import cell.util.log.Logger;
import cube.aigc.psychology.algorithm.Benchmark;
import cube.aigc.psychology.algorithm.KnowledgeStrategy;
import cube.aigc.psychology.composition.Scale;
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

    /**
     * 绘画指导语。
     */
    public final static String Instruction = "请你在纸上至少画出“房、树、人”三个元素（其他元素任意选择），共同构成一副有意义的画面。绘画时间10到15分钟。";

    public final static String ReportTextFormat = "%s的报告描述";

    private File termDescriptionFile = new File("assets/psychology/interpretation.json");
    private long termDescriptionLastModified = 0;
    private List<KnowledgeStrategy> knowledgeStrategies;

    private File themeFile = new File("assets/psychology/theme.json");
    private long themeLastModified = 0;
    private Map<String, ThemeTemplate> themeTemplates;

    private File benchmarkScoreFile = new File("assets/psychology/benchmark.json");
    private long benchmarkScoreLastModified = 0;
    private Benchmark benchmark;

    private final static Resource instance = new Resource();

    private Resource() {
        this.knowledgeStrategies = new ArrayList<>();
        this.themeTemplates = new ConcurrentHashMap<>();
    }

    public static Resource getInstance() {
        return Resource.instance;
    }

    public List<KnowledgeStrategy> getTermInterpretations() {
        if (this.termDescriptionFile.exists()) {
            if (this.termDescriptionFile.lastModified() != this.termDescriptionLastModified) {
                this.termDescriptionLastModified = this.termDescriptionFile.lastModified();
                this.knowledgeStrategies.clear();

                Logger.i(this.getClass(), "Read term description file: " + this.termDescriptionFile.getAbsolutePath());

                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.termDescriptionFile.getAbsolutePath()));
                    JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
                    for (int i = 0; i < array.length(); ++i) {
                        KnowledgeStrategy cd = new KnowledgeStrategy(array.getJSONObject(i));
                        this.knowledgeStrategies.add(cd);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return this.knowledgeStrategies;
    }

    public KnowledgeStrategy getTermInterpretation(Term term) {
        if (this.knowledgeStrategies.isEmpty()) {
            this.getTermInterpretations();
        }

        for (KnowledgeStrategy interpretation : this.knowledgeStrategies) {
            if (interpretation.getTerm() == term) {
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

                Logger.i(this.getClass(), "Read the theme template file: " + this.themeFile.getAbsolutePath());

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

    public Benchmark getBenchmark() {
        if (this.benchmarkScoreFile.exists()) {
            if (this.benchmarkScoreFile.lastModified() != this.benchmarkScoreLastModified) {
                this.benchmarkScoreLastModified = this.benchmarkScoreFile.lastModified();

                Logger.i(this.getClass(), "Read benchmark file: " + this.benchmarkScoreFile.getAbsolutePath());

                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.benchmarkScoreFile.getAbsolutePath()));
                    JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));
                    this.benchmark = new Benchmark(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return this.benchmark;
    }

    public Scale loadScaleByFilename(String filename) {
        File file = new File("assets/psychology/questionnaires/" + filename + ".json");
        if (!file.exists()) {
            Logger.w(this.getClass(), "#loadScaleByFilename - Can NOT find file: " + file.getAbsolutePath());
            return null;
        }
        return new Scale(file);
    }
}
