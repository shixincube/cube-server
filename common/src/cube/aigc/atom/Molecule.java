/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.atom;

import cell.util.log.Logger;
import cube.common.entity.Chart;

import java.util.*;

public class Molecule {

    public Molecule() {
    }

    public Chart build(List<Atom> atomList, List<String> labelList) {
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
                        // 解析失败时不参与排序
                        Logger.d(Molecule.class, "#compare - Failed to parse the date as a number: " + e.getMessage());
                    }

                    return 0;
                }
            });
        }

        return generateChart(sameLabelMap.values());
    }

    private Chart generateChart(Collection<LinkedList<Atom>> list) {
        // 图表生成逻辑已停用：历史实现依赖 Chart 的旧版 API，该 API 已随 Chart 重构移除。
        // 此处恒返回 null，因此调用方 Molecule#build 亦恒返回 null。恢复需按现行 Chart API 改写。
        return null;
    }
}
