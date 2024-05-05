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

import java.util.ArrayList;
import java.util.List;

/**
 * 评估的特征。
 */
public class EvaluationFeature {

    private List<Feature> features;

    private List<Score> scores;

    public EvaluationFeature() {
        this.features = new ArrayList<>();
        this.scores = new ArrayList<>();
    }

    public void addFeature(Comment comment, Tendency tendency) {
        this.features.add(new Feature(comment, tendency));
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
        for (Score score : this.scores) {
            if (score.indicator == indicator) {
                return score;
            }
        }
        return null;
    }

    public class Feature {

        public Comment comment;

        public Tendency tendency;

        public Feature(Comment comment, Tendency tendency) {
            this.comment = comment;
            this.tendency = tendency;
        }
    }
}
