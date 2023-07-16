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

import cube.aigc.Consts;
import cube.aigc.atom.Atom;
import cube.aigc.atom.Molecule;
import cube.common.entity.ChartSeries;
import cube.service.aigc.AIGCStorage;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 原子数据合成。
 */
public class AtomCollider {

    private final AIGCStorage storage;

    public List<String> labelList;

    public int year = 0;

    public List<ChartSeries> chartSeriesList;

    public String recommendWord = null;

    public int recommendYear = 0;

    public AtomCollider(AIGCStorage storage) {
        this.storage = storage;
        this.labelList = new ArrayList<>();
        this.chartSeriesList = new ArrayList<>();
    }

    public void collapse(List<String> words) {
        Molecule molecule = new Molecule();

        // 将词分为标签和日期，进行快速标记
        List<String> dates = new ArrayList<>();
        for (String word : words) {
            if (TextUtils.isDateString(word)) {
                dates.add(TextUtils.convChineseToArabicNumerals(word));
            }
            else {
                this.labelList.add(word);
            }
        }

        // 判断日期是否需要补齐
        boolean hitYear = false;
        boolean hitMonth = false;
        for (String date : dates) {
            if (!hitYear && date.contains("年")) {
                hitYear = true;
            }
            if (!hitMonth && date.contains("月")) {
                hitMonth = true;
            }
        }
        if (!hitYear) {
            // 没有年，补齐年
            Calendar calendar = Calendar.getInstance();
            dates.add(calendar.get(Calendar.YEAR) + "年");
        }
        if (!hitYear && !hitMonth) {
            // 没有年，没有月，补齐月
            Calendar calendar = Calendar.getInstance();
            dates.add((calendar.get(Calendar.MONTH) + 1) + "月");
        }

        String yearDesc = guessYear(dates);
        String monthDesc = guessMonth(dates);
        String dateDesc = guessDate(dates);

        this.year = this.extractYear(yearDesc);

        // 全匹配，把所有符合标签的 Atom 都匹配出来
        List<Atom> atomList = this.storage.fullMatching(this.labelList, yearDesc, monthDesc, dateDesc);
        // 将 Atom 列表生成位图表序列
        ChartSeries result = molecule.build(atomList, this.labelList);
        if (null != result) {
            this.chartSeriesList.add(result);
            return;
        }

        // 未找到数据，进行日期猜测
        try {
            Calendar calendar = Calendar.getInstance();
            int thisYear = calendar.get(Calendar.YEAR);
            int year = this.year;
            if (year == thisYear) {
                // 前推一年
                year = year - 1;
            }
            else {
                // 设定为今年
                year = thisYear;
            }

            boolean exists = this.storage.existsAtoms(this.labelList, year + "年", monthDesc);
            if (exists) {
                // 推测的年份存在数据
                this.recommendYear = year;
                this.recommendWord = String.format(Consts.ANSWER_FIND_SOME_YEAR_DATA,
                        yearDesc, year + "年");
            }
        } catch (Exception e) {
            // Nothing
        }
    }

    private String guessYear(List<String> dates) {
        for (String value : dates) {
            if (value.contains("年")) {
                return value;
            }
        }
        return null;
    }

    private String guessMonth(List<String> dates) {
        for (String value : dates) {
            if (value.contains("月")) {
                return value;
            }
        }
        return null;
    }

    private String guessDate(List<String> dates) {
        for (String value : dates) {
            if (value.contains("日") || value.contains("号")) {
                return value;
            }
        }
        return null;
    }

    private int extractYear(String desc) {
        return Integer.parseInt(desc.replace("年", ""));
    }
}
