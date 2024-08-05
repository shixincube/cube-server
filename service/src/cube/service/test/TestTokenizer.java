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

package cube.service.test;

import cube.service.tokenizer.SegToken;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.TextUtils;

import java.util.List;

public class TestTokenizer {

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

                "他的心理症状会影响他最近的工作状态吗？",
                "他的性格适合销售岗位工作吗？",
                "他的人格特点是什么？",
                "他能否胜任部门主管的岗位？",
                "中度的成就动机的报告描述"
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
    }
}
