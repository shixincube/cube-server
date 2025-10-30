/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
                return Indicator.Aggression;
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
