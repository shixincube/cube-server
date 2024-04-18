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
            this.positiveScore += score.value * score.weight;
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

        StringBuilder buf = new StringBuilder();
        switch (this.indicator) {
            case Obsession:
                if (this.positiveScore > 0.2 && this.positiveScore < 0.4) {
                    buf.append("轻度强迫症的报告描述");
                } else if (this.positiveScore >= 0.4 && this.positiveScore < 0.8) {
                    buf.append("中度强迫症的报告描述");
                } else if (this.positiveScore >= 0.8) {
                    buf.append("重度强迫症的报告描述");
                }
                break;
            case Depression:
                if (this.positiveScore > 0.3 && this.positiveScore < 0.5) {
                    buf.append("轻度抑郁的报告描述");
                } else if (this.positiveScore >= 0.5 && this.positiveScore < 0.9) {
                    buf.append("中度抑郁的报告描述");
                } else if (this.positiveScore >= 0.9) {
                    buf.append("严重抑郁的报告描述");
                }
                break;
            case Anxiety:
                if (this.positiveScore > 0.3 && this.positiveScore < 0.5) {
                    buf.append("轻度焦虑的报告描述");
                } else if (this.positiveScore >= 0.5 && this.positiveScore < 0.9) {
                    buf.append("中度焦虑的报告描述");
                } else if (this.positiveScore >= 0.9) {
                    buf.append("严重焦虑的报告描述");
                }
                break;
            case Hostile:
                if (this.positiveScore > 0.2 && this.positiveScore < 0.4) {
                    buf.append("轻微敌对的报告描述");
                } else if (this.positiveScore >= 0.4 && this.positiveScore < 0.8) {
                    buf.append("中度敌对的报告描述");
                } else if (this.positiveScore >= 0.8) {
                    buf.append("严重敌对的报告描述");
                }
                break;
            case Paranoid:
                if (this.positiveScore > 0.2 && this.positiveScore < 0.5) {
                    buf.append("轻微偏执的报告描述");
                } else if (this.positiveScore >= 0.5 && this.positiveScore < 0.8) {
                    buf.append("中度偏执的报告描述");
                } else if (this.positiveScore >= 0.8) {
                    buf.append("严重偏执的报告描述");
                }
                break;
            case SelfControl:
                // 自我控制
                if (this.positiveScore >= 0.2) {
                    buf.append("自我控制较好的报告描述");
                } else if (this.negativeScore >= 0.2) {
                    buf.append("自我控制较差的报告描述");
                } else {
                    buf.append("自我控制一般的报告描述");
                }
                break;
            case Creativity:
                // 创造力
                if (this.positiveScore > 1.2) {
                    buf.append("有较强的创造力的报告描述");
                } else if (this.positiveScore >= 0.5) {
                    buf.append("有一定的创造力的报告描述");
                } else {
                    buf.append("创造力一般的报告描述");
                }
                break;
            case SelfConsciousness:
                // 自我意识
                if (this.positiveScore < 0.2) {
                    buf.append("自我意识不强的报告描述");
                } else if (this.positiveScore < 1.2) {
                    buf.append("自我意识中等的报告描述");
                } else {
                    buf.append("自我意识强的报告描述");
                }
                break;
            case Confidence:
                if (this.positiveScore < 0.2) {
                    buf.append("自信心不足的报告描述");
                } else if (this.positiveScore < 1.5) {
                    buf.append("自信心较强的报告描述");
                } else {
                    buf.append("自信心很强的报告描述");
                }
                break;
            case EvaluationFromOutside:
                if (this.value >= 2) {
                    buf.append("非常重视外在评价的报告描述");
                } else if (this.value == 1) {
                    buf.append("较为重视外在评价的报告描述");
                }
                break;
            case AchievementMotivation:
                if (this.value > 1 && this.value < 4) {
                    buf.append("中度的成就动机的报告描述");
                } else if (this.value >= 4) {
                    buf.append("很强的成就动机的报告描述");
                } else {
                    buf.append("较弱的成就动机的报告描述");
                }
                break;
            case SenseOfSecurity:
                if (this.negativeScore > 1.0) {
                    buf.append("安全感很差的报告描述");
                } else if (this.negativeScore >= 0.3) {
                    buf.append("安全感合格的报告描述");
                } else {
                    buf.append("安全感较好的报告描述");
                }
                break;
            case Attacking:
                if (this.value <= 1) {
                    buf.append("较小的攻击性的报告描述");
                } else if (this.value <= 3) {
                    buf.append("中度的攻击性的报告描述");
                } else {
                    buf.append("很强的攻击性的报告描述");
                }
                break;
            case Impulsion:
                if (this.positiveScore <= 0.5) {
                    buf.append("轻微冲动性的报告描述");
                } else if (this.positiveScore <= 1.2) {
                    buf.append("中度冲动性的报告描述");
                } else {
                    buf.append("重度冲动性的报告描述");
                }
                break;
            case Narcissism:
                if (this.positiveScore <= 0.5) {
                    buf.append("不太自恋的报告描述");
                } else if (this.positiveScore <= 1.2) {
                    buf.append("中度自恋的报告描述");
                } else {
                    buf.append("重度自恋的报告描述");
                }
                break;
            case Constrain:
                if (this.positiveScore <= 0.5) {
                    buf.append("不太压抑的报告描述");
                } else if (this.positiveScore <= 1.2) {
                    buf.append("中度压抑的报告描述");
                } else {
                    buf.append("重度压抑的报告描述");
                }
                break;
            case SocialAdaptability:
                if (this.positiveScore >= 0.3) {
                    buf.append("社会适应性良好的报告描述");
                } else {
                    buf.append("社会适应性较差的报告描述");
                }
                break;
            case Family:
                if (this.value >= 3) {
                    buf.append("较为重视家庭关系的报告描述");
                } else if (this.value >= 2) {
                    buf.append("一般重视家庭关系的报告描述");
                } else {
                    buf.append("不太重视家庭关系的报告描述");
                }
                break;
            case InterpersonalRelation:
                if (this.positiveScore >= 1.3) {
                    buf.append("人际关系很受朋友欢迎的报告描述");
                } else if (this.positiveScore >= 1.0) {
                    buf.append("人际关系有点距离感的报告描述");
                } else {
                    buf.append("人际关系疏远的报告描述");
                }
                break;
            default:
                buf.append(this.indicator.name);
                buf.append("的报告描述。");
                break;
        }
        return buf.toString();
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
