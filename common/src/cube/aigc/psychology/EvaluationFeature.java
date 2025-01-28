/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.BigFiveFactor;
import cube.common.entity.Material;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 评估的特征。
 */
public class EvaluationFeature {

    private List<Feature> features;

    private List<Score> scores;

    private List<FiveFactor> fiveFactors;

    public EvaluationFeature() {
        this.features = new ArrayList<>();
        this.scores = new ArrayList<>();
        this.fiveFactors = new ArrayList<>();
    }

    public void addFeature(String description, Term term, Tendency tendency) {
        this.features.add(new Feature(description, term, tendency));
    }

    public void addFeature(String description, Term term, Tendency tendency, Material[] materials) {

    }

    public List<Feature> getFeatures() {
        return this.features;
    }

    public Score addScore(Indicator indicator, int value, double weight) {
        Score score = new Score(indicator, value, weight);
        this.scores.add(score);
        return score;
    }

    public List<Score> getScores() {
        return this.scores;
    }

    public Score getScore(Indicator indicator) {
        Score result = new Score(indicator, 0, 0);
        for (Score score : this.scores) {
            if (score.indicator == indicator) {
                result.value += score.value;
                result.weight += score.value * score.weight;
            }
        }

        if (result.value == 0) {
            return null;
        }

        return result;
    }

    public List<Score> getScores(Indicator indicator) {
        List<Score> result = new ArrayList<>();
        for (Score score : this.scores) {
            if (score.indicator == indicator) {
                result.add(score);
            }
        }
        return result;
    }

    public void removeScores(Indicator indicator) {
        Iterator<Score> iter = this.scores.iterator();
        while (iter.hasNext()) {
            Score score = iter.next();
            if (score.indicator == indicator) {
                iter.remove();
            }
        }
    }

    public void removeScore(Score score) {
        this.scores.remove(score);
    }

    public void addFiveFactor(BigFiveFactor bigFiveFactor, double weight) {
        FiveFactor fiveFactor = new FiveFactor(bigFiveFactor, weight);
        this.fiveFactors.add(fiveFactor);
    }

    public List<FiveFactor> getFiveFactors() {
        return this.fiveFactors;
    }

    public class Feature {

        public String description;

        public Term term;

        public Tendency tendency;

        public Material[] sources;

        public Feature(String description, Term term, Tendency tendency) {
            this.description = description;
            this.term = term;
            this.tendency = tendency;
        }
    }

    public class FiveFactor {

        public BigFiveFactor bigFiveFactor;

        public double source;

        public FiveFactor(BigFiveFactor bigFiveFactor, double source) {
            this.bigFiveFactor = bigFiveFactor;
            this.source = source;
        }
    }
}
