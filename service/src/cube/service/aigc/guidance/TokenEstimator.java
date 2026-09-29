/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;

/**
 * Token 估算器。
 *
 * <p>提示词预算是按 Token 计量的，但拼装阶段只能拿到字符数。本类维护「每 Token 对应字符数」
 * 的在线估计值（{@code charsPerToken}），把字符数换算为 Token 数。</p>
 *
 * <p>估计值随模型单元返回的真实用量（{@code inputTokens}）自动校准：每次真实调用完成后调用
 * {@link #observe(int, long)}，用指数加权移动平均（EWMA）向实测值收敛。因此服务运行一段时间后，
 * 估算误差会稳定在较小范围；校准值只在单个实例内生效（属于估算，不要求多实例一致）。</p>
 *
 * <p>估算值被约束在 {@link #MIN_CHARS_PER_TOKEN} ~ {@link #MAX_CHARS_PER_TOKEN} 之间，
 * 并对明显异常的观测值（例如单元返回的用量单位与字符数严重不成比例）直接丢弃，避免被脏数据带偏。</p>
 */
public class TokenEstimator {

    /**
     * 默认的每 Token 字符数。中文为主的内容约 1.5~2 个字符/Token，英文约 4 个字符/Token，
     * 混排场景取 1.8 作为初始值。
     */
    public final static double DEFAULT_CHARS_PER_TOKEN = 1.8;

    public final static double MIN_CHARS_PER_TOKEN = 0.5;

    public final static double MAX_CHARS_PER_TOKEN = 8.0;

    /**
     * 默认的 EWMA 平滑系数：新观测值占 30% 权重。
     */
    public final static double DEFAULT_ALPHA = 0.3;

    private static final TokenEstimator sInstance = new TokenEstimator();

    public static TokenEstimator getInstance() {
        return sInstance;
    }

    private volatile double charsPerToken = DEFAULT_CHARS_PER_TOKEN;

    private volatile double alpha = DEFAULT_ALPHA;

    /**
     * 累计观测次数与最近一次估算偏差，用于留痕与排查。
     */
    private volatile long observations = 0L;

    private volatile double lastActualRatio = 0.0;

    private TokenEstimator() {
    }

    /**
     * 应用配置。
     *
     * @param initialCharsPerToken 初始的每 Token 字符数，超出有效区间时使用默认值。
     * @param alpha EWMA 平滑系数，超出 (0, 1] 时使用默认值。
     */
    public void configure(double initialCharsPerToken, double alpha) {
        this.charsPerToken = sanitize(initialCharsPerToken, DEFAULT_CHARS_PER_TOKEN);
        this.alpha = (alpha > 0.0 && alpha <= 1.0) ? alpha : DEFAULT_ALPHA;
    }

    /**
     * 估算文本占用的 Token 数。
     *
     * @param text 文本，允许为 {@code null}。
     * @return 返回 Token 数，至少为 0。
     */
    public int estimate(String text) {
        if (null == text || text.isEmpty()) {
            return 0;
        }

        return (int) Math.ceil(text.length() / this.charsPerToken);
    }

    /**
     * 估算字符数对应的 Token 数。用于把 Token 预算反算为可容纳的字符数。
     *
     * @param textLength 字符数。
     * @return 返回 Token 数，至少为 0。
     */
    public int estimateByLength(int textLength) {
        if (textLength <= 0) {
            return 0;
        }

        return (int) Math.ceil(textLength / this.charsPerToken);
    }

    /**
     * 把 Token 预算换算为可容纳的字符数。
     *
     * @param tokens Token 数。
     * @return 返回字符数，至少为 0。
     */
    public int tokensToChars(int tokens) {
        if (tokens <= 0) {
            return 0;
        }

        return (int) (tokens * this.charsPerToken);
    }

    /**
     * 用真实用量校准估算值。
     *
     * @param promptChars 本次请求实际的提示词字符数。
     * @param inputTokens 模型单元返回的输入 Token 数。
     */
    public void observe(int promptChars, long inputTokens) {
        if (promptChars <= 0 || inputTokens <= 0) {
            return;
        }

        double ratio = (double) promptChars / (double) inputTokens;
        if (ratio < MIN_CHARS_PER_TOKEN || ratio > MAX_CHARS_PER_TOKEN) {
            // 观测值不在合理区间：丢弃，避免污染校准值
            if (Logger.isDebugLevel()) {
                Logger.d(TokenEstimator.class, "#observe - Ignore outlier ratio: " + ratio
                        + " - chars: " + promptChars + ", tokens: " + inputTokens);
            }
            return;
        }

        double updated = this.charsPerToken * (1.0 - this.alpha) + ratio * this.alpha;
        this.charsPerToken = sanitize(updated, this.charsPerToken);
        this.lastActualRatio = ratio;
        ++this.observations;
    }

    /**
     * 重置校准状态，回到初始估计值。
     */
    public void reset() {
        this.charsPerToken = DEFAULT_CHARS_PER_TOKEN;
        this.observations = 0L;
        this.lastActualRatio = 0.0;
    }

    public double getCharsPerToken() {
        return this.charsPerToken;
    }

    public long getObservations() {
        return this.observations;
    }

    public double getLastActualRatio() {
        return this.lastActualRatio;
    }

    private static double sanitize(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        if (value < MIN_CHARS_PER_TOKEN || value > MAX_CHARS_PER_TOKEN) {
            return fallback;
        }
        return value;
    }
}
