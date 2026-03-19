/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.composition.*;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.service.aigc.scene.evaluation.Evaluation;
import cube.service.aigc.scene.evaluation.SubconsciousRelationshipBetweenACoupleEvaluation;
import cube.util.Gender;
import cube.util.TextUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ComprehensiveReportWorker implements Runnable {

    private final boolean printDebug = true;

    private AIGCService service;

    private PsychologyStorage storage;

    private AIGCChannel channel;

    private ComprehensiveReportListener listener;

    private ComprehensiveReport report;

    public ComprehensiveReportWorker(AIGCService service, PsychologyStorage storage, AIGCChannel channel, Theme theme,
                                     List<Comprehensive> comprehensives, ComprehensiveReportListener listener) {
        this.service = service;
        this.storage = storage;
        this.channel = channel;
        this.listener = listener;
        this.report = new ComprehensiveReport(theme, comprehensives);
    }

    public ComprehensiveReport getReport() {
        return this.report;
    }

    @Override
    public void run() {
        // 设置为正在操作
        this.channel.setProcessing(true);

        try {
            if (!this.verifyParameters()) {
                // 验证参数失败
                Logger.w(this.getClass(), "#run - Invalid parameter - " + this.report.sn);
                this.report.state = AIGCStateCode.InvalidParameter;
                this.report.finished = true;
                this.listener.onEvaluateFailed(this.report);
                return;
            }

            // 获取单元
            AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
            if (null == unit) {
                // 没有可用单元
                this.report.state = AIGCStateCode.UnitError;
                this.report.finished = true;
                this.listener.onEvaluateFailed(this.report);
                return;
            }

            // 更新单元状态
            unit.setRunning(true);

            // 回调正在评估
            this.listener.onEvaluating(this.report);

            for (Comprehensive comprehensive : this.report.comprehensives) {
                // 回调正在预测内容
                this.listener.onPredicting(this.report, comprehensive);

                // 1. 推理绘画
                Painting painting = this.predictPainting(unit, comprehensive.getFileLabel(), true, false);
                if (null == painting) {
                    // 预测绘图失败
                    Logger.w(this.getClass(), "#run - predictPainting failed: " +
                            comprehensive.getFileLabel().getFileCode());
                    // 更新单元状态
                    unit.setRunning(false);
                    this.report.state = AIGCStateCode.FileError;
                    this.report.finished = true;
                    this.listener.onEvaluateFailed(this.report);
                    return;
                }

                // 设置绘画
                painting.setAttribute(comprehensive.getAttribute());
                painting.fileLabel = comprehensive.getFileLabel();
                comprehensive.setPainting(comprehensive.getFileLabel().getFileCode(), painting);

                // 2. 执行绘画评估
                Evaluation evaluation = this.evaluate(painting);
                if (null == evaluation) {
                    Logger.w(this.getClass(), "#run - evaluate failed: " +
                            comprehensive.getFileLabel().getFileCode());
                    // 更新单元状态
                    unit.setRunning(false);
                    this.report.state = AIGCStateCode.FileError;
                    this.report.finished = true;
                    this.listener.onEvaluateFailed(this.report);
                    return;
                }

                // 制作报告
                EvaluationReport evaluationReport = evaluation.makeEvaluationReport();

                // 3. 执行预测
                this.predictComprehensive(comprehensive, evaluation, evaluationReport);
            }

            // 更新单元状态
            unit.setRunning(false);

            // 4. 合成结果
            boolean success = this.generateComprehensiveReport();
            if (success) {
                this.report.state = AIGCStateCode.Ok;
                this.report.elapsed = System.currentTimeMillis() - this.report.timestamp;
                this.report.finished = true;
                this.listener.onEvaluateCompleted(this.report);

                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        // 保存到数据库
                        try {
                            storage.writeComprehensiveReport(channel.getAuthToken().getContactId(), report);
                        } catch (Exception e) {
                            Logger.e(this.getClass(), "#writeComprehensiveReport", e);
                        }
                    }
                });
            }
            else {
                this.report.state = AIGCStateCode.Failure;
                this.report.elapsed = System.currentTimeMillis() - this.report.timestamp;
                this.report.finished = true;
                this.listener.onEvaluateFailed(this.report);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run - sn: " + this.report.sn, e);
        } finally {
            this.channel.setProcessing(false);
        }
    }

    private boolean verifyParameters() {
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenACouple:
                List<Comprehensive> comprehensiveList = this.report.comprehensives;
                if (comprehensiveList.size() == 2) {
                    // 将选择输入量表
                    for (Comprehensive comprehensive : comprehensiveList) {
                        if (!comprehensive.hasScales()) {
                            Scale scale = Resource.getInstance().loadScaleByName("SRBC",
                                    this.channel.getAuthToken().getContactId());
                            if (null == scale) {
                                Logger.e(this.getClass(), "#verifyParameters - Can NOT find scale: SRBC");
                                return false;
                            }

                            for (String word : comprehensive.getChoices()) {
                                SubconsciousRelationshipBetweenACoupleEvaluation.SRBCWord srbcWord =
                                        SubconsciousRelationshipBetweenACoupleEvaluation.SRBCWord.parse(word);
                                scale.chooseAnswer(1, Integer.toString(srbcWord.sn));
                            }
                            comprehensive.addScale(scale);
                        }
                    }

                    Comprehensive comprehensive1 = comprehensiveList.get(0);
                    Comprehensive comprehensive2 = comprehensiveList.get(1);

                    boolean genderOk = false;
                    Attribute attribute1 = comprehensive1.getAttribute();
                    Attribute attribute2 = comprehensive2.getAttribute();
                    // 必须一男一女
                    if ((attribute1.isMale() && attribute2.isFemale()) ||
                            (attribute1.isFemale() && attribute2.isMale())) {
                        genderOk = true;
                    }

                    // 必须选择3个词
                    boolean scaleOk = false;
                    Scale scale1 = comprehensive1.getScale();
                    Question question1 = scale1.getQuestions().get(0);
                    Scale scale2 = comprehensive2.getScale();
                    Question question2 = scale2.getQuestions().get(0);
                    if (question1.getChosenAnswers().size() == 3 &&
                            question2.getChosenAnswers().size() == 3) {
                        scaleOk = true;
                    }

                    Logger.d(this.getClass(), "#verifyParameters - gender: " + genderOk + " , scale: " + scaleOk);

                    return genderOk && scaleOk;
                }
                else {
                    return false;
                }
            default:
                return false;
        }
    }

    private Painting predictPainting(AIGCUnit unit, FileLabel fileLabel, boolean adjust, boolean upload) {
        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        data.put("adjust", adjust);
        data.put("upload", upload);
        Packet request = new Packet(AIGCAction.PredictPsychologyPainting.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 5 * 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(), "#predictPainting - Predict image unit error");
            return null;
        }

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#predictPainting - Predict image response state: " +
                    Packet.extractCode(response));
            return null;
        }

        try {
            JSONObject responseData = Packet.extractDataPayload(response);
            // 绘画识别结果
            Painting painting = new Painting(responseData.getJSONArray("result").getJSONObject(0));
            return painting;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#predictPainting", e);
            return null;
        }
    }

    private Evaluation evaluate(Painting painting) {
        Evaluation evaluation = null;
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenACouple:
                evaluation = new SubconsciousRelationshipBetweenACoupleEvaluation(
                        this.channel.getAuthToken().getContactId(), painting);
                break;
            default:
                break;
        }

        if (null == evaluation) {
            Logger.w(this.getClass(), "#evaluate - Unsupported evaluate theme: " + this.report.theme.code);
            return null;
        }

        return evaluation;
    }

    private void predictComprehensive(Comprehensive comprehensive, Evaluation evaluation, EvaluationReport evaluationReport) {
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenACouple:
                SubconsciousRelationshipBetweenACoupleEvaluation srbcEvaluation =
                        (SubconsciousRelationshipBetweenACoupleEvaluation) evaluation;

                srbcEvaluation.service = this.service;

                // 提取绘画分数
                List<EvaluationScore> scoreList = evaluationReport.getEvaluationScores();

                // 合并指标
                List<EvaluationScore> paintingScores = srbcEvaluation.mergeScoreList(scoreList);
                if (printDebug) {
                    printEvaluationScore("Painting Scores", paintingScores);
                }

                // 提取词分数
                Scale scale = comprehensive.getScale();
                List<Answer> answerList = scale.getQuestions().get(0).getChosenAnswers();
                List<String> words = new ArrayList<>();
                for (Answer answer : answerList) {
                    words.add(answer.content);
                }
                // 词指标
                List<EvaluationScore> wordScores = srbcEvaluation.evaluateWords(words);
                if (printDebug) {
                    printEvaluationScore("Word Scores", wordScores);
                }

                // 计算指标得分
                List<Score> scores = srbcEvaluation.caleIndicatorScores(paintingScores, wordScores);
                if (printDebug) {
                    printScore("Total", scores);
                }

                // 按照分数排序
                scores.sort(new Comparator<Score>() {
                    @Override
                    public int compare(Score score1, Score scores2) {
                        return scores2.value - score1.value;
                    }
                });
                Score result = scores.get(0);
                Logger.d(this.getClass(), "#predictComprehensive - Score: " + result.indicator.getName() +
                        " : " + result.value);

                // 生成内容
                ComprehensiveSection section = srbcEvaluation.generateComprehensiveSection(result);
                comprehensive.addComprehensiveSection(section);
                break;
            default:
                Logger.w(this.getClass(), "#predictComprehensive - Unsupported theme: " + this.report.theme.code);
                break;
        }
    }

    private boolean generateComprehensiveReport() {
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenACouple:
                Comprehensive male = this.report.getComprehensiveByGender(Gender.Male);
                Comprehensive female = this.report.getComprehensiveByGender(Gender.Female);
                if (null == male || null == female) {
                    Logger.e(this.getClass(), "#generateComprehensiveReport - No Male or Female data");
                    return false;
                }

                StringBuilder query = new StringBuilder(male.getKeywordWithGender());
                query.append("，").append(female.getKeywordWithGender());
                List<String> keywords = this.service.segmentText(TextUtils.filterPunctuation(query.toString()));
                List<String> contentList = Resource.getInstance().loadDataset().searchContentInOrder(
                        keywords.toArray(new String[0]), keywords.size());

                // XJW 测试用
                contentList = new ArrayList<>();
                contentList.add("## 关系特征\n\n* 一个想时刻黏着，一个想要独立空间，像“树袋熊和树干”\n* 总在靠近和推开之间拉扯，一个追一个跑\n* 女方理性独立，男方感性依赖，性格互补又互相吸引\n* 外人看是男暖女冷，其实两人都在努力找平衡\n* 吵架理由永远是“你为什么不理我”和“你能不能给我点空间”\n* 一个渴望亲密，一个需要独处，像跳探戈\n* 女方周末想自己待着看书，男方想黏着她看电影\n* 男方发十条消息，女方回一条“在忙，稍后”\n* 女方觉得男方太闲，男方觉得女方太忙\n* 两人像在不同频道，偶尔才能对上\n\n## 正面情景\n\n* 黏人的一方慢慢学会给对方空间，变得更成熟\n* 独立的一方被温暖融化，学会撒娇和依赖\n* 互补中感情更深，既能各自搞事业，又能回家抱抱\n* 磨合好了特别稳，像齿轮咬合，互相成就\n* 女方开始主动报备行程，男方学会不追问\n* 两人找到最适合的相处节奏，不远不近刚刚好\n* 女方加班时会给男方点杯奶茶，男方收到特别开心\n* 男方学会自己找事做，不再整天等消息\n* 女方偶尔撒娇，男方觉得特别珍贵\n* 两人在一起时全心投入，分开时各自精彩\n\n## 负面情景\n\n* 黏人的总觉得对方不够爱，常问“你还爱我吗”\n* 独立的总觉得被束缚，想逃又舍不得\n* 一个追一个躲，两人都心累，像拉锯战\n* 男方可能因为缺乏安全感，变得疑神疑鬼\n* 女方加班时男方连环Call，女方想摔手机\n* 久了男方可能心凉，觉得“我永远等不到她的主动”\n* 男方查岗查太勤，女方觉得不被信任\n* 女方偶尔想二人世界，男方已经习惯了独处\n* 两人各过各的，像室友不像情侣\n* 吵架时女方说“你能不能给我点空间”，男方说“空间空间，你还要多少空间”\n\n## 关键词关联提示\n\n* 翅膀/驿站：侧重自由与空间需求，可强调独立的一面\n* 港湾/灯火：侧重温暖与归宿，强调最终能达成平衡\n* 战场/枷锁：强调拉扯与冲突，多用负面描述\n* 潮汐：暗示关系时近时远，有起伏但可能找到节奏\n");

                if (contentList.isEmpty()) {
                    Logger.e(this.getClass(), "#generateComprehensiveReport - No query content: " + query.toString());
                    return false;
                }

                // 生成摘要
                GeneratingRecord summary = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                        String.format(Resource.getInstance().getCorpus("report", "SRBC_REPORT_SUMMARY"),
                                contentList.get(0)),
                        null, null, null);
                if (null == summary) {
                    Logger.e(this.getClass(), "#generateComprehensiveReport - Generates summary failed: " + query.toString());
                    return false;
                }

                // 设置摘要
                this.report.setSummary(summary.answer);

                // 融合区
                String prompt = Prompts.getPrompt("srbc-integration_zone");
                if (null == prompt) {
                    Logger.e(this.getClass(), "#generateComprehensiveReport - No prompt: srbc-integration_zone");
                    return false;
                }
                prompt = prompt.replace("{{男方爱情类型}}", male.getComprehensiveSection().indicator.getName());
                prompt = prompt.replace("{{男方三个关键词}}", male.buildChoicesString());
                prompt = prompt.replace("{{女方爱情类型}}", female.getComprehensiveSection().indicator.getName());
                prompt = prompt.replace("{{女方三个关键词}}", female.buildChoicesString());
                prompt = prompt.replace("{{情侣爱情描述内容}}", contentList.get(0));

                GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                        prompt, null, null, null);
                if (null == result) {
                    Logger.w(this.getClass(), "#generateComprehensiveReport - generate content failed: "
                            + ModelConfig.BAIZE_NEXT_UNIT + " - prompt length: " + prompt.length());
                    result = this.service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                            prompt, null, null, null);
                    if (null == result) {
                        Logger.e(this.getClass(), "#generateComprehensiveReport - generate content failed: "
                                + ModelConfig.BAIZE_X_UNIT + " - prompt length: " + prompt.length());
                        return false;
                    }
                }
//                if (printDebug) {
//                    System.out.println("Report content");
//                    System.out.println(result.answer);
//                }

                this.report.addSection(new ComprehensiveSection("双人潜意识合盘——融合区（你们共同潜意识的基石）",
                        result.answer));

                // 盲点区
                // 男方
                prompt = Prompts.getPrompt("srbc-blind_spots");
                if (null == prompt) {
                    Logger.e(this.getClass(), "#generateComprehensiveReport - No prompt: srbc-blind_spots");
                    return false;
                }
                prompt = prompt.replace("{{你的植物类型}}", male.getComprehensiveSection().indicator.getName());
                prompt = prompt.replace("{{你的三个关键词}}", male.buildChoicesString());
                prompt = prompt.replace("{{你的植物类型特点}}", male.getComprehensiveSection().getContent());
                prompt = prompt.replace("{{对方的植物类型}}", female.getComprehensiveSection().indicator.getName());
                prompt = prompt.replace("{{对方的三个关键词}}", female.buildChoicesString());
                prompt = prompt.replace("{{对方的植物类型特点}}", female.getComprehensiveSection().getContent());
                GeneratingRecord resultMale = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                        prompt, null, null, null);
                // 添加数据
                male.addComprehensiveSection(new ComprehensiveSection("你隐藏的期待，TA并未察觉", resultMale.answer));

                // 女方
                prompt = Prompts.getPrompt("srbc-blind_spots");
                prompt = prompt.replace("{{你的植物类型}}", female.getComprehensiveSection().indicator.getName());
                prompt = prompt.replace("{{你的三个关键词}}", female.buildChoicesString());
                prompt = prompt.replace("{{你的植物类型特点}}", female.getComprehensiveSection().getContent());
                prompt = prompt.replace("{{对方的植物类型}}", male.getComprehensiveSection().indicator.getName());
                prompt = prompt.replace("{{对方的三个关键词}}", male.buildChoicesString());
                prompt = prompt.replace("{{对方的植物类型特点}}", male.getComprehensiveSection().getContent());
                GeneratingRecord resultFemale = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                        prompt, null, null, null);
                // 添加数据
                female.addComprehensiveSection(new ComprehensiveSection("你隐藏的期待，TA并未察觉", resultFemale.answer));

                return true;
            default:
                break;
        }

        return false;
    }

    private void printEvaluationScore(String title, List<EvaluationScore> scores) {
        StringBuilder buf = new StringBuilder();
        buf.append("----------------------------------------------------------------\n");
        buf.append(title).append("\n");
        for (EvaluationScore score : scores) {
            buf.append(score.indicator.getName());
            buf.append(" - ");
            buf.append(score.calcScore());
            buf.append("\n");
        }
        System.out.println(buf.toString());
    }

    private void printScore(String title, List<Score> scores) {
        StringBuilder buf = new StringBuilder();
        buf.append("----------------------------------------------------------------\n");
        buf.append(title).append("\n");
        for (Score score : scores) {
            buf.append(score.indicator.getName());
            buf.append(" - ");
            buf.append(score.value);
            buf.append("\n");
        }
        System.out.println(buf.toString());
    }
}
