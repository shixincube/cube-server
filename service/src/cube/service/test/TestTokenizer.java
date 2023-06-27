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

package cube.service.test;

import cube.service.tokenizer.SegToken;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.List;

public class TestTokenizer {

    public static void main(String[] args) {
        Tokenizer tokenizer = new Tokenizer();

        String[] sentences = new String[] {
                "北京最近一周的天气气温图表",
                "北京2023年6月的天气气温图表",
                "这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。",
                "我不喜欢日本和服。",
                "雷猴回归人间。",
                "工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作",
                "结果婚的和尚未结过婚的",
                "北京6月份的气温图表。针对6月份的气温数据，给一些防止中暑的办法",
                "根据北京6月份的天气数据，给我一些防暑方法"
        };

//        for (String sentence : sentences) {
//            List<SegToken> result = tokenizer.process(sentence, Tokenizer.SegMode.INDEX);
//            System.out.println(result.toString());
//        }

        System.out.println("----------------------------------------");

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer();
        for (String sentence : sentences) {
            List<Keyword> keywordList = analyzer.analyze(sentence, 5);
            for (Keyword keyword : keywordList) {
                System.out.print(keyword.getName() + ":" + keyword.getTfidfvalue() + ", ");
            }
            System.out.println("");
        }
    }
}
