/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.test;

import cell.util.Utils;
import cube.aigc.psychology.Dataset;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.Question;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScalesConfiguration;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.FloatUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestEvaluation {

    public static void testEvaluationReport() {
    }

    public static void testDataset() {
        System.out.println("testDataset");

        Tokenizer tokenizer = new Tokenizer();

        Dataset dataset = Resource.getInstance().loadDataset();
        if (null == dataset) {
            System.err.println("Dateset file error");
            return;
        }

        if (!dataset.hasAnalyzed()) {
            for (String question : dataset.getQuestions()) {
                TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
                List<Keyword> keywordList = analyzer.analyze(question, 5);
                if (keywordList.isEmpty()) {
                    continue;
                }

                List<String> keywords = new ArrayList<>();
                for (Keyword keyword : keywordList) {
                    keywords.add(keyword.getWord());
                }
                // 填充问题关键词
                dataset.fillQuestionKeywords(question, keywords.toArray(new String[0]));
            }
        }

        String query = "宜人性一般的表现";
        System.out.println("Query:\n" + query);

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        List<Keyword> keywordList = analyzer.analyze(query, 5);
        if (keywordList.isEmpty()) {
            System.err.println("Query keyword is none");
            return;
        }

        List<String> keywords = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            keywords.add(keyword.getWord());
        }

        String answer = dataset.matchContent(keywords.toArray(new String[0]), 5);
        System.out.println("Answer:\n" + answer);
    }

    public static void testBigFive() {
        System.out.println("testBigFive");

        double obligingness = FloatUtils.random(0.5, 10.0);
        double conscientiousness = FloatUtils.random(0.5, 10.0);
        double extraversion = FloatUtils.random(0.5, 10.0);
        double achievement = FloatUtils.random(0.5, 10.0);
        double neuroticism = FloatUtils.random(0.5, 10.0);

        BigFivePersonality feature = new BigFivePersonality(obligingness, conscientiousness, extraversion, achievement,
                neuroticism);

        System.out.println(feature.getDisplayName());
        System.out.println("obligingness: " + obligingness);
        System.out.println("conscientiousness: " + conscientiousness);
        System.out.println("extraversion: " + extraversion);
        System.out.println("achievement: " + achievement);
        System.out.println("neuroticism: " + neuroticism);
        System.out.println("----------------------------------------");
        System.out.println(BigFivePersonality.match(feature.getName()).getTemplateValue().toString());
    }

    public static void testScalesConfiguration() {
        System.out.println("testScalesConfiguration");

        ScalesConfiguration configuration = new ScalesConfiguration();

        ScalesConfiguration.Category category = configuration.getCategory(ScalesConfiguration.CATEGORY_PERSONALITY);
        System.out.println("Name: " + category.name);

        System.out.println("----------------------------------------");
    }

    public static void testListScales() {
        System.out.println("testListScales");

        List<File> fileList = Resource.getInstance().listScaleFiles();
        for (File file : fileList) {
            System.out.println(file.getAbsolutePath());
        }
    }

    public static void testMBTIScale() {
        System.out.println("testMBTIScale");

        Scale scale = Resource.getInstance().loadScaleByFilename("MBTI-16Personalities", 0);
//        System.out.println(scale.toMarkdown());

        System.out.println("Complete: " + scale.isComplete());

        System.out.println("----------------------------------------");

        for (Question question : scale.getQuestions()) {
            question.chooseAnswer(Utils.randomUnsigned() % 2 == 0 ? "A" : "B");
        }

        System.out.println("Complete: " + scale.isComplete());

        System.out.println("----------------------------------------");

        JSONObject conclusion = scale.scoring().toJSON();
        System.out.println(conclusion.toString(4));
    }

    public static void testSCL90Scale() {
        System.out.println("testSCL90Scale");

        Scale scale = Resource.getInstance().loadScaleByFilename("SCL-90", 0);
        for (Question question : scale.getQuestions()) {
            switch (Utils.randomInt(1, 5)) {
                case 1:
                    question.chooseAnswer("A");
                    break;
                case 2:
                    question.chooseAnswer("B");
                    break;
                case 3:
                    question.chooseAnswer("C");
                    break;
                case 4:
                    question.chooseAnswer("D");
                    break;
                case 5:
                    question.chooseAnswer("E");
                    break;
                default:
                    break;
            }
        }
        System.out.println("Complete: " + scale.isComplete());

        System.out.println("----------------------------------------");

        JSONObject conclusion = scale.scoring().toJSON();
        System.out.println(conclusion.toString(4));
    }

    public static void testBigFiveScale() {
        System.out.println("testBigFiveScale");

        Scale scale = Resource.getInstance().loadScaleByName("BigFive", 0);
        for (Question question : scale.getQuestions()) {
            switch (Utils.randomInt(1, 5)) {
                case 1:
                    question.chooseAnswer("A");
                    break;
                case 2:
                    question.chooseAnswer("B");
                    break;
                case 3:
                    question.chooseAnswer("C");
                    break;
                case 4:
                    question.chooseAnswer("D");
                    break;
                case 5:
                    question.chooseAnswer("E");
                    break;
                default:
                    break;
            }
        }
        System.out.println("Complete: " + scale.isComplete());

        System.out.println("----------------------------------------");

        JSONObject conclusion = scale.scoring().toJSON();
        System.out.println(conclusion.toString(4));
    }

    public static void testPANASScale() {
        System.out.println("testPANASScale");

        Scale scale = Resource.getInstance().loadScaleByName("PANAS", 0);

        AnswerSheet answerSheet = new AnswerSheet(scale.getSN());
        for (Question question : scale.getQuestions()) {
            switch (Utils.randomInt(1, 5)) {
                case 1:
                    answerSheet.submit(question.sn, "A");
                    break;
                case 2:
                    answerSheet.submit(question.sn, "B");
                    break;
                case 3:
                    answerSheet.submit(question.sn, "C");
                    break;
                case 4:
                    answerSheet.submit(question.sn, "D");
                    break;
                case 5:
                    answerSheet.submit(question.sn, "E");
                    break;
                default:
                    break;
            }
        }
        scale.submitAnswer(answerSheet);
        System.out.println("Valid: " + answerSheet.isValid());

//        for (Question question : scale.getQuestions()) {
//            switch (Utils.randomInt(1, 5)) {
//                case 1:
//                    question.chooseAnswer("A");
//                    break;
//                case 2:
//                    question.chooseAnswer("B");
//                    break;
//                case 3:
//                    question.chooseAnswer("C");
//                    break;
//                case 4:
//                    question.chooseAnswer("D");
//                    break;
//                case 5:
//                    question.chooseAnswer("E");
//                    break;
//                default:
//                    break;
//            }
//        }
        System.out.println("Complete: " + scale.isComplete());

        System.out.println("----------------------------------------");

        JSONObject conclusion = scale.scoring().toJSON();
        System.out.println(conclusion.toString(4));
    }

    public static void testPADScale() {
        System.out.println("testPADScale");

        Scale scale = Resource.getInstance().loadScaleByName("PAD", 0);
        for (Question question : scale.getQuestions()) {
            switch (Utils.randomInt(1, 9)) {
                case 1:
                    question.chooseAnswer("A");
                    break;
                case 2:
                    question.chooseAnswer("B");
                    break;
                case 3:
                    question.chooseAnswer("C");
                    break;
                case 4:
                    question.chooseAnswer("D");
                    break;
                case 5:
                    question.chooseAnswer("E");
                    break;
                case 6:
                    question.chooseAnswer("F");
                    break;
                case 7:
                    question.chooseAnswer("G");
                    break;
                case 8:
                    question.chooseAnswer("H");
                    break;
                case 9:
                    question.chooseAnswer("I");
                    break;
                default:
                    break;
            }
        }
        System.out.println("Complete: " + scale.isComplete());

        System.out.println("----------------------------------------");

        JSONObject conclusion = scale.scoring().toJSON();
        System.out.println(conclusion.toString(4));
    }

    public static void main(String[] args) {
//        testDataset();

//        testBigFive();

//        testScalesConfiguration();

//        testListScales();

//        testMBTIScale();

//        testSCL90Scale();

//        testBigFiveScale();

        testPANASScale();

//        testPADScale();
    }
}
