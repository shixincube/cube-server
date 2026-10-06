/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.utils;

import cell.util.log.Logger;
import cube.aigc.psychology.Dataset;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.aigc.psychology.composition.HexagonDimension;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.aigc.text.Keyword;
import cube.common.Language;
import cube.util.TextUtils;
import cube.util.tokenizer.Tokenizer;
import cube.util.tokenizer.keyword.TFIDFAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * 按关键词匹配语料的报告内容加工。
 *
 * <p>这两个方法要按 TF-IDF 权重匹配语料库中的问句与答案，
 * 分词与权重计算均以 {@code cube.util.tokenizer} 下的实现为准。</p>
 *
 * <p>不依赖分词的报告渲染方法在 {@link ReportRenderer}。
 * 本类的方法经宿主能力接口转发调用，故需接收宿主持有的分词器实例。</p>
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
