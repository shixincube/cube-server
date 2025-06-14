/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.test;

import cube.service.tokenizer.SegToken;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.TextUtils;

import java.util.List;

public class TestTokenizer {

    public double sentenceSimilarity(String sentenceA, String sentenceB, Tokenizer tokenizer) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        List<String> wordsA = analyzer.analyzeOnlyWords(sentenceA, 10);
        List<String> wordsB = analyzer.analyzeOnlyWords(sentenceB, 10);
        List<String> pole = null;
        List<String> monkey = null;
        if (wordsA.size() > wordsB.size()) {
            pole = wordsA;
            monkey = wordsB;
        }
        else {
            pole = wordsB;
            monkey = wordsA;
        }
        double count = 0;
        for (String word : pole) {
            if (monkey.contains(word)) {
                count += 1.0;
            }
        }
        return count / pole.size();
    }

    public static void main(String[] args) {
        Tokenizer tokenizer = new Tokenizer();

        String[] sentences = new String[] {
//                "北京最近一周的天气气温图表",
//                "北京2023年6月的天气气温图表",
//                "这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。",
//                "我不喜欢日本和服。",
//                "雷猴回归人间。",
//                "工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作",
//                "结果婚的和尚未结过婚的",
//                "北京6月份的气温图表。针对6月份的气温数据，给一些防止中暑的办法",
//                "根据北京6月份的天气数据，给我一些防暑方法",
//                "请作为心理学专家完成后续问答",

//                "把业务三部2022年1月到12月的差旅费用按照月为单位统计成图表进行展示。",
//                "把业务三部2022年全年的差旅费用按照月为单位统计成图表进行展示。",
//                "展示汤臣倍健2022年的舆情数据图表",
//                "根据汤臣倍健2023年6月的舆情图表数据，撰写汤臣倍健6月份的舆情报告。",

//                "自我存在是个体体验到自身是能思想、能感知、有情感、能行动的统一体，是扮演各种“角色”之总和，从中能看到并意识到自己的这些不同方面。",

//                "黟县报告里关于交通组织问题的描述《相关说明文件》",
//                "YX-220227-黟县国际乡村旅游度假区整改报告，重点描述,内容概要",

//                "他的心理症状会影响他最近的工作状态吗？",
//                "他的性格适合销售岗位工作吗？",
//                "他的人格特点是什么？",
//                "他能否胜任部门主管的岗位？",

//                "抑郁倾向低",
//                "抑郁倾向中等",
//                "抑郁倾向高",
//                "最近一次评测"
//                "轻度焦虑",
//                "不太重视家庭关系",
//                "中度的成就动机的报告描述",
//                "无明显躯体化症状的描述和表现",
//                "无明显躯体化症状的建议",
//                "有较轻躯体化症状的描述和表现",
//                "有较轻躯体化症状的建议",
//                "有躯体化症状的描述和表现",
//                "有躯体化症状的建议",
//                "有明显躯体化症状的描述和表现",
//                "有明显躯体化症状的建议",
//                "通才画像报告",
//                "高宜人性",
//                "宜人性的描述",
//                "高分宜人性表现",
//                "低分宜人性表现",
//                "宜人性一般的表现",
//                "低分情绪性表现",
//                "不太重视家庭关系的建议",
//                "适应者画像报告",
//                "建筑师的思维方式",
//                "无明显躯体化症状的描述和表现",
//                "创造力一般的报告描述",
//                "有较强的创造力的报告描述",
//                "自我意识不强的报告描述",
//                "自我意识强的报告描述",
//                "自我意识中等的报告描述",
//                "自信心较强的报告描述",
//                "较为依赖环境的报告描述",
//                "较弱的成就动机的报告描述",
//                "大五人格的团队全景图位于外向性进取性区域的人特点",
//                "大五人格的团队全景图位于宜人性外向性区域的人特点",
//                "大五人格的团队全景图位于进取性尽责性区域的人特点",
//                "大五人格的团队全景图位于宜人性尽责性区域的人特点",
//                "宜人性是什么意思？",
//                "尽责性是什么意思？",
//                "外向性是什么意思？",
//                "进取性是什么意思？",
//                "情绪性是什么意思？",
//                "高分宜人性的优势",
//                "高分宜人性的风险",
//                "低分宜人性的优势",
//                "低分宜人性的风险",
//                "六维分析中情绪维度得分高的表现",
//                "六维分析中情绪维度得分中等的表现",
//                "六维分析中情绪维度得分低的表现",
//                "六维分析中认知维度得分高的表现",
//                "六维分析中认知维度得分中等的表现",
//                "六维分析中认知维度得分低的表现",
//                "六维分析中行为维度得分高的表现",
//                "六维分析中行为维度得分中等的表现",
//                "六维分析中行为维度得分低的表现",
//                "六维分析中人际关系维度得分高的表现",
//                "六维分析中人际关系维度得分中等的表现",
//                "六维分析中人际关系维度得分低的表现",
//                "六维分析中自我评价维度得分高的表现",
//                "六维分析中自我评价维度得分中等的表现",
//                "六维分析中自我评价维度得分低的表现",
//                "六维分析中心理健康维度得分高的表现",
//                "六维分析中心理健康维度得分中等的表现",
//                "六维分析中心理健康维度得分低的表现"
//                "爱心理是个什么产品",
//                "爱心理平台是什么产品",
//                "打开第1份报告",
//                "第九个"
//                "进行绘画测验说明", "绘画指导", "绘画注意事项", "如何进行绘画评测",
//                "我应该如何进行绘画评测",

//                "症状自评量表SCL-90",
                "诊断一下我的心理健康状态",
                "猜猜，我的姓什么？",
                "男39",
                "选择123456号评测记录",
                "选择123456号报告",
                "选择编号123456的报告"
        };

        // 分词
        for (String sentence : sentences) {
//            List<SegToken> result = tokenizer.process(sentence, Tokenizer.SegMode.SEARCH);
            List<String> result = tokenizer.sentenceProcess(sentence);
            System.out.println(result.toString());
        }

        System.out.println("----------------------------------------");

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        for (String sentence : sentences) {
            List<Keyword> keywordList = analyzer.analyze(sentence, 10);
            for (Keyword keyword : keywordList) {
                System.out.print(keyword.getWord() + ":" + keyword.getTfidfValue() + ", ");
            }
            System.out.println("");
        }

//        TestTokenizer test = new TestTokenizer();
//        double source = test.sentenceSimilarity(sentences[3], sentences[4], tokenizer);
//        System.out.println(source);
    }
}
