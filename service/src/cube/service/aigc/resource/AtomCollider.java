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

    public AtomCollider(AIGCStorage storage) {
        this.storage = storage;
    }

    public ChartSeries collapse(List<String> words) {
        Molecule molecule = new Molecule();

        // 将词分为标签和日期，进行快速标记
        List<String> labels = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        for (String word : words) {
            if (TextUtils.isDateString(word)) {
                dates.add(TextUtils.convChineseToArabicNumerals(word));
            }
            else {
                labels.add(word);
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
            // 没有年，补齐
            Calendar calendar = Calendar.getInstance();
            dates.add(calendar.get(Calendar.YEAR) + "年");
        }
        if (!hitMonth) {
            // 没有月，用当月补齐
            Calendar calendar = Calendar.getInstance();
            dates.add((calendar.get(Calendar.MONTH) + 1) + "月");
        }

        // 全匹配，把所有符合标签的 Atom 都匹配出来
        List<Atom> atomList = this.storage.fullMatching(labels, guessYear(dates), guessMonth(dates), guessDate(dates));
        // 计算
        return molecule.build(atomList, labels);
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
}
