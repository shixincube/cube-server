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

import cube.aigc.psychology.Term;
import cube.aigc.psychology.Indicator;

public class RepresentationStrategy {

    private RepresentationStrategy() {
    }

    public static Indicator matchIndicator(Term term) {
        switch (term) {
            case Extroversion:
                return Indicator.Extroversion;
            case Introversion:
                return Indicator.Introversion;
            case Narcissism:
                return Indicator.Narcissism;
            case SelfConfidence:
                return Indicator.Confidence;
            case SelfEsteem:
                return Indicator.SelfEsteem;
            case SocialAdaptability:
                return Indicator.SocialAdaptability;
            case Independence:
                return Indicator.Independence;
            case Idealization:
                return Indicator.Idealism;
            case DelicateEmotions:
                return Indicator.Mood;
            case SelfExistence:
                return Indicator.SelfConsciousness;
            case Luxurious:
                return Indicator.Realism;
            case SenseOfSecurity:
                return Indicator.SenseOfSecurity;
            case MentalStress:
            case ExternalPressure:
                return Indicator.Stress;
            case SelfControl:
                return Indicator.SelfControl;
            case WorldWeariness:
            case Escapism:
                return Indicator.Anxiety;
            case Depression:
                return Indicator.Depression;
            case SimpleIdea:
            case Childish:
                return Indicator.Simple;
            case Hostility:
                return Indicator.Hostile;
            case Aggression:
                return Indicator.Attacking;
            case PayAttentionToFamily:
                return Indicator.Family;
            case PursueInterpersonalRelationships:
                return Indicator.InterpersonalRelation;
            case PursuitOfAchievement:
                return Indicator.AchievementMotivation;
            case Creativity:
                return Indicator.Creativity;
            case PositiveExpectation:
                return Indicator.Struggle;
            case DesireForFreedom:
                return Indicator.DesireForFreedom;
            default:
                break;
        }
        return null;
    }
}
