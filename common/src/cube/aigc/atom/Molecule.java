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

package cube.aigc.atom;

import cube.common.entity.ChartSeries;
import org.json.JSONArray;

import java.util.*;

public class Molecule {

    public Molecule() {
    }

    public ChartSeries build(List<Atom> atomList, List<String> labelList) {
        // 从数据库里模糊匹配的词可能相关性很低，过滤规则：
        // 1. 最少2两个词命中
        // 2. 保留命中最多的 Atom 列表

        int maxMatching = 0;
        Iterator<Atom> iter = atomList.iterator();
        while (iter.hasNext()) {
            Atom atom = iter.next();
            int num = atom.numMatchingLabels(labelList);
            if (num > maxMatching) {
                maxMatching = num;
            }
            if (num < 2) {
                iter.remove();
            }
        }

        if (atomList.isEmpty()) {
            // 没有匹配的数据
            return null;
        }

        // 删除命中数量不是最大数据的 Atom
        iter = atomList.iterator();
        while (iter.hasNext()) {
            Atom atom = iter.next();
            if (atom.currentLabelMatchingNum < maxMatching) {
                iter.remove();
            }
        }

        Map<String, LinkedList<Atom>> sameLabelMap = new HashMap<>();

        // 分类，将标签相同分到一个列表里
        for (Atom atom : atomList) {
            LinkedList<Atom> atoms = sameLabelMap.computeIfAbsent(atom.label, k -> new LinkedList<>());
            atoms.add(atom);
        }

        // 排序，日期升序
        for (LinkedList<Atom> list : sameLabelMap.values()) {
            list.sort(new Comparator<Atom>() {
                @Override
                public int compare(Atom atom1, Atom atom2) {
                    String date1 = atom1.serializeDate();
                    String date2 = atom2.serializeDate();
                    date1 = date1.replace("年", "")
                            .replace("月", "")
                            .replace("日", "")
                            .replace("号", "");
                    date2 = date2.replace("年", "")
                            .replace("月", "")
                            .replace("日", "")
                            .replace("号", "");
                    try {
                        long v1 = Long.parseLong(date1);
                        long v2 = Long.parseLong(date2);
                        return (int)(v1 - v2);
                    } catch (Exception e) {
                        // Nothing
                    }

                    return 0;
                }
            });
        }

        return generateChartSeries(sameLabelMap.values());
    }

    private ChartSeries generateChartSeries(Collection<LinkedList<Atom>> list) {
        ArrayList<ChartSeries> seriesList = new ArrayList<>();

        for (LinkedList<Atom> atoms : list) {
            Atom first = atoms.get(0);
            Atom last = atoms.get(atoms.size() - 1);

            String label = first.label;
            String[] words = label.split(",");
            String name = words[0] + "-" + words[1];
            String desc = name + " - " + first.formatDate() + "至" + last.formatDate();

            // 图例名
            String legend = words[words.length - 1];

            ArrayList<String> xAxis = new ArrayList();
            ArrayList<String> xAxisDesc = new ArrayList<>();
            JSONArray data = new JSONArray();
            for (Atom atom : atoms) {
                xAxis.add(atom.formatSimpleDate());
                xAxisDesc.add(atom.formatDate());
                data.put(atom.value);
            }

            ChartSeries chartSeries = new ChartSeries(name, desc, System.currentTimeMillis());
            chartSeries.setXAxis(xAxis);
            chartSeries.setData("line", data, legend);
            chartSeries.setXAxisDesc(xAxisDesc);
            chartSeries.setTimeline(atoms);
            chartSeries.label = words[0];
            seriesList.add(chartSeries);
        }

        // 仅一条数据
        if (seriesList.size() == 1) {
            return seriesList.get(0);
        }

        // 合并多条数据
        ChartSeries result = seriesList.get(0);
        for (int i = 1; i < seriesList.size(); ++i) {
            result.mergeSeries(seriesList.get(i));
        }

        return result;
    }
}
