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

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.composition.TheBigFive;
import cube.common.JSONable;
import cube.util.FloatUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 人格分析催化剂。
 */
public class PersonalityAccelerator implements JSONable  {

    private BigFiveFeature[] bigFiveFeatures = new BigFiveFeature[] {
            BigFiveFeature.Architect,
            BigFiveFeature.Expert,
            BigFiveFeature.Guide,
            BigFiveFeature.Generalist,
            BigFiveFeature.Idealist,
            BigFiveFeature.Supporter,
            BigFiveFeature.Developer,
            BigFiveFeature.Advocate,
            BigFiveFeature.Realist,
            BigFiveFeature.Instructor,
            BigFiveFeature.Promoter,
            BigFiveFeature.Explorer,
            BigFiveFeature.Traditionalist,
            BigFiveFeature.Adapter,
            BigFiveFeature.Entrepreneur,
            BigFiveFeature.Demonstrator,
            BigFiveFeature.Controller
    };
    private MBTIFeature[] mbtiFeatures = new MBTIFeature[] {
            MBTIFeature.INTJ,
            MBTIFeature.INTP,
            MBTIFeature.ENTJ,
            MBTIFeature.ENTP,
            MBTIFeature.INFJ,
            MBTIFeature.INFP,
            MBTIFeature.ENFJ,
            MBTIFeature.ENFP,
            MBTIFeature.ISTJ,
            MBTIFeature.ISFJ,
            MBTIFeature.ESTJ,
            MBTIFeature.ESFJ,
            MBTIFeature.ISTP,
            MBTIFeature.ISFP,
            MBTIFeature.ESTP,
            MBTIFeature.ESFP,
            MBTIFeature.ISFJ
    };

    private List<FiveFactor> fiveFactorList;

    private BigFiveFeature feature;

    public PersonalityAccelerator(JSONObject json) {

    }

    public PersonalityAccelerator(List<EvaluationFeature> evaluationFeatureList) {
        this.build(evaluationFeatureList);
    }

    private void build(List<EvaluationFeature> evaluationFeatureList) {
        this.fiveFactorList = new ArrayList<>();

        FiveFactor obligingness = new FiveFactor(TheBigFive.Obligingness);
        FiveFactor conscientiousness = new FiveFactor(TheBigFive.Conscientiousness);
        FiveFactor extraversion = new FiveFactor(TheBigFive.Extraversion);
        FiveFactor achievement = new FiveFactor(TheBigFive.Achievement);
        FiveFactor neuroticism = new FiveFactor(TheBigFive.Neuroticism);

        for (EvaluationFeature ef : evaluationFeatureList) {
            List<EvaluationFeature.FiveFactor> list = ef.getFiveFactors();
            for (EvaluationFeature.FiveFactor factor : list) {
                switch (factor.theBigFive) {
                    case Obligingness:
                        obligingness.score += factor.source;
                        obligingness.total += 1;
                        break;
                    case Conscientiousness:
                        conscientiousness.score += factor.source;
                        conscientiousness.total += 1;
                        break;
                    case Extraversion:
                        extraversion.score += factor.source;
                        extraversion.total += 1;
                        break;
                    case Achievement:
                        achievement.score += factor.source;
                        achievement.total += 1;
                        break;
                    case Neuroticism:
                        neuroticism.score += factor.source;
                        neuroticism.total += 1;
                        break;
                    default:
                        break;
                }
            }
        }

        if (obligingness.total == 0) {
            obligingness.score = FloatUtils.random(5.0, 5.5);
            obligingness.total = 1;
        }
        if (conscientiousness.total == 0) {
            conscientiousness.score = FloatUtils.random(5.0, 5.5);
            conscientiousness.total = 1;
        }
        if (extraversion.total == 0) {
            extraversion.score = FloatUtils.random(5.0, 5.5);
            extraversion.total = 1;
        }
        if (achievement.total == 0) {
            achievement.score = FloatUtils.random(5.0, 5.5);
            achievement.total = 1;
        }
        if (neuroticism.total == 0) {
            neuroticism.score = FloatUtils.random(5.0, 5.5);
            neuroticism.total = 1;
        }

        this.fiveFactorList.add(obligingness);
        this.fiveFactorList.add(conscientiousness);
        this.fiveFactorList.add(extraversion);
        this.fiveFactorList.add(achievement);
        this.fiveFactorList.add(neuroticism);

        double obligingnessScore = obligingness.score / obligingness.total;
        double conscientiousnessScore = conscientiousness.score / conscientiousness.total;
        double extraversionScore = extraversion.score / extraversion.total;
        double achievementScore = achievement.score / achievement.total;
        double neuroticismScore = neuroticism.score / neuroticism.total;

        this.feature = new BigFiveFeature(obligingnessScore, conscientiousnessScore, extraversionScore,
                achievementScore, neuroticismScore);
    }

    public BigFiveFeature getBigFiveFeature() {
        return this.feature;
    }

    public MBTIFeature getMBTIFeature() {
        for (int i = 0; i < this.bigFiveFeatures.length; ++i) {
            BigFiveFeature bigFiveFeature = this.bigFiveFeatures[i];
            if (this.feature.getName().equals(bigFiveFeature.getName())) {
                return this.mbtiFeatures[i];
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public class FiveFactor {

        public TheBigFive theBigFive;

        public double score = 0;

        public double total = 0;

        public FiveFactor(TheBigFive theBigFive) {
            this.theBigFive = theBigFive;
        }

        public FiveFactor(JSONObject json) {

        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bigFive", this.theBigFive.code);
            json.put("score", this.score);
            json.put("total", this.total);
            return json;
        }
    }
}
