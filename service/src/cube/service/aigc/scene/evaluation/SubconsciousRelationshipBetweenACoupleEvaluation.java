/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.evaluation;

import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.House;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Tree;
import cube.aigc.psychology.material.other.DrawingSet;
import cube.aigc.psychology.material.tree.Root;
import cube.util.FloatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubconsciousRelationshipBetweenACoupleEvaluation extends Evaluation {
    /**
     * 指标。
     */
    public enum SRBCIndicator implements Indicable {
        /**
         * 榕树型。
         */
        BanyanTree("BanyanTree", "BanyanTree", 9, "榕树型"),

        /**
         * 橡树型。
         */
        OakTree("OakTree", "OakTree", 9, "橡树型"),

        /**
         * 藤蔓型。
         */
        Vine("Vine", "Vine", 9, "藤蔓型"),

        /**
         * 向日葵型。
         */
        Sunflower("Sunflower", "Sunflower", 9, "向日葵型"),

        /**
         * 仙人掌型。
         */
        Cactus("Cactus", "Cactus", 9, "仙人掌型"),

        /**
         * 竹子型。
         */
        Bamboo("Bamboo", "Bamboo", 9, "竹子型"),

        /**
         * 并蒂莲型。
         */
        TwinLotus("TwinLotus", "TwinLotus", 9, "并蒂莲型"),

        /**
         * 蒲公英型。
         */
        Dandelion("Dandelion", "Dandelion", 9, "蒲公英型"),

        /**
         * 雪松型。
         */
        Cedar("Cedar", "Cedar", 9, "雪松型"),

        /**
         * 未知。
         */
        Unknown("Unknown", "Unknown", 0, "未知")

        ;

        private final String name;

        private final String code;

        private final int priority;

        private final String word;

        SRBCIndicator(String name, String code, int priority, String word) {
            this.name = name;
            this.code = code;
            this.priority = priority;
            this.word = word;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getCode() {
            return this.code;
        }

        @Override
        public int getPriority() {
            return this.priority;
        }

        public String getWord() {
            return this.word;
        }
    }

    /**
     * 核心词。
     */
    public enum SRBCWord {

        Harbour(1, "港湾"),

        Battlefield(2, "战场"),

        Stage(3, "舞台"),

        Post(4, "驿站"),

        Chains(5, "枷锁"),

        Wing(6, "翅膀"),

        Lights(7, "灯火"),

        Tide(8, "潮汐"),

        Melody(9, "旋律"),

        Root(10, "根系"),

        Shadow(11, "影子"),

        Maze(12, "迷宫"),

        Unknown(13, ""),

        ;

        public final int sn;

        public final String word;

        SRBCWord(int sn, String word) {
            this.sn = sn;
            this.word = word;
        }

        public final static SRBCWord parse(String word) {
            for (SRBCWord srbcWord : SRBCWord.values()) {
                if (srbcWord.word.equalsIgnoreCase(word)) {
                    return srbcWord;
                }
            }
            return Unknown;
        }
    }

    private final static HashMap<SRBCWord, Score[]> sWordMap = new HashMap<>();

    static {
        // 港湾
        sWordMap.put(SRBCWord.Harbour, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 2),
                new Score(SRBCIndicator.OakTree, 2),
                new Score(SRBCIndicator.Vine, 1),
                new Score(SRBCIndicator.Sunflower, 1),
                new Score(SRBCIndicator.Cactus, 0),
                new Score(SRBCIndicator.Bamboo, 0),
                new Score(SRBCIndicator.TwinLotus, 1),
                new Score(SRBCIndicator.Dandelion, -2),
                new Score(SRBCIndicator.Cedar, 1),
        });
        // 战场
        sWordMap.put(SRBCWord.Battlefield, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 0),
                new Score(SRBCIndicator.OakTree, 1),
                new Score(SRBCIndicator.Vine, -1),
                new Score(SRBCIndicator.Sunflower, -2),
                new Score(SRBCIndicator.Cactus, 2),
                new Score(SRBCIndicator.Bamboo, 0),
                new Score(SRBCIndicator.TwinLotus, -2),
                new Score(SRBCIndicator.Dandelion, 0),
                new Score(SRBCIndicator.Cedar, 1),
        });
        // 舞台
        sWordMap.put(SRBCWord.Stage, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 0),
                new Score(SRBCIndicator.OakTree, 0),
                new Score(SRBCIndicator.Vine, 0),
                new Score(SRBCIndicator.Sunflower, 2),
                new Score(SRBCIndicator.Cactus, -1),
                new Score(SRBCIndicator.Bamboo, 1),
                new Score(SRBCIndicator.TwinLotus, 1),
                new Score(SRBCIndicator.Dandelion, 1),
                new Score(SRBCIndicator.Cedar, -1),
        });
        // 驿站
        sWordMap.put(SRBCWord.Post, new Score[] {
                new Score(SRBCIndicator.BanyanTree, -2),
                new Score(SRBCIndicator.OakTree, -1),
                new Score(SRBCIndicator.Vine,  -1),
                new Score(SRBCIndicator.Sunflower, 0),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, 1),
                new Score(SRBCIndicator.TwinLotus, -2),
                new Score(SRBCIndicator.Dandelion, 2),
                new Score(SRBCIndicator.Cedar, -1),
        });
        // 枷锁
        sWordMap.put(SRBCWord.Chains, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 1),
                new Score(SRBCIndicator.OakTree, -2),
                new Score(SRBCIndicator.Vine,  2),
                new Score(SRBCIndicator.Sunflower, -1),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, -1),
                new Score(SRBCIndicator.TwinLotus, -1),
                new Score(SRBCIndicator.Dandelion, -2),
                new Score(SRBCIndicator.Cedar, -1),
        });
        // 翅膀
        sWordMap.put(SRBCWord.Wing, new Score[] {
                new Score(SRBCIndicator.BanyanTree, -1),
                new Score(SRBCIndicator.OakTree, 0),
                new Score(SRBCIndicator.Vine,  -1),
                new Score(SRBCIndicator.Sunflower, 1),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, 1),
                new Score(SRBCIndicator.TwinLotus, 0),
                new Score(SRBCIndicator.Dandelion, 2),
                new Score(SRBCIndicator.Cedar, 0),
        });
        // 灯火
        sWordMap.put(SRBCWord.Lights, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 1),
                new Score(SRBCIndicator.OakTree, 1),
                new Score(SRBCIndicator.Vine,  1),
                new Score(SRBCIndicator.Sunflower, 2),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, 1),
                new Score(SRBCIndicator.TwinLotus, 1),
                new Score(SRBCIndicator.Dandelion, 0),
                new Score(SRBCIndicator.Cedar, 1),
        });
        // 潮汐
        sWordMap.put(SRBCWord.Tide, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 1),
                new Score(SRBCIndicator.OakTree, 1),
                new Score(SRBCIndicator.Vine,  1),
                new Score(SRBCIndicator.Sunflower, 0),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, 0),
                new Score(SRBCIndicator.TwinLotus, 1),
                new Score(SRBCIndicator.Dandelion, 0),
                new Score(SRBCIndicator.Cedar, 1),
        });
        // 旋律
        sWordMap.put(SRBCWord.Melody, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 1),
                new Score(SRBCIndicator.OakTree, 1),
                new Score(SRBCIndicator.Vine,  0),
                new Score(SRBCIndicator.Sunflower, 1),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, 1),
                new Score(SRBCIndicator.TwinLotus, 2),
                new Score(SRBCIndicator.Dandelion, 0),
                new Score(SRBCIndicator.Cedar, 0),
        });
        // 根系
        sWordMap.put(SRBCWord.Root, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 2),
                new Score(SRBCIndicator.OakTree, 1),
                new Score(SRBCIndicator.Vine,  1),
                new Score(SRBCIndicator.Sunflower, 0),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, 1),
                new Score(SRBCIndicator.TwinLotus, 1),
                new Score(SRBCIndicator.Dandelion, -2),
                new Score(SRBCIndicator.Cedar, 1),
        });
        // 影子
        sWordMap.put(SRBCWord.Shadow, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 1),
                new Score(SRBCIndicator.OakTree, -1),
                new Score(SRBCIndicator.Vine,  2),
                new Score(SRBCIndicator.Sunflower, -1),
                new Score(SRBCIndicator.Cactus,  0),
                new Score(SRBCIndicator.Bamboo, -1),
                new Score(SRBCIndicator.TwinLotus, 1),
                new Score(SRBCIndicator.Dandelion, 0),
                new Score(SRBCIndicator.Cedar, -1),
        });
        // 迷宫
        sWordMap.put(SRBCWord.Maze, new Score[] {
                new Score(SRBCIndicator.BanyanTree, 2),
                new Score(SRBCIndicator.OakTree, -1),
                new Score(SRBCIndicator.Vine,  1),
                new Score(SRBCIndicator.Sunflower, -2),
                new Score(SRBCIndicator.Cactus,  1),
                new Score(SRBCIndicator.Bamboo, -1),
                new Score(SRBCIndicator.TwinLotus, -1),
                new Score(SRBCIndicator.Dandelion, 0),
                new Score(SRBCIndicator.Cedar, -1),
        });
    }

    private PaintingFeatureSet paintingFeatureSet;

    public SubconsciousRelationshipBetweenACoupleEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        List<EvaluationFeature> results = new ArrayList<>();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalTree(spaceLayout));
        results.add(this.evalOthers(spaceLayout));

        EvaluationReport report = new EvaluationReport(this.contactId, Theme.SubconsciousRelationshipBetweenACouple,
                this.painting.getAttribute(), Reference.Normal, new PaintingConfidence(this.painting), results);
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
    }

    /**
     * 对关键词对应的指标进行评估。
     *
     * @param words
     * @return
     */
    public List<EvaluationScore> evaluateWords(List<String> words) {
        List<EvaluationScore> result = this.buildEmptyScoreList();
        for (String word : words) {
            SRBCWord srbcWord = SRBCWord.parse(word);
            Score scoreList[] = sWordMap.get(srbcWord);
            for (EvaluationScore res : result) {
                for (Score score : scoreList) {
                    if (score.indicator == res.indicator) {
                        res.scoring(score);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 合并分数列表
     *
     * @param input
     * @return
     */
    public List<EvaluationScore> mergeScoreList(List<EvaluationScore> input) {
        List<EvaluationScore> result = this.buildEmptyScoreList();
        for (EvaluationScore base : input) {
            EvaluationScore target = null;
            for (EvaluationScore es : result) {
                if (es.indicator == base.indicator) {
                    target = es;
                    break;
                }
            }
            target.value += base.value;
            target.hit = base.hit;
        }
        return result;
    }

    /**
     * 计算指标分。
     *
     * @param paintingScores
     * @param wordScores
     * @return
     */
    public List<Score> caleIndicatorScores(List<EvaluationScore> paintingScores, List<EvaluationScore> wordScores) {
        if (paintingScores.size() != SRBCIndicator.values().length || wordScores.size() != SRBCIndicator.values().length) {
            Logger.w(this.getClass(), "#caleIndicatorScores - Data length error");
            return null;
        }

        // 归一化绘画分数
        double[] paintingScoreArray = new double[SRBCIndicator.values().length];
        for (int i = 0; i < paintingScoreArray.length; ++i) {
            EvaluationScore score = paintingScores.get(i);
            paintingScoreArray[i] = score.calcScore();
        }
        double[] paintingNormalizationScores = FloatUtils.normalization(paintingScoreArray, 0, 100);

        // 归一化词分数
        double[] wordScoreArray = new double[SRBCIndicator.values().length];
        for (int i = 0; i < wordScoreArray.length; ++i) {
            EvaluationScore score = wordScores.get(i);
            wordScoreArray[i] = score.calcScore();
        }
        double[] wordNormalizationScores = FloatUtils.normalization(wordScoreArray, 0, 100);

        // 总分数组
        double[] totalArray = new double[SRBCIndicator.values().length];
        for (int i = 0; i < totalArray.length; ++i) {
            totalArray[i] = paintingNormalizationScores[i] * 0.6 + wordNormalizationScores[i] * 0.4;
        }

        List<Score> scoreList = new ArrayList<>();
        for (int i = 0; i < totalArray.length; ++i) {
            scoreList.add(new Score(SRBCIndicator.values()[i], (int) Math.round(totalArray[i])));
        }
        return scoreList;
    }

    private List<EvaluationScore> buildEmptyScoreList() {
        List<EvaluationScore> result = new ArrayList<>();
        for (SRBCIndicator indicator : SRBCIndicator.values()) {
            result.add(new EvaluationScore(indicator, 0));
        }
        return result;
    }

    private EvaluationFeature evalTree(SpaceLayout spaceLayout) {
        EvaluationFeature feature = new EvaluationFeature();

        if (this.painting.hasTree()) {
            for (Tree tree : this.painting.getTrees()) {
                double trunkWidth = tree.getTrunkBoundingBox().width;

                // 树根
                if (tree.hasRoot()) {
                    KeyFeature keyFeature = new KeyFeature("画面中的树有树根", "");
                    feature.addKeyFeature(keyFeature);

                    // 计算树干相对根的宽度占比
                    Root root = tree.getRoots().get(0);
                    double radio = trunkWidth / root.getWidth();
                    if (radio > 0.3) {
                        keyFeature = new KeyFeature("画面中的树树干相对树根来说树干较粗", "");
                        feature.addKeyFeature(keyFeature);

                        feature.addScore(SRBCIndicator.BanyanTree, 3, 1);
                    }
                    else if (radio < 0.18) {
                        keyFeature = new KeyFeature("画面中的树树干相对树根来说树干较细", "");
                        feature.addKeyFeature(keyFeature);

                        feature.addScore(SRBCIndicator.BanyanTree, 3, 1);
                        feature.addScore(SRBCIndicator.Cedar, 2, 1);
                        feature.addScore(SRBCIndicator.Bamboo, 1, 1);
                    }
                    else {
                        keyFeature = new KeyFeature("画面中的树树干粗细相较树干来说粗细适中", "");
                        feature.addKeyFeature(keyFeature);

                        feature.addScore(SRBCIndicator.BanyanTree, 3, 1);
                        feature.addScore(SRBCIndicator.Vine, 2, 1);
                    }
                }
                else {
                    // 无树根
                    KeyFeature keyFeature = new KeyFeature("画面中出现的树没有画树根", "");
                    feature.addKeyFeature(keyFeature);
                    feature.addScore(SRBCIndicator.Dandelion, 3, 1);
                    feature.addScore(SRBCIndicator.BanyanTree, -2, 1);
                    feature.addScore(SRBCIndicator.OakTree, -2, 1);
                    feature.addScore(SRBCIndicator.Cedar, -2, 1);
                }

                // 树干
                double ratio = tree.getTrunkWidthRatio();
                if (ratio < 0.18) {
                    KeyFeature keyFeature = new KeyFeature("画面中出现的树树干较细", "");
                    feature.addKeyFeature(keyFeature);

                    feature.addScore(SRBCIndicator.Bamboo, 2, 1);
                    feature.addScore(SRBCIndicator.Vine, -1, 1);
                }
                else if (ratio > 0.3) {
                    KeyFeature keyFeature = new KeyFeature("画面中出现的树树干较粗", "");
                    feature.addKeyFeature(keyFeature);

                    feature.addScore(SRBCIndicator.OakTree, 2, 1);
                    feature.addScore(SRBCIndicator.BanyanTree, -1, 1);
                }
                else {
                    KeyFeature keyFeature = new KeyFeature("画面中出现的树树干粗细适中", "");
                    feature.addKeyFeature(keyFeature);

                    feature.addScore(SRBCIndicator.OakTree, 2, 1);
                    feature.addScore(SRBCIndicator.TwinLotus, 2, 1);
                    feature.addScore(SRBCIndicator.BanyanTree, -1, 1);
                }

                // 树洞
                if (tree.hasHole()) {
                    KeyFeature keyFeature = new KeyFeature("画面中出现的有树洞", "");
                    feature.addKeyFeature(keyFeature);

                    feature.addScore(SRBCIndicator.Cactus, 3, 1);
                    feature.addScore(SRBCIndicator.Cedar, 1, 1);
                    feature.addScore(SRBCIndicator.Sunflower, -2, 1);
                    feature.addScore(SRBCIndicator.TwinLotus, -1, 1);
                }
                else {
                    KeyFeature keyFeature = new KeyFeature("画面中出现的没有树洞", "");
                    feature.addKeyFeature(keyFeature);
                }

                // 树冠
                // TODO

                // 果实
                if (tree.hasFruit()) {
                    feature.addScore(SRBCIndicator.OakTree, 1, 1);
                    feature.addScore(SRBCIndicator.Bamboo, 1, 1);
                }
            }
        }

        return feature;
    }

    private EvaluationFeature evalOthers(SpaceLayout spaceLayout) {
        EvaluationFeature feature = new EvaluationFeature();

        if (this.painting.hasHouse()) {
            feature.addScore(SRBCIndicator.BanyanTree, 3, 1);
            feature.addScore(SRBCIndicator.OakTree, 2, 1);
            feature.addScore(SRBCIndicator.Dandelion, -2, 1);

            House house = this.painting.getHouse();
            if (house.hasPath()) {
                feature.addScore(SRBCIndicator.Bamboo, 2, 1);
                feature.addScore(SRBCIndicator.OakTree, 1, 1);
                feature.addScore(SRBCIndicator.BanyanTree, -1, 1);
            }
        }

        DrawingSet set = this.painting.getDrawingSet();

        if (set.has(Label.Sun)) {
            feature.addScore(SRBCIndicator.Sunflower, 3, 1);
            feature.addScore(SRBCIndicator.TwinLotus, 1, 1);
            feature.addScore(SRBCIndicator.Cactus, -1, 1);
            feature.addScore(SRBCIndicator.Cedar, -1, 1);
        }

        if (set.has(Label.Cloud)) {
            feature.addScore(SRBCIndicator.TwinLotus, 1, 1);
            feature.addScore(SRBCIndicator.Dandelion, 1, 1);
            feature.addScore(SRBCIndicator.Sunflower, -1, 1);
        }

        if (set.has(Label.Rainbow)) {
            feature.addScore(SRBCIndicator.TwinLotus, 2, 1);
            feature.addScore(SRBCIndicator.Sunflower, 2, 1);
            feature.addScore(SRBCIndicator.Cactus, -1, 1);
        }

        if (set.has(Label.Bird) || set.has(Label.Butterfly)) {
            feature.addScore(SRBCIndicator.Dandelion, 2, 1);
            feature.addScore(SRBCIndicator.Sunflower, 1, 1);
            feature.addScore(SRBCIndicator.Vine, -1, 1);
            feature.addScore(SRBCIndicator.BanyanTree, -1, 1);
        }

        if (set.has(Label.Rabbit)) {
            feature.addScore(SRBCIndicator.Sunflower, 2, 1);
            feature.addScore(SRBCIndicator.TwinLotus, 1, 1);
            feature.addScore(SRBCIndicator.Cactus, -1, 1);
        }

        return feature;
    }
}
