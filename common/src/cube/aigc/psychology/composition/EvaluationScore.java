/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.aigc.psychology.algorithm.Score;
import cube.common.JSONable;
import cube.common.Language;
import org.json.JSONObject;

/**
 * 评估得分。
 */
public class EvaluationScore implements JSONable {

    public final Indicator indicator;

    public int hit = 0;

    public int value = 0;

    public int positive = 0;

    public double positiveWeight = 0;

    public int negative = 0;

    public double negativeWeight = 0;

    public double positiveScore = 0;

    public double negativeScore = 0;

    public IndicatorRate rate;

    public EvaluationScore(Indicator indicator) {
        this.indicator = indicator;
    }

    public EvaluationScore(Indicator indicator, int value, double weight, Attribute attribute) {
        this.indicator = indicator;
        this.hit = 1;
        this.value = value;
        if (value > 0) {
            this.positive = value;
            this.positiveWeight = weight;
            this.positiveScore = value * weight;
        }
        else if (value < 0) {
            this.negative = Math.abs(value);
            this.negativeWeight = weight;
            this.negativeScore = Math.abs(value) * weight;
        }
        this.rate = this.getIndicatorRate(attribute);
    }

    public EvaluationScore(JSONObject json) {
        this.indicator = Indicator.parse(json.getString("indicator"));
        this.hit = json.getInt("hit");
        this.value = json.getInt("value");
        this.positiveScore = json.getDouble("positiveScore");
        this.negativeScore = json.getDouble("negativeScore");
        if (json.has("rate")) {
            this.rate = IndicatorRate.parse(json.getJSONObject("rate"));
        }
        else {
            this.rate = this.getIndicatorRate(new Attribute("male", 18, Language.Chinese, false));
        }
    }

    public IndicatorRate getRate(Attribute attribute) {
        if (null == this.rate) {
            this.rate = this.getIndicatorRate(attribute);
        }
        return this.rate;
    }

    public double calcScore() {
        return this.positiveScore - this.negativeScore;
    }

    public double calcScoreReLU() {
        double value = this.positiveScore - this.negativeScore;
        if (value < 0) {
            return 0;
        }
        return value;
    }

    public void scoring(Score score) {
        this.hit += 1;
        // 原始分
        this.value += score.value;

        if (score.value > 0) {
            // 最大权重值
            if (score.weight > this.positiveWeight) {
                this.positive = score.value;
                this.positiveWeight = score.weight;
            }

            // 计分
            this.positiveScore += Math.abs(score.value * score.weight);
        }
        else if (score.value < 0) {
            // 最大权重值
            if (score.weight > this.negativeWeight) {
                this.negative = Math.abs(score.value);
                this.negativeWeight = score.weight;
            }

            // 计分
            this.negativeScore += Math.abs(score.value) * score.weight;
        }
    }

    public String generateReportPrompt(Attribute attribute) {
        if (this.hit == 0 || (this.positiveScore == 0 && this.negativeScore == 0)) {
            return null;
        }

        String word = this.generateWord(attribute);
        if (null == word || word.length() == 0) {
            return null;
        }

        return attribute.language.isChinese() ?
                (word + "的报告描述") :
                ("The report content when the assessment indicator is \"" + word.toLowerCase() + "\"");
    }

    public String generateSuggestionPrompt(Attribute attribute) {
        if (this.hit == 0 || (this.positiveScore == 0 && this.negativeScore == 0)) {
            return null;
        }

        String word = this.generateWord(attribute);
        if (null == word || word.length() == 0) {
            return null;
        }

        return attribute.language.isChinese() ?
                (word + "的建议") :
                ("The suggestions when the assessment indicator is \"" + word.toLowerCase() + "\"");
    }

    public String generateWord(Attribute attribute) {
        IndicatorRate rate = this.getIndicatorRate(attribute);
        StringBuilder buf = new StringBuilder();
        switch (this.indicator) {
            case Depression:
                if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "抑郁倾向低" : "Low depressive tendency");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "抑郁倾向中等" : "Moderate depressive tendency");
                } else if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "抑郁倾向高" : "High depressive tendency");
                } else {
                    return null;
                }
                break;
            case Anxiety:
                if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "轻度焦虑情绪" : "Mild anxiety");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度焦虑情绪" : "Moderate anxiety");
                } else if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "重度焦虑情绪" : "Severe anxiety");
                } else {
                    return null;
                }
                break;
            case Obsession:
                if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "轻度强迫症" : "Mild obsession");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度强迫症" : "Moderate obsession");
                } else if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "重度强迫症" : "Severe obsession");
                } else {
                    return null;
                }
                break;
            case Creativity:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "有较强的创造力" : "High level of creativity");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "有一定的创造力" : "Moderate level of creativity");
                } else {
                    buf.append(attribute.language.isChinese() ? "创造力一般" : "Creativity in general");
                }
                break;
            case Pessimism:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "悲观者" : "Pessimist");
                } else {
                    return null;
                }
                break;
            case Optimism:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "乐观者" : "Optimist");
                } else {
                    return null;
                }
                break;
            case SenseOfSecurity:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "安全感较好" : "Good sense of security");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "安全感合格" : "Sense of security is satisfactory");
                } else {
                    buf.append(attribute.language.isChinese() ? "安全感一般" : "A moderate level of security");
                }
                break;
            case Extroversion:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "外倾" : "Extroversion");
                }
                break;
            case Introversion:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "内倾" : "Introversion");
                }
                break;
            case Impulsion:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "重度冲动性" : "Severe impulsiveness");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度冲动性" : "Moderate impulsiveness");
                } else {
                    buf.append(attribute.language.isChinese() ? "轻微冲动性" : "Mild impulsiveness");
                }
                break;
            case Confidence:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "自信心很强" : "Very high self-confidence");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "自信心较强" : "High self-confidence");
                } else if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "自信心不足" : "Lack of self-confidence");
                } else {
                    return null;
                }
                break;
            case Mood:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "情绪较为稳定" : "Emotions are relatively stable");
                } else {
                    buf.append(attribute.language.isChinese() ? "情绪较不稳定" : "Emotions are unstable");
                }
                break;
            case Repression:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "重度压抑" : "Severe suppression");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度压抑" : "Moderate suppression");
                } else {
                    buf.append(attribute.language.isChinese() ? "不太压抑" : "Mild suppression");
                }
                break;
            case Independence:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "较为独立" : "Relatively independent");
                } else {
                    buf.append(attribute.language.isChinese() ? "较为依赖环境" : "Relatively dependent on the environment");
                }
                break;
            case AchievementMotivation:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "很强的成就动机" : "Strong achievement motivation");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度的成就动机" : "Moderate achievement motivation");
                } else {
                    buf.append(attribute.language.isChinese() ? "较弱的成就动机" : "Weak achievement motivation");
                }
                break;
            case Paranoid:
                if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "轻微偏执" : "Mild paranoia");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度偏执" : "Moderate paranoia");
                } else if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "严重偏执" : "Severe paranoia");
                } else {
                    return null;
                }
                break;
            case Hostile:
                if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "轻微敌对" : "Mild hostility");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度敌对" : "Moderate hostility");
                } else if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "严重敌对" : "Severe hostility");
                } else {
                    return null;
                }
                break;
            case SocialAdaptability:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "社会适应性良好" : "Good social adaptability");
                } else if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "社会适应性一般" : "General social adaptability");
                } else {
                    return null;
                }
                break;
            case Narcissism:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "重度自恋" : "Severe narcissism");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度自恋" : "Moderate narcissism");
                } else {
                    buf.append(attribute.language.isChinese() ? "轻度自恋" : "Mild narcissism");
                }
                break;
            case EvaluationFromOutside:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "非常重视外在评价" : "High importance is placed on external evaluation");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "较为重视外在评价" : "Greater emphasis is placed on external evaluation");
                } else {
                    buf.append(attribute.language.isChinese() ? "一般重视外在评价" : "General attention is placed on external evaluation");
                }
                break;
            case Aggression:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "很强的攻击性" : "Severe aggression");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度的攻击性" : "Moderate aggression");
                } else {
                    buf.append(attribute.language.isChinese() ? "较小的攻击性" : "Mild aggression");
                }
                break;
            case Family:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "较为重视家庭关系" : "Putting greater emphasis on family relationships");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "一般重视家庭关系" : "Putting general attention on family relationships");
                } else {
                    buf.append(attribute.language.isChinese() ? "不太重视家庭关系" : "Not paying much attention to family relationships");
                }
                break;
            case InterpersonalRelation:
                double score = this.positiveScore - this.negativeScore;
                if (score >= 3.5) {
                    buf.append(attribute.language.isChinese() ? "人际关系很好" : "Good interpersonal relationships");
                } else if (score >= 2.1) {
                    buf.append(attribute.language.isChinese() ? "人际关系良好" : "General interpersonal relationships");
                } else if (score >= 1.0) {
                    buf.append(attribute.language.isChinese() ? "人际关系有点距离感" : "Distance in interpersonal relationships");
                } else if (score > 0.1) {
                    buf.append(attribute.language.isChinese() ? "人际关系疏远" : "Weakening of interpersonal relationships");
                } else if (score > -0.3 ) {
                    buf.append(attribute.language.isChinese() ? "轻微人际敏感" : "Mild interpersonal sensitivity");
                } else if (score > -0.8 ) {
                    buf.append(attribute.language.isChinese() ? "中度人际敏感" : "Moderate interpersonal sensitivity");
                } else {
                    buf.append(attribute.language.isChinese() ? "严重人际敏感" : "Severe interpersonal sensitivity");
                }
                break;
            case SelfControl:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "自我控制较好" : "Good self-control");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "自我控制一般" : "Self-control in general");
                } else {
                    buf.append(attribute.language.isChinese() ? "自我控制较差" : "Poor self-control");
                }
                break;
            case SelfConsciousness:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "自我意识强" : "High self-awareness");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "自我意识中等" : "Moderate self-awareness");
                } else {
                    buf.append(attribute.language.isChinese() ? "自我意识不强" : "Weak self-awareness");
                }
                break;
            case Idealism:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "理想主义者" : "Idealists");
                } else {
                    return null;
                }
                break;
            case Realism:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "现实主义者" : "Realists");
                } else {
                    return null;
                }
                break;
            case Psychosis:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "重度精神病性" : "Severe psychotic");
                } else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "中度精神病性" : "Moderate psychotic");
                } else if (rate == IndicatorRate.Low) {
                    buf.append(attribute.language.isChinese() ? "轻度精神病性" : "Mild psychotic");
                } else {
                    return null;
                }
                break;
            case LogicalThinking:
                if (rate == IndicatorRate.High) {
                    buf.append(attribute.language.isChinese() ? "逻辑思维很好" : "Excellent logical thinking");
                }
                else if (rate == IndicatorRate.Medium) {
                    buf.append(attribute.language.isChinese() ? "逻辑思维较好" : "Good logical thinking");
                }
                else {
                    buf.append(attribute.language.isChinese() ? "逻辑思维一般" : "Logical thinking is at an average level");
                }
                break;
            case SecureAttachment:
                buf.append(attribute.language.isChinese() ? "安全型依恋" : "Secure attachment");
                break;
            case AnxiousPreoccupiedAttachment:
                buf.append(attribute.language.isChinese() ? "焦虑型依恋" : "Anxious-Preoccupied attachment");
                break;
            case DismissiveAvoidantAttachment:
                buf.append(attribute.language.isChinese() ? "回避型依恋" : "Dismissive-Avoidant attachment");
                break;
            case DisorganizedAttachment:
                buf.append(attribute.language.isChinese() ? "混乱型依恋" : "Disorganized attachment");
                break;
            default:
                return null;
        }
        return buf.toString();
    }

    public IndicatorRate getIndicatorRate(Attribute attribute) {
        double score = this.positiveScore - this.negativeScore;

        IndicatorRate rate = IndicatorRate.None;
        switch (this.indicator) {
            case Depression:
                if (attribute.age < 18) {
                    if (score > 1.0 && score <= 1.4) {
                        rate = IndicatorRate.Low;
                    } else if (score > 1.4 && score <= 2.0) {
                        rate = IndicatorRate.Medium;
                    } else if (score > 2.0) {
                        rate = IndicatorRate.High;
                    }
                }
                else {
                    if (score > 0.3 && score <= 0.7) {
                        rate = IndicatorRate.Low;
                    } else if (score > 0.7 && score <= 1.1) {
                        rate = IndicatorRate.Medium;
                    } else if (score > 1.1) {
                        rate = IndicatorRate.High;
                    }
                }
                break;
            case Anxiety:
                if (attribute.age < 18) {
                    if (score >= 0.8 && score <= 1.2) {
                        rate = IndicatorRate.Low;
                    } else if (score > 1.2 && score <= 1.6) {
                        rate = IndicatorRate.Medium;
                    } else if (score > 1.6) {
                        rate = IndicatorRate.High;
                    }
                }
                else {
                    if (score >= 0.5 && score <= 1.0) {
                        rate = IndicatorRate.Low;
                    } else if (score > 1.0 && score <= 1.8) {
                        rate = IndicatorRate.Medium;
                    } else if (score > 1.8) {
                        rate = IndicatorRate.High;
                    }
                }
                break;
            case Obsession:
                if (attribute.age < 18) {
                    if (score >= 0.6 && score <= 1.0) {
                        rate = IndicatorRate.Low;
                    } else if (score > 1.0 && score <= 1.6) {
                        rate = IndicatorRate.Medium;
                    } else if (score > 1.6) {
                        rate = IndicatorRate.High;
                    }
                }
                else {
                    if (score >= 0.1 && score <= 0.8) {
                        rate = IndicatorRate.Low;
                    } else if (score > 0.8 && score <= 1.2) {
                        rate = IndicatorRate.Medium;
                    } else if (score > 1.2) {
                        rate = IndicatorRate.High;
                    }
                }
                break;
            case Creativity:
                if (score >= 0.6) {
                    rate = IndicatorRate.High;
                } else if (score >= 0.3) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Pessimism:
                if (score > 0.2) {
                    rate = IndicatorRate.High;
                }
                break;
            case Optimism:
                if (attribute.age < 18) {
                    if (score > 1.2) {
                        rate = IndicatorRate.High;
                    }
                } else {
                    if (score > 0.9) {
                        rate = IndicatorRate.High;
                    }
                }
                break;
            case SenseOfSecurity:
                if (score >= 0.5) {
                    rate = IndicatorRate.High;
                } else if (score >= -0.3) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Extroversion:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score > 0.5) {
                    rate = IndicatorRate.Medium;
                }
                break;
            case Introversion:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score >= 0.5) {
                    rate = IndicatorRate.Medium;
                }
                break;
            case Impulsion:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score > 0.5) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Confidence:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score > 0.3) {
                    rate = IndicatorRate.Medium;
                } else if (score > -0.3) {
                    rate = IndicatorRate.Low;
                }
                break;
            case Mood:
                if (score >= 0.4) {
                    rate = IndicatorRate.High;
                } else if (score < -0.1) {
                    rate = IndicatorRate.Low;
                }
                break;
            case Repression:
                if (score >= 1.1) {
                    rate = IndicatorRate.High;
                } else if (score > 0.5) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Independence:
                if (score > 0.3) {
                    rate = IndicatorRate.High;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case AchievementMotivation:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score > 0.6) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Paranoid:
                if (score > 0.2 && score <= 0.5) {
                    rate = IndicatorRate.Low;
                } else if (score > 0.5 && score <= 0.8) {
                    rate = IndicatorRate.Medium;
                } else if (score > 0.8) {
                    rate = IndicatorRate.High;
                }
                break;
            case Hostile:
                if (score > 0.2 && score <= 0.4) {
                    rate = IndicatorRate.Low;
                } else if (score > 0.4 && score <= 0.8) {
                    rate = IndicatorRate.Medium;
                } else if (score > 0.8) {
                    rate = IndicatorRate.High;
                }
                break;
            case SocialAdaptability:
                if (score >= 0.3) {
                    rate = IndicatorRate.High;
                } else if (score <= -0.2) {
                    rate = IndicatorRate.Low;
                }
                break;
            case Narcissism:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score >= 0.5) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case EvaluationFromOutside:
                if (score >= 1.0) {
                    rate = IndicatorRate.High;
                } else if (score > 0.3) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Aggression:
                if (score > 0.7) {
                    rate = IndicatorRate.High;
                } else if (score > 0.4) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Family:
                if (score > 1.0) {
                    rate = IndicatorRate.High;
                } else if (score > 0) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case InterpersonalRelation:
                if (score >= 3.5) {
                    rate = IndicatorRate.Highest;
                } else if (score >= 2.1) {
                    rate = IndicatorRate.High;
                } else if (score >= 1.0) {
                    rate = IndicatorRate.Medium;
                } else if (score > 0.1) {
                    rate = IndicatorRate.Medium;
                } else if (score > -0.3 ) {
                    rate = IndicatorRate.Low;
                } else if (score > -0.8 ) {
                    rate = IndicatorRate.Low;
                } else {
                    rate = IndicatorRate.Lowest;
                }
                break;
            case SelfControl:
                if (score >= 0.8) {
                    rate = IndicatorRate.High;
                } else if (score >= 0.0) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case SelfConsciousness:
                if (score > 0.5) {
                    rate = IndicatorRate.High;
                } else if (score > 0.3) {
                    rate = IndicatorRate.Medium;
                } else {
                    rate = IndicatorRate.Low;
                }
                break;
            case Idealism:
                if (score >= 0.32) {
                    rate = IndicatorRate.High;
                }
                break;
            case Realism:
                if (score >= 0.31) {
                    rate = IndicatorRate.High;
                }
                break;
            case Psychosis:
                if (score > 1.5) {
                    rate = IndicatorRate.High;
                } else if ( score > 1.0) {
                    rate = IndicatorRate.Medium;
                } else if (score > 0.5) {
                    rate = IndicatorRate.Low;
                }
                break;
            case LogicalThinking:
                if (score >= 1.0) {
                    rate = IndicatorRate.High;
                }
                else if (score >= 0.5) {
                    rate = IndicatorRate.Medium;
                }
                else {
                    rate = IndicatorRate.Low;
                }
                break;
            case SecureAttachment:
            case AnxiousPreoccupiedAttachment:
            case DismissiveAvoidantAttachment:
            case DisorganizedAttachment:
                if (score >= 1.2) {
                    rate = IndicatorRate.High;
                }
                else if (score >= 0.7) {
                    rate = IndicatorRate.Medium;
                }
                else {
                    rate = IndicatorRate.Low;
                }
                break;
            default:
                break;
        }

        this.rate = rate;
        return rate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EvaluationScore) {
            EvaluationScore other = (EvaluationScore) obj;
            if (other == this) {
                return true;
            }

            if (other.indicator == this.indicator) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.indicator.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("rate", (null != this.rate) ? this.rate.toJSON() :
                this.getIndicatorRate(new Attribute("male", 18, Language.Chinese, false)).toJSON());
        json.put("hit", this.hit);
        json.put("value", this.value);
        json.put("positiveScore", this.positiveScore);
        json.put("negativeScore", this.negativeScore);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
