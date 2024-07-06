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

package cube.service.aigc.scene;

import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.algorithm.BigFiveFeature;

import java.util.List;

public class PersonalityEvaluation {

    private BigFiveFeature feature;

    public PersonalityEvaluation(List<EvaluationFeature> featureList) {

    }

    /*
    private void build(List<EvaluationScore> scoreList) {
        double obligingness = 0;
        double conscientiousness = 0;
        double extraversion = 0;
        double achievement = 0;
        double neuroticism = 0;

        double obligingnessValue = 0;
        double extraversionValue = 0;

        for (EvaluationScore score : scoreList) {
            switch (score.indicator) {
                case Extroversion:
                    extraversionValue += 7;
                    break;
                case Introversion:
                    extraversionValue -= 7;
                    break;
                case Optimism:
                    break;
                case Pessimism:
                    break;
                case Narcissism:
                    obligingnessValue -= 10 * score.calcScore();
                    break;
                case Confidence:
                    obligingnessValue += 2 * score.calcScore();
                    break;
                case SelfEsteem:
                    obligingnessValue += 2 * score.calcScore();
                    break;
                case SocialAdaptability:
                    obligingnessValue += 5 * score.calcScore();
                    break;
                case Independence:
                    obligingnessValue -= 1 * score.calcScore();
                    break;
                case Idealism:
                    break;
                case Realism:
                    break;
                case Emotion:
                    break;
                case SelfConsciousness:
                    break;
                case Thought:
                    break;
                case SenseOfSecurity:
                    break;
                case Obsession:
                    break;
                case Constrain:
                    break;
                case SelfControl:
                    break;
                case Anxiety:
                    break;
                case Depression:
                    break;
                case Simple:
                    break;
                case Meekness:
                    break;
                case Hostile:
                    obligingnessValue -= 10 * score.calcScore();
                    break;
                case Attacking:
                    obligingnessValue -= 10 * score.calcScore();
                    break;
                case Family:
                    obligingnessValue += 3 * score.calcScore();
                    break;
                case InterpersonalRelation:
                    obligingnessValue += 2 * score.calcScore();
                    break;
                case EvaluationFromOutside:
                    break;
                case Paranoid:
                    break;
                case AchievementMotivation:
                    break;
                case Stress:
                    break;
                case Creativity:
                    break;
                case Impulsion:
                    break;
                case Struggle:
                    break;
                case MoralSense:
                    break;
                case DesireForFreedom:
                    break;
                default:
                    break;
            }
        }
    }*/
}
