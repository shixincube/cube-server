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

package cube.aigc.psychology;

/**
 * 专业描述词。
 */
public enum Term {

    /**
     * 自我存在。
     */
    SelfExistence("自我存在"),

    /**
     * 自尊。
     */
    SelfEsteem("自尊"),

    /**
     * 自信心。
     */
    SelfConfidence("自信心"),

    /**
     * 自制力。
     */
    SelfControl("自制力"),

    /**
     * 自我无力感。
     */
    Powerlessness("自我无力感"),

    /**
     * 自恋。
     */
    Narcissism("自恋"),

    /**
     * 自满。
     */
    Complacent("自满"),

    /**
     * 社会适应性。
     */
    SocialAdaptability("社会适应性"),

    /**
     * 对环境疏离感。
     */
    EnvironmentalAlienation("环境疏离感"),

    /**
     * 对环境依赖。
     */
    EnvironmentalDependence("环境依赖感"),

    /**
     * 对环境亲切感。
     */
    EnvironmentalFriendliness("环境亲切感"),

    /**
     * 防御性。
     */
    Defensiveness("防御性"),

    /**
     * 内向。
     */
    Introversion("内向"),

    /**
     * 外向。
     */
    Extroversion("外向"),

    /**
     * 朴素。
     */
    Simple("朴素"),

    /**
     * 想法简单。
     */
    SimpleIdea("想法简单"),

    /**
     * 理想化。
     */
    Idealization("理想化"),

    /**
     * 现实感。
     */
    Realization("现实感"),

    /**
     * 本能。
     */
    Instinct("本能"),

    /**
     * 怀旧。
     */
    Nostalgia("怀旧"),

    /**
     * 憧憬未来。
     */
    Future("憧憬未来"),

    /**
     * 追求奢华。
     */
    Luxurious("追求奢华"),

    /**
     * 幻想型。
     */
    Fantasy("幻想型"),

    /**
     * 幼稚。
     */
    Childish("幼稚"),

    /**
     * 极端化。
     */
    Extreme("极端化"),

    /**
     * 厌世。
     */
    WorldWeariness("厌世"),

    /**
     * 完美主义。
     */
    Perfectionism("完美主义"),

    /**
     * 心理压力。
     */
    MentalStress("心理压力"),

    /**
     * 外部压力。
     */
    ExternalPressure("外部压力"),

    /**
     * 逃避现实。
     */
    Escapism("逃避现实"),

    /**
     * 特立独行。
     */
    Maverick("特立独行"),

    /**
     * 追求人际关系。
     */
    PursueInterpersonalRelationships("追求人际关系"),

    /**
     * 情感淡漠。
     */
    EmotionalIndifference("情感淡漠"),

    /**
     * 情感稳定。
     */
    EmotionalStability("情感稳定"),

    /**
     * 情绪不安。
     */
    MoodDisturbance("情绪不安"),

    /**
     * 依赖性。
     */
    Dependence("依赖性"),

    /**
     * 社交无力感。
     */
    SocialPowerlessness("社交无力感"),

    /**
     * 社交需求。
     */
    SocialDemand("社交需求"),

    /**
     * 自我需求。
     */
    SelfDemand("自我需求"),

    /**
     * 自我膨胀。
     */
    SelfInflated("自我膨胀"),

    /**
     * 敏感。
     */
    Sensitiveness("敏感"),

    /**
     * 感性。
     */
    Emotionality("感性"),

    /**
     * 多疑。
     */
    Suspiciousness("多疑"),

    /**
     * 直率。
     */
    Straightforwardness("直率"),

    /**
     * 警惕。
     */
    Vigilance("警惕"),

    /**
     * 警戒心。
     */
    Alertness("警戒心"),

    /**
     * 抑郁。
     */
    Depression("抑郁"),

    /**
     * 焦虑。
     */
    Anxiety("焦虑"),

    /**
     * 创造力。
     */
    Creativity("创造力"),

    /**
     * 独立。
     */
    Independence("独立"),

    /**
     * 创伤。
     */
    Trauma("创伤"),

    /**
     * 精力旺盛。
     */
    HighEnergy("精力旺盛"),

    /**
     * 安全感。
     */
    SenseOfSecurity("安全感"),

    /**
     * 追求成就。
     */
    PursuitOfAchievement("追求成就"),

    /**
     * 多目标。
     */
    ManyGoals("多目标"),

    /**
     * 多欲望。
     */
    ManyDesires("多欲望"),

    /**
     * 内心冲突。
     */
    IntrapsychicConflict("内心冲突"),

    /**
     * 敌意。
     */
    Hostility("敌意"),

    /**
     * 注意细节。
     */
    AttentionToDetail("注意细节"),

    /**
     * 渴望母爱。
     */
    LongingForMaternalLove("渴望母爱"),

    /**
     * 坚强。
     */
    Strong("坚强"),

    /**
     * 固执。
     */
    Stubborn("固执"),

    /**
     * 控制欲。
     */
    DesireForControl("控制欲"),

    /**
     * 多愁善感。
     */
    Sentimentality("多愁善感"),

    /**
     * 攻击性。
     */
    Aggression("攻击性"),

    /**
     * 拘谨。
     */
    Cautious("拘谨"),

    /**
     * 积极期待。
     */
    PositiveExpectation("积极期待"),

    /**
     * 想象力。
     */
    Imagination("想象力"),

    /**
     * 情感细腻。
     */
    DelicateEmotions("情感细腻"),

    /**
     * 渴望自由。
     */
    DesireForFreedom("渴望自由"),

    /**
     * 虚荣。
     */
    Vanity("虚荣"),

    /**
     * 渴望保护。
     */
    NeedProtection("渴望保护"),

    /**
     * 刻板。
     */
    Stereotype("刻板"),

    /**
     * 重视家庭。
     */
    PayAttentionToFamily("重视家庭"),

    /**
     * 未知。
     */
    Unknown("未知");

    public final String word;

    Term(String word) {
        this.word = word;
    }

    public static Term parse(String word) {
        for (Term term : Term.values()) {
            if (term.word.equals(word)) {
                return term;
            }
        }
        return Unknown;
    }
}
