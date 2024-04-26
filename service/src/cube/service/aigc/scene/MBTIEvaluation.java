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

import cell.util.log.Logger;
import cube.aigc.psychology.algorithm.MBTIFeature;
import cube.aigc.psychology.algorithm.MyersBriggsTypeIndicator;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.EvaluationScore;

import java.util.ArrayList;
import java.util.List;

public class MBTIEvaluation {

    private MBTIFeature result;

    public MBTIEvaluation(List<Representation> representationList, List<EvaluationScore> scoreList) {
        this.result = this.build(representationList, scoreList);
    }

    public MBTIFeature getResult() {
        return this.result;
    }

    private MBTIFeature build(List<Representation> representationList, List<EvaluationScore> scoreList) {
        List<MBTICandidate> list = new ArrayList<>();

        for (Representation representation : representationList) {
            switch (representation.knowledgeStrategy.getComment()) {
                case SelfExistence:
                    break;
                case SelfEsteem:
                    break;
                case SelfConfidence:
                    break;
                case SelfControl:
                    break;
                case Powerlessness:
                    break;
                case Narcissism:
                    break;
                case SocialAdaptability:
                    break;
                case EnvironmentalAlienation:
                    break;
                case EnvironmentalDependence:
                    break;
                case EnvironmentalFriendliness:
                    break;
                case Defensiveness:
                    break;
                case Introversion:
                    break;
                case Extroversion:
                    break;
                case Simple:
                    break;
                case SimpleIdea:
                    break;
                case Idealization:
                    break;
                case Instinct:
                    break;
                case Nostalgia:
                    break;
                case Future:
                    break;
                case Luxurious:
                    break;
                case Fantasy:
                    break;
                case Childish:
                    break;
                case Extreme:
                    break;
                case WorldWeariness:
                    break;
                case Perfectionism:
                    break;
                case HighPressure:
                    break;
                case ExternalPressure:
                    break;
                case Escapism:
                    break;
                case Maverick:
                    break;
                case PursueInterpersonalRelationships:
                    break;
                case EmotionalIndifference:
                    break;
                case EmotionalStability:
                    break;
                case EmotionalDisturbance:
                    break;
                case Dependence:
                    break;
                case SocialPowerlessness:
                    break;
                case SocialDemand:
                    break;
                case SelfDemand:
                    break;
                case SelfInflated:
                    break;
                case Sensitiveness:
                    break;
                case Emotionality:
                    break;
                case Suspiciousness:
                    break;
                case Straightforwardness:
                    break;
                case Vigilance:
                    break;
                case Alertness:
                    break;
                case Depression:
                    break;
                case Creativity:
                    break;
                case Independence:
                    break;
                case Trauma:
                    break;
                case HighEnergy:
                    break;
                case SenseOfSecurity:
                    break;
                case PursuitOfAchievement:
                    break;
                case ManyGoals:
                    break;
                case ManyDesires:
                    break;
                case IntrapsychicConflict:
                    break;
                case Hostility:
                    break;
                case AttentionToDetail:
                    break;
                case LongingForMaternalLove:
                    break;
                case Strong:
                    break;
                case Stubborn:
                    break;
                case DesireForControl:
                    break;
                case Sentimentality:
                    break;
                case Aggression:
                    break;
                case Cautious:
                    break;
                case PositiveExpectation:
                    break;
                case Imagination:
                    break;
                case DelicateEmotions:
                    break;
                case DesireForFreedom:
                    break;
                case Vanity:
                    break;
                case NeedProtection:
                    break;
                case Stereotype:
                    break;
                case PayAttentionToFamily:
                    break;
                default:
                    break;
            }
        }

        int seed = 0;
        for (EvaluationScore score : scoreList) {
            seed += score.value;
            switch (score.indicator) {
                case Extroversion:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 1, true));
                    break;
                case Introversion:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 1, true));
                    break;
                case Optimism:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 0.1));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Feeling, 0.1));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Perceiving, 0.1));
                    break;
                case Pessimism:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.1));
                    break;
                case Narcissism:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Intuition, 0.1));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Thinking, 0.1));
                    break;
                case Confidence:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 0.5));
                    break;
                case SelfEsteem:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Feeling, 0.1));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Perceiving, 0.1));
                    break;
                case SocialAdaptability:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 0.5));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Intuition, 0.5));
                    }
                    else {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.5));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Sensing, 0.5));
                    }
                    break;
                case Independence:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Thinking, 0.1));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.1));
                    break;
                case Idealism:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Sensing, 0.5));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Feeling, 0.5));
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Perceiving, 0.5));
                    }
                    break;
                case Emotion:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 0.5));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Feeling, 0.5));
                    }
                    else {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.5));
                    }
                    break;
                case SelfConsciousness:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.5));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Thinking, 0.4));
                    }
                    break;
                case Realism:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Intuition, 0.6));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Thinking, 0.5));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.5));
                    break;
                case Thought:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Sensing, 0.6));
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Thinking, 1, true));
                    }
                    break;
                case SenseOfSecurity:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 0.1));
                    }
                    else {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.1));
                    }
                    break;
                case Obsession:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.8));
                    }
                    break;
                case Constrain:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.1));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Sensing, 0.1));
                    break;
                case SelfControl:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.5));
                    break;
                case Anxiety:
                    break;
                case Depression:
                    break;
                case Simple:
                    break;
                case Meekness:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Perceiving, 0.5));
                    break;
                case Hostile:
                    break;
                case Attacking:
                    break;
                case Family:
                    break;
                case InterpersonalRelation:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Extraversion, 0.3));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Intuition, 0.5));
                    }
                    else {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Introversion, 0.3));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Sensing, 0.5));
                    }
                    break;
                case EvaluationFromOutside:
                    break;
                case Paranoid:
                    break;
                case AchievementMotivation:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.4));
                    break;
                case Stress:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.5));
                    break;
                case Creativity:
                    if (score.positiveScore > score.negativeScore) {
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Intuition, 0.5));
                        list.add(new MBTICandidate(MyersBriggsTypeIndicator.Perceiving, 0.5));
                    }
                    break;
                case Impulsion:
                    break;
                case Struggle:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Judging, 0.5));
                    break;
                case MoralSense:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Feeling, 0.6));
                    break;
                case DesireForFreedom:
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Intuition, 0.4));
                    list.add(new MBTICandidate(MyersBriggsTypeIndicator.Perceiving, 0.5));
                    break;
                default:
                    break;
            }
        }

        List<MyersBriggsTypeIndicator> mbtiList = this.analyse(list);
        mbtiList = this.verify(mbtiList, seed);
        return new MBTIFeature(mbtiList);
    }

    private List<MyersBriggsTypeIndicator> analyse(List<MBTICandidate> candidates) {
        List<MyersBriggsTypeIndicator> list = new ArrayList<>();
        // 先判断 reject
        for (MBTICandidate candidate : candidates) {
            if (candidate.rejectOther) {
                if (!list.contains(candidate.indicator)) {
                    list.add(candidate.indicator);
                }
            }
        }

        // E/I
        if ((!list.contains(MyersBriggsTypeIndicator.Extraversion) && !list.contains(MyersBriggsTypeIndicator.Introversion))
            || (list.contains(MyersBriggsTypeIndicator.Extraversion) && list.contains(MyersBriggsTypeIndicator.Introversion))) {
            double extraversion = 0;
            double introversion = 0;
            for (MBTICandidate candidate : candidates) {
                if (candidate.indicator == MyersBriggsTypeIndicator.Extraversion) {
                    extraversion += candidate.weight;
                }
                else if (candidate.indicator == MyersBriggsTypeIndicator.Introversion) {
                    introversion += candidate.weight;
                }
            }
            list.remove(MyersBriggsTypeIndicator.Extraversion);
            list.remove(MyersBriggsTypeIndicator.Introversion);
            if (extraversion > introversion) {
                list.add(MyersBriggsTypeIndicator.Extraversion);
            }
            else {
                list.add(MyersBriggsTypeIndicator.Introversion);
            }
        }

        // S/N
        if ((!list.contains(MyersBriggsTypeIndicator.Sensing) && !list.contains(MyersBriggsTypeIndicator.Intuition))
            || (list.contains(MyersBriggsTypeIndicator.Sensing) && list.contains(MyersBriggsTypeIndicator.Intuition))) {
            double sensing = 0;
            double intuition = 0;
            for (MBTICandidate candidate : candidates) {
                if (candidate.indicator == MyersBriggsTypeIndicator.Sensing) {
                    sensing += candidate.weight;
                }
                else if (candidate.indicator == MyersBriggsTypeIndicator.Intuition) {
                    intuition += candidate.weight;
                }
            }
            list.remove(MyersBriggsTypeIndicator.Sensing);
            list.remove(MyersBriggsTypeIndicator.Intuition);
            if (intuition > sensing) {
                list.add(MyersBriggsTypeIndicator.Intuition);
            }
            else {
                list.add(MyersBriggsTypeIndicator.Sensing);
            }
        }

        // F/T
        if ((!list.contains(MyersBriggsTypeIndicator.Feeling) && !list.contains(MyersBriggsTypeIndicator.Thinking))
                || (list.contains(MyersBriggsTypeIndicator.Feeling) && list.contains(MyersBriggsTypeIndicator.Thinking))) {
            double feeling = 0;
            double thinking = 0;
            for (MBTICandidate candidate : candidates) {
                if (candidate.indicator == MyersBriggsTypeIndicator.Feeling) {
                    feeling += candidate.weight;
                }
                else if (candidate.indicator == MyersBriggsTypeIndicator.Thinking) {
                    thinking += candidate.weight;
                }
            }
            list.remove(MyersBriggsTypeIndicator.Feeling);
            list.remove(MyersBriggsTypeIndicator.Thinking);
            if (feeling > thinking) {
                list.add(MyersBriggsTypeIndicator.Feeling);
            }
            else {
                list.add(MyersBriggsTypeIndicator.Thinking);
            }
        }

        // J/P
        if ((!list.contains(MyersBriggsTypeIndicator.Judging) && !list.contains(MyersBriggsTypeIndicator.Perceiving))
                || (list.contains(MyersBriggsTypeIndicator.Judging) && list.contains(MyersBriggsTypeIndicator.Perceiving))) {
            double judging = 0;
            double perceiving = 0;
            for (MBTICandidate candidate : candidates) {
                if (candidate.indicator == MyersBriggsTypeIndicator.Judging) {
                    judging += candidate.weight;
                }
                else if (candidate.indicator == MyersBriggsTypeIndicator.Perceiving) {
                    perceiving += candidate.weight;
                }
            }
            list.remove(MyersBriggsTypeIndicator.Judging);
            list.remove(MyersBriggsTypeIndicator.Perceiving);
            if (perceiving > judging) {
                list.add(MyersBriggsTypeIndicator.Perceiving);
            }
            else {
                list.add(MyersBriggsTypeIndicator.Judging);
            }
        }

        return list;
    }

    private List<MyersBriggsTypeIndicator> verify(List<MyersBriggsTypeIndicator> mbtiList, int seed) {
        // 检测缺失
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Introversion) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Extraversion)) {
            Logger.w(this.getClass(), "#verify - E/I lose");
            mbtiList.add(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Introversion : MyersBriggsTypeIndicator.Extraversion);
        }
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Sensing) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Intuition)) {
            Logger.w(this.getClass(), "#verify - S/N lose");
            mbtiList.add(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Sensing : MyersBriggsTypeIndicator.Intuition);
        }
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Feeling) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Thinking)) {
            Logger.w(this.getClass(), "#verify - F/T lose");
            mbtiList.add(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Feeling : MyersBriggsTypeIndicator.Thinking);
        }
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Judging) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Perceiving)) {
            Logger.w(this.getClass(), "#verify - J/P lose");
            mbtiList.add(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Judging : MyersBriggsTypeIndicator.Perceiving);
        }

        // 检测冲突
        if (mbtiList.contains(MyersBriggsTypeIndicator.Introversion) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Extraversion)) {
            Logger.w(this.getClass(), "#verify - E/I breakdown");
            mbtiList.remove(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Introversion : MyersBriggsTypeIndicator.Extraversion);
        }
        if (mbtiList.contains(MyersBriggsTypeIndicator.Sensing) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Intuition)) {
            Logger.w(this.getClass(), "#verify - S/N breakdown");
            mbtiList.remove(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Sensing : MyersBriggsTypeIndicator.Intuition);
        }
        if (mbtiList.contains(MyersBriggsTypeIndicator.Feeling) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Thinking)) {
            Logger.w(this.getClass(), "#verify - F/T breakdown");
            mbtiList.remove(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Feeling : MyersBriggsTypeIndicator.Thinking);
        }
        if (mbtiList.contains(MyersBriggsTypeIndicator.Judging) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Perceiving)) {
            Logger.w(this.getClass(), "#verify - J/P breakdown");
            mbtiList.remove(seed % 2 == 0 ?
                    MyersBriggsTypeIndicator.Judging : MyersBriggsTypeIndicator.Perceiving);
        }

        return mbtiList;
    }

    protected class MBTICandidate {

        protected final MyersBriggsTypeIndicator indicator;

        protected final double weight;

        protected final boolean rejectOther;

        public MBTICandidate(MyersBriggsTypeIndicator indicator, double weight) {
            this(indicator, weight, false);
        }

        public MBTICandidate(MyersBriggsTypeIndicator indicator, double weight, boolean rejectOther) {
            this.indicator = indicator;
            this.weight = weight;
            this.rejectOther = rejectOther;
        }
    }
}
