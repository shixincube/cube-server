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

package cube.service.aigc.resource;

import cube.aigc.attachment.DatePicker;
import cube.common.entity.Stage;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 互动舞台调度和管理。
 */
public class StageDirector {

    private final static Pattern sDateRangePattern = Pattern.compile("(\\d+)年(\\d+)月到(\\d+)月");

    private final Tokenizer tokenizer;

    private Map<Long, Stage> stageMap;

    public StageDirector(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.stageMap = new ConcurrentHashMap<>();
    }

    public String settingUp(String query, String answer) {
        return answer;
    }

    public Stage infer(String content) {
        Stage stage = null;

        DatePicker datePicker = this.inferDatePicker(content);
        if (null != datePicker) {
            stage = new Stage();
            stage.addComponent(datePicker);
        }

        if (null != stage) {
            this.stageMap.put(stage.getId(), stage);
        }

        return stage;
    }

    private DatePicker inferDatePicker(String content) {
        DatePicker datePicker = null;

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<Keyword> keywordList = analyzer.analyze(content, 10);

        boolean hitDate = false;
        // 前3个关键词是否命中日期
        for (int i = 0; i < 3 && i < keywordList.size(); ++i) {
            String word = keywordList.get(i).getWord();
            if (word.contains("年")) {
                hitDate = true;
                break;
            }
            else if (word.contains("月")) {
                hitDate = true;
                break;
            }
        }

        if (hitDate) {
            // 提取日期范围
            Matcher matcher = sDateRangePattern.matcher(content);
            if (matcher.find()) {
                String yearStr = matcher.group(1);
                String month1Str = matcher.group(2);
                String month2Str = matcher.group(3);

                int year = Integer.parseInt(yearStr);
                int month1 = Integer.parseInt(month1Str);
                int month2 = Integer.parseInt(month2Str);

                Calendar calendar1 = Calendar.getInstance();
                calendar1.set(year, month1 - 1, 1);

                Calendar calendar2 = Calendar.getInstance();
                calendar2.set(year, month2 - 1, 1);

                datePicker = new DatePicker(DatePicker.TYPE_DATE_RANGE);
                datePicker.setRange(calendar1.getTimeInMillis(), calendar2.getTimeInMillis());
            }
        }

        return datePicker;
    }
}
