/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.composition.Answer;
import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScaleResult;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.entity.AIGCUnit;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 提交答题卡动作。
 *
 * <p>对应线协议动作 {@code submitPsychologyAnswerSheet}，逐字符等同于既有枚举
 * {@code AIGCAction.SubmitPsychologyAnswerSheet} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code NoToken → IllegalOperation → Ok / Failure / InvalidParameter}。</p>
 *
 * <p><b>本动作是量表族中依赖最重的一个</b>：主观题需要先由模型产出自由文本，
 * 再按 TF-IDF 权重把文本匹配回备选项。因此它同时用到三个宿主能力：
 * 单元选择、同步生成、TF-IDF 关键词抽取。迁移前这些能力由
 * {@code PsychologyScene} 直接调用 {@code AIGCService} 与
 * {@code TFIDFAnalyzer}（均在 service 模块），插件不可见。</p>
 *
 * <p><b>与宿主侧并存</b>：宿主 {@code PsychologyScene#inferScaleAnswer}
 * 仍然保留（{@code AIGCCellet} 里的 else-if 未删除），因此本文件与宿主侧
 * 存在<b>两份等价的答案评级实现</b>，由 {@code aigc-modules.properties}
 * 的模块开关决定走哪一份。修改评级规则时<b>两份必须同步</b>，
 * 否则会出现「启用插件前后结果不一致」。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link ListPsychologyScalesAction}。</p>
 */
public final class SubmitPsychologyAnswerSheetAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        if (null == ctx.getHost().resolveToken(tokenCode)) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }

        JSONObject requestData = ctx.getRequest().data;

        try {
            AnswerSheet answerSheet = new AnswerSheet(requestData);

            ScaleResult scaleResult = ((PsychologyModule) ctx.getModule()).submitAnswerSheet(ctx, answerSheet);

            if (null == scaleResult) {
                // 迁移前此处回显原始请求体，不是空对象
                ctx.respond(AIGCStateCode.Failure, requestData);
                return AIGCStateCode.Failure;
            }

            ctx.respond(AIGCStateCode.Ok, scaleResult.toCompactJSON());
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }

    /**
     * 为主观题推断答案。
     *
     * <p>逐字复刻宿主 {@code PsychologyScene#inferScaleAnswer}：先用
     * {@code Baize2} 生成描述文本，失败回退 {@code Baize}，再按各备选项
     * 文本的 TF-IDF 前 3 个关键词做包含匹配；命中不到时取第一项。</p>
     *
     * <p>数字类关键词会被跳过——它们在选项文本里过于常见，容易产生误命中。</p>
     *
     * @param ctx 动作上下文。
     * @param scale 待评级的量表。
     */
    static void inferScaleAnswers(ActionContext ctx, Scale scale) {
        List<cube.aigc.psychology.composition.Question> questions = scale.getQuestions();
        if (null == questions) {
            return;
        }

        for (cube.aigc.psychology.composition.Question question : questions) {
            inferScaleAnswer(ctx, scale, question);
        }
    }

    /**
     * 为单个主观题推断答案。
     *
     * @param ctx 动作上下文。
     * @param scale 待评级的量表。
     * @param question 待推断的题目。
     */
    private static void inferScaleAnswer(ActionContext ctx, Scale scale,
            cube.aigc.psychology.composition.Question question) {
        if (!question.isDescriptive()) {
            return;
        }

        // 已有答案则跳过
        if (question.hasChosen()) {
            return;
        }

        GeneratingRecord record = generate(ctx, question);
        if (null == record) {
            // 生成失败：退化为第一项，与迁移前一致
            Answer first = question.answers.get(0);
            question.chooseAnswer(first.code);
            question.setInferenceResult(first.content);
            return;
        }

        // 各备选项文本的 TF-IDF 关键词
        List<List<String>> answerKeywordList = new ArrayList<>();
        for (Answer answer : question.answers) {
            answerKeywordList.add(ctx.getHost().extractKeywords(answer.content, 3));
        }

        Answer hit = null;
        int index = 0;
        for (List<String> answerKeywords : answerKeywordList) {
            for (String word : answerKeywords) {
                if (cube.util.TextUtils.isNumeric(word)) {
                    // 跳过数字
                    continue;
                }

                if (record.answer.contains(word)) {
                    hit = question.answers.get(index);
                    break;
                }
            }

            ++index;
            if (null != hit) {
                break;
            }
        }

        if (null == hit) {
            // 一个关键词都没命中，退化为第一项
            hit = question.answers.get(0);
        }

        question.chooseAnswer(hit.code);
        question.setInferenceResult(record.answer);
    }

    /**
     * 生成题目的推断文本。
     *
     * <p>优先使用 {@code Baize2} 单元，失败回退 {@code Baize}。</p>
     *
     * @param ctx 动作上下文。
     * @param question 待推断的题目。
     * @return 返回生成记录；两级单元均失败时返回 <code>null</code>。
     */
    private static GeneratingRecord generate(ActionContext ctx,
            cube.aigc.psychology.composition.Question question) {
        AIGCUnit unit = ctx.getHost().selectUnit(ModelConfig.BAIZE_2_UNIT);
        if (null == unit) {
            return null;
        }

        GeneratingRecord record = ctx.getHost().syncGenerateText(unit,
                question.makeInferencePrompt(), null, null, null);

        if (null != record) {
            return record;
        }

        // 回退到主单元
        AIGCUnit fallback = ctx.getHost().selectUnit(ModelConfig.BAIZE_UNIT);
        if (null == fallback) {
            return null;
        }

        return ctx.getHost().syncGenerateText(fallback,
                question.makeInferencePrompt(), null, null, null);
    }
}
