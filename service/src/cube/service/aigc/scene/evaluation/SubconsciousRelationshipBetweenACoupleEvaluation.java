/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.evaluation;

import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.Tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubconsciousRelationshipBetweenACoupleEvaluation extends Evaluation {

    /**
     * 类型。
     */
    public enum SRBCType {
        /**
         * 榕树型。
         */
        BanyanTree,

        /**
         * 橡树型。
         */
        OakTree,

        /**
         * 藤蔓型。
         */
        Vine,

        /**
         * 向日葵型。
         */
        Sunflower,

        /**
         * 仙人掌型。
         */
        Cactus,

        /**
         * 竹子型。
         */
        Bamboo,

        /**
         * 并蒂莲型。
         */
        TwinLotus,

        /**
         * 蒲公英型。
         */
        Dandelion,

        /**
         * 雪松型。
         */
        Cedar,
    }

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

    public enum SRBCWord {

        SRBCWord(1, "港湾"),

        ;

        public final int sn;

        public final String word;

        SRBCWord(int sn, String word) {
            this.sn = sn;
            this.word = word;
        }
    }

    private final static HashMap<Integer, EvaluationScore[]> sWordMap = new HashMap<>();

    static {
        sWordMap.put(1, new EvaluationScore[] {
                new EvaluationScore(SRBCIndicator.BanyanTree, 2),
                new EvaluationScore(SRBCIndicator.OakTree, 2),
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

        EvaluationReport report = new EvaluationReport(this.contactId, Theme.SubconsciousRelationshipBetweenACouple,
                this.painting.getAttribute(), Reference.Normal, new PaintingConfidence(this.painting), results);
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
    }

    public List<EvaluationScore> evaluateWords(String word) {
        return null;
    }

    public List<EvaluationScore> buildEmptyScoreList() {
        List<EvaluationScore> result = new ArrayList<>();
        return result;
    }

    private EvaluationFeature evalTree(SpaceLayout spaceLayout) {
        EvaluationFeature feature = new EvaluationFeature();

        if (this.painting.hasTree()) {
            for (Tree tree : this.painting.getTrees()) {
                KeyFeature keyFeature = new KeyFeature("", "");
                feature.addKeyFeature(keyFeature);
                feature.addScore(SRBCIndicator.BanyanTree, 3, 1);
            }
        }

        return feature;
    }
}
