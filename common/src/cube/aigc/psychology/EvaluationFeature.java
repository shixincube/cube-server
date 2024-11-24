/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.aigc.psychology;

import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.BigFiveFactor;

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

    public void addFeature(Term term, Tendency tendency) {
        this.features.add(new Feature(term, tendency));
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

        public Term term;

        public Tendency tendency;

        public Feature(Term term, Tendency tendency) {
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
