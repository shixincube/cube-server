/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene.evaluation;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.indicator.SRBCIndicator;
import cube.aigc.psychology.material.House;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Tree;
import cube.aigc.psychology.material.other.DrawingSet;
import cube.aigc.psychology.material.tree.Root;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.util.FloatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubconsciousRelationshipBetweenACoupleEvaluation extends Evaluation {
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

    public AIGCService service;

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
            target.positiveScore += base.positiveScore;
            target.negativeScore += base.negativeScore;
            target.hit = base.hit;
            target.positive = base.positive;
            target.negative = base.negative;
            target.positiveWeight = base.positiveWeight;
            target.negativeWeight = base.negativeWeight;
        }

        boolean noZero = false;
        for (EvaluationScore es : result) {
            if (es.calcScore() != 0.0) {
                noZero = true;
                break;
            }
        }

        if (!noZero) {
            for (EvaluationScore es : result) {
                if (es.indicator == SRBCIndicator.Bamboo) {
                    es.value = 2;
                    es.hit = 1;
                    es.positiveScore = 2;
                    es.negativeScore = 0;
                    es.positive = 2;
                    es.negative = 0;
                    es.positiveWeight = 1;
                    es.negativeWeight = 1;
                    break;
                }
            }
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
        if (paintingScores.size() != SRBCIndicator.length() || wordScores.size() != SRBCIndicator.length()) {
            Logger.w(this.getClass(), "#caleIndicatorScores - Data length error");
            return null;
        }

        // 归一化绘画分数
        double[] paintingScoreArray = new double[SRBCIndicator.length()];
        for (int i = 0; i < paintingScoreArray.length; ++i) {
            EvaluationScore score = paintingScores.get(i);
            paintingScoreArray[i] = score.calcScore();
        }
        double[] paintingNormalizationScores = FloatUtils.normalization(paintingScoreArray, 0, 100);
        if (null == paintingNormalizationScores) {
            Logger.w(this.getClass(), "#caleIndicatorScores - The painting normalization scores is null");
            return null;
        }

        // 归一化词分数
        double[] wordScoreArray = new double[SRBCIndicator.length()];
        for (int i = 0; i < wordScoreArray.length; ++i) {
            EvaluationScore score = wordScores.get(i);
            wordScoreArray[i] = score.calcScore();
        }
        double[] wordNormalizationScores = FloatUtils.normalization(wordScoreArray, 0, 100);
        if (null == wordNormalizationScores) {
            Logger.w(this.getClass(), "#caleIndicatorScores - The word normalization scores is null");
            return null;
        }

        // 总分数组
        double[] totalArray = new double[SRBCIndicator.length()];
        for (int i = 0; i < totalArray.length; ++i) {
            totalArray[i] = paintingNormalizationScores[i] * 0.6 + wordNormalizationScores[i] * 0.4;
        }

        List<Score> scoreList = new ArrayList<>();
        for (int i = 0; i < totalArray.length; ++i) {
            scoreList.add(new Score(SRBCIndicator.values()[i], (int) Math.round(totalArray[i])));
        }
        return scoreList;
    }

    public ComprehensiveSection generateComprehensiveSection(Score score) {
        SRBCIndicator indicator = (SRBCIndicator) score.indicator;
        String query = indicator.getName() + "爱情关系特点";
        List<String> words = this.service.segmentText(query);
        // 从数据集查找
        List<String> content = Resource.getInstance().loadDataset().searchContentInOrder(
                words.toArray(new String[0]), 4);
        if (content.isEmpty()) {
            Logger.w(this.getClass(), "#generateComprehensiveSection - Can NOT find \"" + indicator.getName()
                    + "\" in dataset - " + query);
            return null;
        }

        ComprehensiveSection section = null;

        // 润色
        GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                String.format(Prompts.getPrompt("FORMAT_POLISH"), content.get(0)),
                null, null, null);
        if (null == result) {
            Logger.d(this.getClass(), "#generateComprehensiveSection - Generating record failed");
            section = new ComprehensiveSection(indicator.getName(), content.get(0), indicator);
        }
        else {
            section = new ComprehensiveSection(indicator.getName(), result.answer, indicator);
        }

        return section;
    }

    private List<EvaluationScore> buildEmptyScoreList() {
        List<EvaluationScore> result = new ArrayList<>();
        for (SRBCIndicator indicator : SRBCIndicator.values()) {
            if (indicator == SRBCIndicator.Unknown) {
                continue;
            }

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
                double canopyAreaRatio = tree.getCanopyAreaRatio();
                if (canopyAreaRatio >= 0.45) {
                    // 树冠较大
                    feature.addScore(SRBCIndicator.BanyanTree, 2, 1);
                    feature.addScore(SRBCIndicator.Sunflower, 1, 1);
                    feature.addScore(SRBCIndicator.Bamboo, -1, 1);
                } else if (canopyAreaRatio <= 0.2) {
                    // 树冠较小
                    feature.addScore(SRBCIndicator.Dandelion, 2, 1);
                    feature.addScore(SRBCIndicator.Sunflower, -1, 1);
                    feature.addScore(SRBCIndicator.TwinLotus, -1, 1);
                } else {
                    feature.addScore(SRBCIndicator.OakTree, 2, 1);
                    feature.addScore(SRBCIndicator.Bamboo, 1, 1);
                    feature.addScore(SRBCIndicator.Vine, -1, 1);
                }

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
