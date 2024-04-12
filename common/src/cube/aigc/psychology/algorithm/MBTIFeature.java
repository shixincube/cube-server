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

import cube.common.JSONable;
import org.json.JSONObject;

import java.util.List;

/**
 * MBTI 性格特征。
 */
public class MBTIFeature implements JSONable {

    // 感观型

    public final static MBTIFeature ISTJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature ISFJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature INFJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature INTJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature ISTP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature ISFP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature INFP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature INTP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Introversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Perceiving
    });


    // 直觉型

    public final static MBTIFeature ESTP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature ESFP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature ENFP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature ENTP = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Perceiving
    });

    public final static MBTIFeature ESTJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature ESFJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Sensing,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature ENFJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Feeling,
            MyersBriggsTypeIndicator.Judging
    });

    public final static MBTIFeature ENTJ = new MBTIFeature(new MyersBriggsTypeIndicator[] {
            MyersBriggsTypeIndicator.Extraversion,
            MyersBriggsTypeIndicator.Intuition,
            MyersBriggsTypeIndicator.Thinking,
            MyersBriggsTypeIndicator.Judging
    });


    private MyersBriggsTypeIndicator[] indicators;

    private String name;

    private String description;

    public MBTIFeature(MyersBriggsTypeIndicator[] indicators) {
        this.indicators = indicators;
        this.parse();
    }

    public MBTIFeature(List<MyersBriggsTypeIndicator> indicators) {
        this.indicators = this.build(indicators);
        this.parse();
    }

    private MyersBriggsTypeIndicator[] build(List<MyersBriggsTypeIndicator> indicators) {
        MyersBriggsTypeIndicator[] result = new MyersBriggsTypeIndicator[4];
        result[0] = indicators.contains(MyersBriggsTypeIndicator.Introversion) ?
                MyersBriggsTypeIndicator.Introversion : MyersBriggsTypeIndicator.Extraversion;
        result[1] = indicators.contains(MyersBriggsTypeIndicator.Sensing) ?
                MyersBriggsTypeIndicator.Sensing : MyersBriggsTypeIndicator.Intuition;
        result[2] = indicators.contains(MyersBriggsTypeIndicator.Feeling) ?
                MyersBriggsTypeIndicator.Feeling : MyersBriggsTypeIndicator.Thinking;
        result[3] = indicators.contains(MyersBriggsTypeIndicator.Judging) ?
                MyersBriggsTypeIndicator.Judging : MyersBriggsTypeIndicator.Perceiving;
        return result;
    }

    private void parse() {
        if (ISTJ.equals(this)) {
            this.name = "检查员型";
            this.description = "安静、严肃，通过全面性和可靠性获得成功。实际，有责任感。决定有逻辑性，并一步步地朝着目标前进，不易分心。喜欢将工作、 家庭和生活都安排得井井有条。重视传统和忠诚。";
        }
        else if (ISFJ.equals(this)) {
            this.name = "照顾者型";
            this.description = "安静、友好、有责任感和良知。坚定地致力于完成他们的义务。全面、勤勉、精确，忠诚、体贴，留心和记得他们重视的人的小细节， 关心他们的感受。努力把工作和家庭环境营造得有序而温馨。";
        }
        else if (INFJ.equals(this)) {
            this.name = "博爱型";
            this.description = "寻求思想、关系、物质等之间的意义和联系。希望了解什么能够激励人，对人有很强的洞察力。有责任心，坚持自己的价值观。对于怎 样更好的服务大众有清晰的远景。在对于目标的实现过程中有计划而且果断坚定。";
        }
        else if (INTJ.equals(this)) {
            this.name = "专家型";
            this.description = "在实现自己的想法和达成自己的目标时有创新的想法和非凡的动力。能很快洞察到外界事物间的规律并形成长期的远景计划。一旦决定 做一件事就会开始规划并直到完成为止。多疑、独立，对于自己和他人能力和表现的要求都非常高。";
        }
        else if (ISTP.equals(this)) {
            this.name = "冒险家型";
            this.description = "灵活、忍耐力强，是个安静的观察者直到有问题发生，就会马上行动，找到实用的解决方法。分析事物运作的原理，能从大量的信息 中很快的找到关键的症结所在。对于原因和结果感兴趣，用逻辑的方式处理问题，重视效率。";
        }
        else if (ISFP.equals(this)) {
            this.name = "艺术家型";
            this.description = "安静、友好、敏感、和善。享受当前。喜欢有自己的空间，喜欢能按照自己的时间表工作。对于自己的价值观和自己觉得重要的人非 常忠诚，有责任心。不喜欢争论和冲突。不会将自己的观念和价值观强加到别人身上。";
        }
        else if (INFP.equals(this)) {
            this.name = "哲学家型";
            this.description = "理想主义，对于自己的价值观和自己觉得重要的人非常忠诚。希望外部的生活和自己内心的价值观是统一的。好奇心重，很快能看到 事情的可能性，能成为实现想法的催化剂。寻求理解别人和帮助他们实现潜能。适应力强，灵活，善于接受，除非是有悖于自己的价值观的。";
        }
        else if (INTP.equals(this)) {
            this.name = "学者型";
            this.description = "对于自己感兴趣的任何事物都寻求找到合理的解释。喜欢理论性的和抽象的事物，热衷于思考而非社交活动。安静、内向、灵活、适应 力强。对于自己感兴趣的领域有超凡的集中精力深度解决问题的能力。多疑，有时会有点挑剔，喜欢分析。";
        }
    }

    public String getCode() {
        return this.indicators[0].code + this.indicators[1].code +
                this.indicators[2].code + this.indicators[3].code;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MBTIFeature) {
            MBTIFeature other = (MBTIFeature) obj;
            return (other.indicators[0] == this.indicators[0] &&
                    other.indicators[1] == this.indicators[1] &&
                    other.indicators[2] == this.indicators[2] &&
                    other.indicators[3] == this.indicators[3]);
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        return null;
    }

    @Override
    public JSONObject toCompactJSON() {
        return null;
    }
}
