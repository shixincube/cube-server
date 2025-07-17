/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.algorithm.KnowledgeStrategy;
import cube.aigc.psychology.composition.Scale;
import cube.common.entity.Membership;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
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

    private File termDescriptionFile = new File("assets/psychology/interpretation.json");
    private long termDescriptionLastModified = 0;
    private List<KnowledgeStrategy> knowledgeStrategies;

    private File corpusFile = new File("assets/psychology/corpus.json");
    private long corpusLastModified = 0;
    private JSONObject corpusJson = null;

//    private File benchmarkScoreFile = new File("assets/psychology/benchmark.json");
//    private long benchmarkScoreLastModified = 0;
//    private Benchmark benchmark;

//    private File hexDimProjectionFile = new File("assets/psychology/projection.json");
//    private long hexDimProjectionModified = 0;
//    private HexagonDimensionProjection hexDimProjection;

    private File questionnairesPath = new File("assets/psychology/questionnaires/");
    private Map<String, File> scaleNameFileMap = new ConcurrentHashMap<>();

    private File datasetFile = new File("assets/psychology/dataset.json");
    private long datasetFileModified = 0;
    private Dataset dataset;

    private File attentionScriptFile = new File("assets/psychology/scripts/attention.js");
    private long attentionScriptFileModified = 0;
    private String attentionScriptContent = null;

    private File suggestionScriptFile = new File("assets/psychology/scripts/suggestion.js");
    private long suggestionScriptFileModified = 0;
    private String suggestionScriptFileContent = null;

    private File memberFile = new File("assets/psychology/membership.json");
    private long memberFileModified = 0;
    private JSONObject membershipData;

    private File mandalaFlowerPath = new File("assets/psychology/mandalaflower/");

    private final static Resource instance = new Resource();

    private Resource() {
        this.knowledgeStrategies = new ArrayList<>();
//        this.themeTemplates = new ConcurrentHashMap<>();
    }

    public static Resource getInstance() {
        return Resource.instance;
    }

    public synchronized List<KnowledgeStrategy> loadTermInterpretations() {
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
        this.loadTermInterpretations();

        for (KnowledgeStrategy interpretation : this.knowledgeStrategies) {
            if (interpretation.getTerm() == term) {
                return interpretation;
            }
        }

        return null;
    }

    private JSONObject loadMembershipData() {
        if (this.memberFile.exists()) {
            if (this.memberFile.lastModified() != this.memberFileModified) {
                this.memberFileModified = this.memberFile.lastModified();

                Logger.i(this.getClass(), "Read member file: " + this.memberFile.getAbsolutePath());

                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.memberFile.getAbsolutePath()));
                    this.membershipData = new JSONObject(new String(data, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return this.membershipData;
    }

    public List<String> getMemberBenefits(String memberType) {
        JSONObject data = this.loadMembershipData();
        if (null == data) {
            return null;
        }

        data = data.getJSONObject("benefits");
        JSONArray list = data.has(memberType) ? data.getJSONArray(memberType) : data.getJSONArray(Membership.TYPE_PREMIUM);
        return JSONUtils.toStringList(list);
    }

//    public Benchmark getBenchmark() {
//        if (this.benchmarkScoreFile.exists()) {
//            if (this.benchmarkScoreFile.lastModified() != this.benchmarkScoreLastModified) {
//                this.benchmarkScoreLastModified = this.benchmarkScoreFile.lastModified();
//
//                Logger.i(this.getClass(), "Read benchmark file: " + this.benchmarkScoreFile.getAbsolutePath());
//
//                try {
//                    byte[] data = Files.readAllBytes(Paths.get(this.benchmarkScoreFile.getAbsolutePath()));
//                    JSONArray jsonArray = new JSONArray(new String(data, StandardCharsets.UTF_8));
//                    this.benchmark = new Benchmark(jsonArray);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        return this.benchmark;
//    }

//    public HexagonDimensionProjection getHexDimProjection() {
//        if (this.hexDimProjectionFile.exists()) {
//            if (this.hexDimProjectionFile.lastModified() != this.hexDimProjectionModified) {
//                this.hexDimProjectionModified = this.hexDimProjectionFile.lastModified();
//
//                Logger.i(this.getClass(), "Read projection file: " + this.hexDimProjectionFile.getAbsolutePath());
//
//                try {
//                    byte[] data = Files.readAllBytes(Paths.get(this.hexDimProjectionFile.getAbsolutePath()));
//                    JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));
//                    this.hexDimProjection = new HexagonDimensionProjection(json);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        return this.hexDimProjection;
//    }

    public File getQuestionnairesPath() {
        return this.questionnairesPath;
    }

    public List<File> listScaleFiles() {
        List<File> result = new ArrayList<>();
        File[] files = this.questionnairesPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (!name.startsWith("_") && name.endsWith(".json"));
            }
        });
        if (null == files) {
            return result;
        }

        for (File file : files) {
            result.add(file);
        }
        return result;
    }

    public List<Scale> listScales(long contactId) {
        List<Scale> result = new ArrayList<>();
        List<File> files = this.listScaleFiles();
        for (File file : files) {
            try {
                Scale scale = new Scale(file, contactId);
                result.add(scale);
                // 量表名对应文件
                this.scaleNameFileMap.put(scale.name, file);
            } catch (Exception e) {
                Logger.w(this.getClass(), "#listScales - File format error: " + file.getAbsolutePath(), e);
            }
        }
        return result;
    }

    public Scale loadScaleByName(String name, long contactId) {
        File file = this.scaleNameFileMap.get(name);
        if (null == file) {
            List<Scale> list = this.listScales(contactId);
            for (Scale scale : list) {
                if (scale.name.equalsIgnoreCase(name)) {
                    return scale;
                }
            }
        }
        else {
            try {
                return new Scale(file, contactId);
            } catch (Exception e) {
                Logger.w(this.getClass(), "#loadScaleByName - File error: " + file.getAbsolutePath(), e);
            }
        }

        return null;
    }

    public Scale loadScaleByFilename(String filename, long contactId) {
        File file = new File(this.questionnairesPath, filename.endsWith(".json") ? filename : filename + ".json");
        if (!file.exists()) {
            Logger.w(this.getClass(), "#loadScaleByFilename - Can NOT find file: " + file.getAbsolutePath());
            return null;
        }
        try {
            Scale scale = new Scale(file, contactId);
            this.scaleNameFileMap.put(scale.name, file);
            return scale;
        } catch (Exception e) {
            Logger.w(this.getClass(), "#loadScaleByFilename - File format error: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    public Dataset loadDataset() {
        if (this.datasetFile.exists()) {
            if (this.datasetFile.lastModified() != this.datasetFileModified) {
                this.datasetFileModified = this.datasetFile.lastModified();

                Logger.i(this.getClass(), "Read dataset file: " + this.datasetFile.getAbsolutePath());

                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.datasetFile.getAbsolutePath()));
                    this.dataset = new Dataset(new JSONArray(new String(data, StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#loadDataset", e);
                }
            }
        }

        return this.dataset;
    }

    public String loadAttentionScript() {
        if (this.attentionScriptFile.exists()) {
            if (this.attentionScriptFileModified != this.attentionScriptFile.lastModified()) {
                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.attentionScriptFile.getAbsolutePath()));
                    this.attentionScriptContent = new String(data, StandardCharsets.UTF_8);
                    this.attentionScriptFileModified = this.attentionScriptFile.lastModified();
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#loadAttentionScript", e);
                }
            }
        }

        return this.attentionScriptContent;
    }

    public String loadSuggestionScript() {
        if (this.suggestionScriptFile.exists()) {
            if (this.suggestionScriptFileModified != this.suggestionScriptFile.lastModified()) {
                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.suggestionScriptFile.getAbsolutePath()));
                    this.suggestionScriptFileContent = new String(data, StandardCharsets.UTF_8);
                    this.suggestionScriptFileModified = this.suggestionScriptFile.lastModified();
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#loadSuggestionScript", e);
                }
            }
        }

        return this.suggestionScriptFileContent;
    }

    public String getCorpus(String category, String content) {
        return this.getCorpus(category, content, "cn");
    }

    public String getCorpus(String category, String content, String lang) {
        if (this.corpusFile.exists()) {
            if (this.corpusFile.lastModified() != this.corpusLastModified) {
                try {
                    byte[] data = Files.readAllBytes(Paths.get(this.corpusFile.getAbsolutePath()));
                    this.corpusJson = new JSONObject(new String(data, StandardCharsets.UTF_8));
                    this.corpusLastModified = this.corpusFile.lastModified();
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#getCorpus", e);
                }
            }
        }
        if (null == this.corpusJson) {
            return "";
        }
        JSONObject categoryJson = this.corpusJson.getJSONObject(category);
        JSONObject contentJson = categoryJson.getJSONObject(content);
        return contentJson.getString(lang.toLowerCase());
    }

    public List<String> getMandalaFlowerFiles() {
        List<String> result = new ArrayList<>();
        File[] files = this.mandalaFlowerPath.listFiles();
        if (null != files && files.length > 0) {
            for (File file : files) {
                if (file.getName().endsWith("jpg")) {
                    result.add(file.getName());
                }
            }
        }
        return result;
    }
}
