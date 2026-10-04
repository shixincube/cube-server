/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.*;
import cube.aigc.psychology.app.Link;
import cube.aigc.psychology.composition.*;
import cube.common.Language;
import cube.aigc.text.Keyword;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 依赖宿主分词器的报告内容加工。
 *
 * <p>这两个方法都要按关键词匹配内容，必须使用宿主维护的词典与 TF-IDF 权重表，
 * 因此留在宿主侧，由心理学业务模块经能力接口调用。</p>
 *
 * <p>不依赖分词器的报告渲染方法在
 * {@code cube.service.psychology.scene.ReportRenderer}。</p>
 */
public class ContentTools {

    private ContentTools() {
    }

    public static void fillHexagonScoreDescription(Tokenizer tokenizer, HexagonDimensionScore hds, Language language) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        for (HexagonDimension dim : HexagonDimension.values()) {
            int score = hds.getDimensionScore(dim);
            String query = null;
            int rate = hds.getDimensionRate(dim);
            if (rate == IndicatorRate.None.value) {
                query = language.isChinese() ? "六维分析中" + dim.displayNameInChinese + "维度表现常规"
                    : "The " + dim.displayNameInEnglish + " dimension in six-dimensional analysis is generally normal";
            } else if (rate <= IndicatorRate.Low.value) {
                query = "六维分析中" + dim.displayNameInChinese + "维度得分低的表现";
            } else if (score >= IndicatorRate.High.value) {
                query = "六维分析中" + dim.displayNameInChinese + "维度得分高的表现";
            } else {
                query = "六维分析中" + dim.displayNameInChinese + "维度得分中等的表现";
            }

            List<String> keywordList = analyzer.analyzeOnlyWords(query, 7);

            Dataset dataset = Resource.getInstance().loadDataset();
            String answer = dataset.matchContent(keywordList.toArray(new String[0]), 7);
            if (null != answer) {
                hds.recordDescription(dim, answer);
            }
            else {
                Logger.e(ContentTools.class, "#fillHexagonScoreDescription - Answer is null: " + query);
            }
        }
    }

    public static String extract(String query, Tokenizer tokenizer) {
        Dataset dataset = Resource.getInstance().loadDataset();
        if (null == dataset) {
            Logger.w(ContentTools.class, "#extract - Read dataset failed");
            return null;
        }

        synchronized (dataset) {
            if (!dataset.hasAnalyzed()) {
                for (String question : dataset.getQuestions()) {
                    TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
                    List<String> keywords = analyzer.analyzeOnlyWords(question, 7);
                    // 填充问题关键词
                    dataset.fillQuestionKeywords(question, keywords.toArray(new String[0]),
                            tokenizer.sentenceProcess(TextUtils.filterPunctuation(question)).toArray(new String[0]));
                }
            }
            else {
                Logger.d(ContentTools.class, "#extract - The dataset is ready");
            }
        }

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        List<Keyword> keywordList = analyzer.analyze(query, 10);
        if (keywordList.isEmpty()) {
            Logger.w(ContentTools.class, "#extract - Query keyword is none");
            return null;
        }

        List<String> keywords = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            keywords.add(keyword.getWord());
        }

        List<String> result = dataset.searchContent(keywords.toArray(new String[0]), keywords.size());
        if (!result.isEmpty()) {
            return result.get(0);
        }

        return dataset.matchContent(keywords.toArray(new String[0]), 7);
    }
}
