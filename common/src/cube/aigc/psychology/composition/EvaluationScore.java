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
        if (this.hit == 0) {
            return null;
        }

        double score = this.positiveScore - this.negativeScore;

        StringBuilder buf = new StringBuilder();
        switch (this.indicator) {
            case Obsession:
                if (score > 0.2 && score <= 0.4) {
                    buf.append("轻度强迫症的报告描述");
                } else if (score > 0.4 && score <= 0.8) {
                    buf.append("中度强迫症的报告描述");
                } else if (score > 0.8) {
                    buf.append("重度强迫症的报告描述");
                }
                break;
            case Depression:
                if (score > 0.3 && score <= 0.5) {
                    buf.append("轻度抑郁的报告描述");
                } else if (score > 0.5 && score <= 0.9) {
                    buf.append("中度抑郁的报告描述");
                } else if (score > 0.9) {
                    buf.append("严重抑郁的报告描述");
                }
                break;
            case Anxiety:
                if (score > 0.3 && score <= 0.5) {
                    buf.append("轻度焦虑的报告描述");
                } else if (score > 0.5 && score <= 0.9) {
                    buf.append("中度焦虑的报告描述");
                } else if (score > 0.9) {
                    buf.append("严重焦虑的报告描述");
                }
                break;
            case Hostile:
                if (score > 0.2 && score <= 0.4) {
                    buf.append("轻微敌对的报告描述");
                } else if (score > 0.4 && score <= 0.8) {
                    buf.append("中度敌对的报告描述");
                } else if (score > 0.8) {
                    buf.append("严重敌对的报告描述");
                }
                break;
            case Paranoid:
                if (score > 0.2 && score <= 0.5) {
                    buf.append("轻微偏执的报告描述");
                } else if (score > 0.5 && score <= 0.8) {
                    buf.append("中度偏执的报告描述");
                } else if (score > 0.8) {
                    buf.append("严重偏执的报告描述");
                }
                break;
            case SelfControl:
                if (score >= 0.8) {
                    buf.append("自我控制较好的报告描述");
                } else if (score >= 0.0) {
                    buf.append("自我控制一般的报告描述");
                } else {
                    buf.append("自我控制较差的报告描述");
                }
                break;
            case Creativity:
                if (score >= 1.2) {
                    buf.append("有较强的创造力的报告描述");
                } else if (score >= 0.5) {
                    buf.append("有一定的创造力的报告描述");
                } else {
                    buf.append("创造力一般的报告描述");
                }
                break;
            case SelfConsciousness:
                if (score > 0.5) {
                    buf.append("自我意识强的报告描述");
                } else if (score > 0.3) {
                    buf.append("自我意识中等的报告描述");
                } else {
                    buf.append("自我意识不强的报告描述");
                }
                break;
            case Confidence:
                if (score > 1.0) {
                    buf.append("自信心很强的报告描述");
                } else if (score > 0.3) {
                    buf.append("自信心较强的报告描述");
                } else {
                    buf.append("自信心不足的报告描述");
                }
                break;
            case Independence:
                if (score > 0.3) {
                    buf.append("较为独立的报告描述");
                } else {
                    buf.append("较为依赖环境的报告描述");
                }
                break;
            case EvaluationFromOutside:
                if (score >= 1.0) {
                    buf.append("非常重视外在评价的报告描述");
                } else if (score > 0.3) {
                    buf.append("较为重视外在评价的报告描述");
                }
                break;
            case AchievementMotivation:
                if (score > 1.0) {
                    buf.append("很强的成就动机的报告描述");
                } else if (score > 0.6) {
                    buf.append("中度的成就动机的报告描述");
                } else {
                    buf.append("较弱的成就动机的报告描述");
                }
                break;
            case SenseOfSecurity:
                if (score >= 0.5) {
                    buf.append("安全感较好的报告描述");
                } else if (score > 0.3) {
                    buf.append("安全感合格的报告描述");
                } else {
                    buf.append("安全感很差的报告描述");
                }
                break;
            case Attacking:
                if (score > 0.7) {
                    buf.append("很强的攻击性的报告描述");
                } else if (score > 0.4) {
                    buf.append("中度的攻击性的报告描述");
                } else {
                    buf.append("较小的攻击性的报告描述");
                }
                break;
            case Impulsion:
                if (score > 1.0) {
                    buf.append("重度冲动性的报告描述");
                } else if (score > 0.5) {
                    buf.append("中度冲动性的报告描述");
                } else {
                    buf.append("轻微冲动性的报告描述");
                }
                break;
            case Narcissism:
                if (score > 1.0) {
                    buf.append("重度自恋的报告描述");
                } else if (score > 0.5) {
                    buf.append("中度自恋的报告描述");
                } else {
                    buf.append("轻度自恋的报告描述");
                }
                break;
            case Emotion:
                if (score > 0.3) {
                    buf.append("情绪较为稳定的报告描述");
                } else {
                    buf.append("情绪较不稳定的报告描述");
                }
                break;
            case Constrain:
                if (score > 1.0) {
                    buf.append("重度压抑的报告描述");
                } else if (score > 0.5) {
                    buf.append("中度压抑的报告描述");
                } else {
                    buf.append("不太压抑的报告描述");
                }
                break;
            case SocialAdaptability:
                if (score >= 0.3) {
                    buf.append("社会适应性良好的报告描述");
                } else {
                    buf.append("社会适应性较差的报告描述");
                }
                break;
            case Family:
                if (score > 1.0) {
                    buf.append("较为重视家庭关系的报告描述");
                } else if (score > 0.8) {
                    buf.append("一般重视家庭关系的报告描述");
                } else {
                    buf.append("不太重视家庭关系的报告描述");
                }
                break;
            case InterpersonalRelation:
                if (score >= 1.3) {
                    buf.append("人际关系很受朋友欢迎的报告描述");
                } else if (score >= 1.0) {
                    buf.append("人际关系有点距离感的报告描述");
                } else if (score > 0.1) {
                    buf.append("人际关系疏远的报告描述");
                } else if (score > -0.3 ) {
                    buf.append("轻微人际敏感的报告描述");
                } else if (score > -0.8 ) {
                    buf.append("中度人际敏感的报告描述");
                } else {
                    buf.append("严重人际敏感的报告描述");
                }
                break;
            case Idealism:
                if (score >= 0.3) {
                    buf.append("理想主义者的报告描述");
                }
                break;
            case Realism:
                if (score >= 0.3) {
                    buf.append("现实主义者的报告描述");
                }
                break;
            case Optimism:
                if (score > 0.1) {
                    buf.append("乐观者的报告描述");
                }
                break;
            case Pessimism:
                if (score > 0.1) {
                    buf.append("悲观者的报告描述");
                }
                break;
            case Psychosis:
                if (score > 1.5) {
                    buf.append("重度精神病性的报告描述");
                } else if ( score > 1.0) {
                    buf.append("中度精神病性的报告描述");
                } else if (score > 0.5) {
                    buf.append("轻度精神病性的报告描述");
                }
                break;
            default:
                buf.append("个体的");
                buf.append(this.indicator.name);
                buf.append("心理特点");
                break;
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
