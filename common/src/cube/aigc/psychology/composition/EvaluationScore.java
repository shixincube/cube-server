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

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.algorithm.Score;
import cube.common.JSONable;
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

    public EvaluationScore(Indicator indicator) {
        this.indicator = indicator;
    }

    public EvaluationScore(Indicator indicator, int value, double weight) {
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
    }

    public EvaluationScore(JSONObject json) {
        this.indicator = Indicator.parse(json.getString("indicator"));
        this.hit = json.getInt("hit");
        this.value = json.getInt("value");
        this.positiveScore = json.getDouble("positiveScore");
        this.negativeScore = json.getDouble("negativeScore");
    }

    public double calcScore() {
        return this.positiveScore - this.negativeScore;
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

    public String generateReportPrompt() {
        if (this.hit == 0 || (this.positiveScore == 0 && this.negativeScore == 0)) {
            return null;
        }

        String word = this.generateWord();
        if (null == word || word.length() == 0) {
            return null;
        }

        return word + "的报告描述";
    }

    public String generateSuggestionPrompt() {
        if (this.hit == 0 || (this.positiveScore == 0 && this.negativeScore == 0)) {
            return null;
        }

        String word = this.generateWord();
        if (null == word || word.length() == 0) {
            return null;
        }

        return word + "的建议";
    }

    public String generateWord() {
        double score = this.positiveScore - this.negativeScore;

        StringBuilder buf = new StringBuilder();
        switch (this.indicator) {
            case Obsession:
                if (score >= 0.1 && score <= 0.4) {
                    buf.append("轻度强迫症");
                } else if (score > 0.4 && score <= 0.8) {
                    buf.append("中度强迫症");
                } else if (score > 0.8) {
                    buf.append("重度强迫症");
                } else {
                    return null;
                }
                break;
            case Depression:
                if (score > 0.3 && score <= 0.7) {
                    buf.append("轻度抑郁");
                } else if (score > 0.7 && score <= 1.1) {
                    buf.append("中度抑郁");
                } else if (score > 1.1) {
                    buf.append("严重抑郁");
                } else {
                    return null;
                }
                break;
            case Anxiety:
                if (score >= 0.2 && score <= 0.7) {
                    buf.append("轻度焦虑");
                } else if (score > 0.7 && score <= 1.1) {
                    buf.append("中度焦虑");
                } else if (score > 1.1) {
                    buf.append("严重焦虑");
                } else {
                    return null;
                }
                break;
            case Hostile:
                if (score > 0.2 && score <= 0.4) {
                    buf.append("轻微敌对");
                } else if (score > 0.4 && score <= 0.8) {
                    buf.append("中度敌对");
                } else if (score > 0.8) {
                    buf.append("严重敌对");
                } else {
                    return null;
                }
                break;
            case Paranoid:
                if (score > 0.2 && score <= 0.5) {
                    buf.append("轻微偏执");
                } else if (score > 0.5 && score <= 0.8) {
                    buf.append("中度偏执");
                } else if (score > 0.8) {
                    buf.append("严重偏执");
                } else {
                    return null;
                }
                break;
            case SelfControl:
                if (score >= 0.8) {
                    buf.append("自我控制较好");
                } else if (score >= 0.0) {
                    buf.append("自我控制一般");
                } else {
                    buf.append("自我控制较差");
                }
                break;
            case Creativity:
                if (score >= 1.2) {
                    buf.append("有较强的创造力");
                } else if (score >= 0.5) {
                    buf.append("有一定的创造力");
                } else {
                    buf.append("创造力一般");
                }
                break;
            case SelfConsciousness:
                if (score > 0.5) {
                    buf.append("自我意识强");
                } else if (score > 0.3) {
                    buf.append("自我意识中等");
                } else {
                    buf.append("自我意识不强");
                }
                break;
            case Confidence:
                if (score > 1.0) {
                    buf.append("自信心很强");
                } else if (score > 0.3) {
                    buf.append("自信心较强");
                } else if (score > -0.3) {
                    buf.append("自信心不足");
                } else {
                    return null;
                }
                break;
            case Independence:
                if (score > 0.3) {
                    buf.append("较为独立");
                } else {
                    buf.append("较为依赖环境");
                }
                break;
            case EvaluationFromOutside:
                if (score >= 1.0) {
                    buf.append("非常重视外在评价");
                } else if (score > 0.3) {
                    buf.append("较为重视外在评价");
                } else {
                    buf.append("一般重视外在评价");
                }
                break;
            case AchievementMotivation:
                if (score > 1.0) {
                    buf.append("很强的成就动机");
                } else if (score > 0.6) {
                    buf.append("中度的成就动机");
                } else {
                    buf.append("较弱的成就动机");
                }
                break;
            case SenseOfSecurity:
                if (score >= 0.5) {
                    buf.append("安全感较好");
                } else if (score >= -0.3) {
                    buf.append("安全感合格");
                } else {
                    buf.append("安全感很差");
                }
                break;
            case Attacking:
                if (score > 0.7) {
                    buf.append("很强的攻击性");
                } else if (score > 0.4) {
                    buf.append("中度的攻击性");
                } else {
                    buf.append("较小的攻击性");
                }
                break;
            case Impulsion:
                if (score > 1.0) {
                    buf.append("重度冲动性");
                } else if (score > 0.5) {
                    buf.append("中度冲动性");
                } else {
                    buf.append("轻微冲动性");
                }
                break;
            case Narcissism:
                if (score > 1.0) {
                    buf.append("重度自恋");
                } else if (score > 0.5) {
                    buf.append("中度自恋");
                } else {
                    buf.append("轻度自恋");
                }
                break;
            case Emotion:
                if (score > 0.3) {
                    buf.append("情绪较为稳定");
                } else {
                    buf.append("情绪较不稳定");
                }
                break;
            case Constrain:
                if (score > 1.0) {
                    buf.append("重度压抑");
                } else if (score > 0.5) {
                    buf.append("中度压抑");
                } else {
                    buf.append("不太压抑");
                }
                break;
            case SocialAdaptability:
                if (score >= 0.3) {
                    buf.append("社会适应性良好");
                } else if (score <= -0.2) {
                    buf.append("社会适应性较差");
                } else {
                    return null;
                }
                break;
            case Family:
                if (score > 1.0) {
                    buf.append("较为重视家庭关系");
                } else if (score > 0.8) {
                    buf.append("一般重视家庭关系");
                } else {
                    buf.append("不太重视家庭关系");
                }
                break;
            case InterpersonalRelation:
                if (score >= 3.5) {
                    buf.append("人际关系很受朋友欢迎");
                } else if (score >= 2.1) {
                    buf.append("人际关系良好");
                } else if (score >= 1.0) {
                    buf.append("人际关系有点距离感");
                } else if (score > 0.1) {
                    buf.append("人际关系疏远");
                } else if (score > -0.3 ) {
                    buf.append("轻微人际敏感");
                } else if (score > -0.8 ) {
                    buf.append("中度人际敏感");
                } else {
                    buf.append("严重人际敏感");
                }
                break;
            case Idealism:
                if (score >= 0.3) {
                    buf.append("理想主义者");
                } else {
                    return null;
                }
                break;
            case Realism:
                if (score >= 0.3) {
                    buf.append("现实主义者");
                } else {
                    return null;
                }
                break;
            case Optimism:
                if (score > 0.1) {
                    buf.append("乐观者");
                } else {
                    return null;
                }
                break;
            case Pessimism:
                if (score > 0.1) {
                    buf.append("悲观者");
                } else {
                    return null;
                }
                break;
            case Psychosis:
                if (score > 1.5) {
                    buf.append("重度精神病性");
                } else if ( score > 1.0) {
                    buf.append("中度精神病性");
                } else if (score > 0.5) {
                    buf.append("轻度精神病性");
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        return buf.toString();
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
