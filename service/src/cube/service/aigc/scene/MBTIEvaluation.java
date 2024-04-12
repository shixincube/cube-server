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

import cube.aigc.psychology.algorithm.MBTIFeature;
import cube.aigc.psychology.algorithm.MyersBriggsTypeIndicator;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.algorithm.ScoreGroup;
import cube.aigc.psychology.composition.EvaluationScore;

import java.util.ArrayList;
import java.util.List;

public class MBTIEvaluation {

    private MBTIFeature result;

    public MBTIEvaluation(List<Representation> representationList, ScoreGroup scoreGroup) {
        this.result = this.build(representationList, scoreGroup);
    }

    public MBTIFeature getResult() {
        return this.result;
    }

    private MBTIFeature build(List<Representation> representationList, ScoreGroup scoreGroup) {
        List<MyersBriggsTypeIndicator> list = new ArrayList<>();

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

        for (EvaluationScore score : scoreGroup.getEvaluationScores()) {
            switch (score.indicator) {
                case Extroversion:
                    list.add(MyersBriggsTypeIndicator.Extraversion);
                    break;
                case Introversion:
                    list.add(MyersBriggsTypeIndicator.Introversion);
                    break;
                case Optimism:
                    break;
                case Pessimism:
                    break;
                case Narcissism:
                    break;
                case Confidence:
                    break;
                case SelfEsteem:
                    break;
                case SocialAdaptability:
                    break;
                case Independence:
                    break;
                case Idealism:
                    break;
                case Emotion:
                    break;
                case SelfConsciousness:
                    break;
                case Realism:
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
                    break;
                case Attacking:
                    break;
                case Family:
                    break;
                case InterpersonalRelation:
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

        List<MyersBriggsTypeIndicator> mbtiList = new ArrayList<>();
        for (MyersBriggsTypeIndicator mbti : list) {
            if (mbtiList.contains(mbti)) {
                continue;
            }
            mbtiList.add(mbti);
        }

        // 检测缺失
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Introversion) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Extraversion)) {
            mbtiList.add((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Introversion : MyersBriggsTypeIndicator.Extraversion);
        }
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Sensing) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Intuition)) {
            mbtiList.add((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Sensing : MyersBriggsTypeIndicator.Intuition);
        }
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Feeling) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Thinking)) {
            mbtiList.add((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Feeling : MyersBriggsTypeIndicator.Thinking);
        }
        if (!mbtiList.contains(MyersBriggsTypeIndicator.Judging) &&
                !mbtiList.contains(MyersBriggsTypeIndicator.Perceiving)) {
            mbtiList.add((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Judging : MyersBriggsTypeIndicator.Perceiving);
        }

        // 检测冲突
        if (mbtiList.contains(MyersBriggsTypeIndicator.Introversion) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Extraversion)) {
            mbtiList.remove((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Introversion : MyersBriggsTypeIndicator.Extraversion);
        }
        if (mbtiList.contains(MyersBriggsTypeIndicator.Sensing) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Intuition)) {
            mbtiList.remove((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Sensing : MyersBriggsTypeIndicator.Intuition);
        }
        if (mbtiList.contains(MyersBriggsTypeIndicator.Feeling) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Thinking)) {
            mbtiList.remove((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Feeling : MyersBriggsTypeIndicator.Thinking);
        }
        if (mbtiList.contains(MyersBriggsTypeIndicator.Judging) &&
                mbtiList.contains(MyersBriggsTypeIndicator.Perceiving)) {
            mbtiList.remove((representationList.size() + scoreGroup.numScores()) % 2 == 0 ?
                    MyersBriggsTypeIndicator.Judging : MyersBriggsTypeIndicator.Perceiving);
        }

        return new MBTIFeature(mbtiList);
    }
}
