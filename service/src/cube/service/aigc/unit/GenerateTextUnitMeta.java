/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.aigc.Page;
import cube.aigc.Usage;
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.attachment.FileAttachment;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.Explorer;
import cube.service.aigc.guidance.SkillMeta;
import cube.service.aigc.guidance.SkillRegistry;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.ReadPageListener;
import cube.service.aigc.resource.Relay;
import cube.service.aigc.resource.ResourceAnswer;
import cube.service.contact.ContactManager;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GenerateTextUnitMeta extends UnitMeta {

    /**
     * 截断技能内容时的提示语，用于让模型知道技能指令不完整。
     */
    private final static String SKILL_TRUNCATED_NOTICE = "\n\n[技能内容因上下文预算不足被截断]";

    /**
     * 允许写入的最小技能段长度，低于该长度直接整体丢弃而不做半截截断。
     */
    private final static int MIN_SKILL_SECTION_LENGTH = 256;

    /**
     * SKILL 指令段模板：第一个参数为 SKILL 名称，第二个参数为 SKILL 内容。
     */
    private final static String SKILL_SECTION_FORMAT = "### Skill: %s\n%s";

    /**
     * 携带 SKILL 指令的提示词模板：第一个参数为 SKILL 指令，第二个参数为用户的问题。
     */
    private final static String SKILL_PROMPT_FORMAT = "%s\n\n请严格按照上述技能指令完成用户的请求。\n\n用户请求：%s";

    public final long sn;

    public final AIGCChannel channel;

    protected Contact participant;

    protected String content;

    protected String originalQuery;

    protected GeneratingOption option;

    protected List<String> categories;

    protected List<GeneratingRecord> histories;

    protected int maxHistories;

    protected List<Attachment> attachments;

    protected GenerateTextListener listener;

    protected AIGCChatHistory history;

    protected boolean recordHistoryEnabled = true;

    protected boolean networkingEnabled = false;

    /**
     * 本次请求实际注入的技能名称，用于留痕。
     */
    protected final List<String> injectedSkills = new ArrayList<>();

    /**
     * 本次请求因预算不足被截断或丢弃的技能名称，用于留痕与排查。
     */
    protected final List<String> truncatedSkills = new ArrayList<>();

    public GenerateTextUnitMeta(AIGCService service, AIGCUnit unit, AIGCChannel channel, String content,
                                GeneratingOption option,
                                List<String> categories,
                                List<GeneratingRecord> histories,
                                List<Attachment> attachments,
                                GenerateTextListener listener) {
        super(service, unit);
        this.sn = Utils.generateSerialNumber();
        this.channel = channel;
        this.participant = ContactManager.getInstance().getContact(channel.getAuthToken().getDomain(),
                channel.getAuthToken().getContactId());
        this.content = content;
        this.option = option;
        this.categories = categories;
        this.histories = histories;
        this.attachments = attachments;
        this.maxHistories = 0;
        this.listener = listener;

        this.history = new AIGCChatHistory(this.sn, this.channel.getCode(), unit.getCapability().getName(),
                this.participant.getDomain().getName());
        this.history.queryContactId = channel.getAuthToken().getContactId();
        this.history.queryTime = System.currentTimeMillis();
        this.history.queryContent = content;
    }

    public void setNetworkingEnabled(boolean value) {
        this.networkingEnabled = value;
    }

    public void setRecordHistoryEnabled(boolean value) {
        this.recordHistoryEnabled = value;
    }

    public void setOriginalQuery(String originalQuery) {
        if (null == originalQuery) {
            return;
        }

        this.originalQuery = originalQuery;
        this.history.queryContent = originalQuery;
    }

    public void setMaxHistories(int max) {
        this.maxHistories = max;
    }

    @Override
    public void process() {
        this.channel.setLastUnitMetaSn(this.sn);

        // 识别内容
        ComplexContext complexContext = option.recognizeContext ?
                this.service.recognizeContext(this.content, this.channel.getAuthToken()) :
                new ComplexContext();

        // 设置是否进行联网分析
        complexContext.setNetworking(this.networkingEnabled);

        GeneratingRecord result = null;

        final StringBuilder prompt = new StringBuilder(this.content);

        if (complexContext.isSimplified()) {
            // 一般文本

            int recommendHistories = 5;

            // 上下文窗口长度
            final int contextLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());

            // SKILL 指令的独立预算：默认占上下文窗口的 25%，防止整篇技能把上下文吃光
            final SkillRegistry skillRegistry = this.service.getSkillRegistry();
            final int skillBudget = skillRegistry.getSkillBudgetChars(contextLimit);

            // 解析 categories 里配置的 SKILL 名称，未被识别为 SKILL 的作为知识释义分类
            List<String> knowledgeCategories = new ArrayList<>();
            final String skillInstruction = this.loadSkills(this.categories, knowledgeCategories, skillBudget);

            // 提示词长度限制：扣除用户内容与 SKILL 指令占用的长度。
            // 禁止负预算静默丢内容：一旦越界明确告警并留痕。
            int lengthLimit = contextLimit - this.content.length()
                    - ((null != skillInstruction) ? skillInstruction.length() : 0);
            if (lengthLimit <= 0) {
                Logger.w(this.getClass(), "#process - Prompt budget exhausted - context: " + contextLimit
                        + ", query: " + this.content.length()
                        + ", skill: " + ((null != skillInstruction) ? skillInstruction.length() : 0)
                        + ", channel: " + this.channel.getCode());
                // 钳制为 0，后续附件与历史记录都不会再被装配
                lengthLimit = 0;
            }

            // 技能调用留痕：与对话历史通过 sn 关联
            this.recordSkillInvocation(skillInstruction, contextLimit, skillBudget, lengthLimit);

            JSONObject data = new JSONObject();
            data.put("unit", this.unit.getCapability().getName());
            data.put("content", this.content);
            data.put("participant", this.participant.toCompactJSON());
            data.put("option", this.option.toJSON());

            if (null != this.attachments) {
                // 处理附件
                List<FileLabel> fileList = new ArrayList<>();
                for (Attachment attachment : this.attachments) {
                    if (attachment.getType().equals(cube.aigc.complex.attachment.FileAttachment.TYPE)) {
                        if (null == this.history.queryFileLabels) {
                            this.history.queryFileLabels = new ArrayList<>();
                        }
                        cube.aigc.complex.attachment.FileAttachment fileAttachment = (FileAttachment) attachment;
                        this.history.queryFileLabels.add(fileAttachment.fileLabel);
                        fileList.add(fileAttachment.fileLabel);
                    }
                }

                // 构建提示词
                StringBuilder buf = new StringBuilder();

                List<RetrieveReRankResult> retrieveReRankList = analyseFiles(fileList, this.content);
                List<RetrieveReRankResult.Answer> answerList = new ArrayList<>();
                for (RetrieveReRankResult rrr : retrieveReRankList) {
                    answerList.addAll(rrr.getAnswerList());
                }
                // 按照得分从高到底
                answerList.sort(new Comparator<RetrieveReRankResult.Answer>() {
                    @Override
                    public int compare(RetrieveReRankResult.Answer a1, RetrieveReRankResult.Answer a2) {
                        return (int) Math.round((a2.score - a1.score) * 100);
                    }
                });
                for (RetrieveReRankResult.Answer answer : answerList) {
                    if (buf.length() + answer.content.length() >= lengthLimit) {
                        break;
                    }
                    buf.append(answer.content).append("\n");
                }

                if (buf.length() > 0) {
                    prompt.delete(0, prompt.length());
                    prompt.append(Consts.formatQuestion(buf.toString(), this.content));

                    // 更新提示词
                    data.remove("content");
                    data.put("content", prompt.toString());
                }
            }

            // 将 SKILL 指令注入提示词，一并提交给大模型
            if (null != skillInstruction) {
                String question = prompt.toString();
                prompt.delete(0, prompt.length());
                prompt.append(String.format(SKILL_PROMPT_FORMAT, skillInstruction, question));

                // 更新提示词
                data.put("content", prompt.toString());

                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#process - SKILL prompt length: " + prompt.length());
                }
            }

            // 处理多轮历史记录
            int lengthCount = prompt.length();
            List<GeneratingRecord> candidateRecords = new ArrayList<>();
            if (null == this.histories) {
                int validNumHistories = this.maxHistories;
                if (validNumHistories > 0) {
                    List<GeneratingRecord> records = this.channel.getLastHistory(validNumHistories);
                    // 正序列表转为倒序以便计算上下文长度
                    Collections.reverse(records);
                    for (GeneratingRecord record : records) {
                        // 判断长度
                        lengthCount += record.totalWords();
                        if (lengthCount > lengthLimit) {
                            // 长度越界
                            break;
                        }
                        // 加入候选
                        candidateRecords.add(record);
                    }
                    // 候选列表的倒序转为正序
                    Collections.reverse(candidateRecords);
                }
            }
            else {
                for (int i = 0; i < this.histories.size(); ++i) {
                    GeneratingRecord record = this.histories.get(i);
                    if (record.hasQueryFile() || record.hasQueryAddition()) {
                        // 为了兼容旧版本，排除掉附件类型
                        continue;
                    }

                    lengthCount += record.totalWords();
                    // 判断长度
                    if (lengthCount > lengthLimit) {
                        // 长度越界
                        break;
                    }
                    // 加入候选
                    candidateRecords.add(record);
                    if (candidateRecords.size() >= recommendHistories) {
                        break;
                    }
                }
                // 翻转顺序
                Collections.reverse(candidateRecords);
            }

            // 加入分类释义
            if (!knowledgeCategories.isEmpty()) {
                this.fillRecords(candidateRecords, knowledgeCategories, lengthLimit - lengthCount,
                        this.unit.getCapability().getName());
            }

            // 写入多轮对话历史数组
            JSONArray history = new JSONArray();
            for (GeneratingRecord record : candidateRecords) {
                history.put(record.toJSON());
            }
            data.put("history", history);

            if (this.content.contains(Consts.NO_CONTENT_SENTENCE) || Consts.NO_CONTENT_SENTENCE.contains(this.content)) {
                // 知识库会使用 NO_CONTENT_SENTENCE 作为答案
                String responseText = Consts.NO_CONTENT_SENTENCE;
                result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        (null != this.originalQuery) ? this.originalQuery : this.content,
                        responseText, "", null, complexContext);
            }
            else if (this.networkingEnabled) {
                // 启用搜索或者启用联网信息检索都执行搜索
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        // 进行资源搜索
                        SearchResult searchResult = Explorer.getInstance().search(
                                (null != originalQuery) ? originalQuery : content, channel.getAuthToken());
                        if (searchResult.hasResult()) {
                            // 执行搜索问答
                            performSearchPageQA(content, unit.getCapability().getName(),
                                    searchResult, complexContext, 3);
                        }
                        else {
                            // 没有搜索结果
                            complexContext.fixNetworkingResult(null, null);
                        }
                    }
                });

                String responseText = Consts.SEARCHING_INTERNET_FOR_INFORMATION;
                result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        (null != this.originalQuery) ? this.originalQuery : this.content,
                        responseText, "", null, complexContext);
            }
            else if (this.service.useRelay) {
                GeneratingRecord generatingRecord =
                        Relay.getInstance().generateText(channel.getCode(), this.unit.getCapability().getName(),
                                this.content, new GeneratingOption(), this.histories);
                if (null != generatingRecord) {
                    // 过滤中文字符
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content,
                            generatingRecord.answer, generatingRecord.thought, null, complexContext);
                }
                else {
                    this.channel.setProcessing(false);
                    // 回调失败
                    this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                    return;
                }
            }
            else {
                Packet request = new Packet(AIGCAction.TextToText.name, data);
                ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(),
                        3 * 60 * 1000, this.sn);
                if (null == dialect) {
                    Logger.w(AIGCService.class, "Unit error - channel: " + this.channel.getCode());
                    // 记录故障
                    this.unit.markFailure(AIGCStateCode.UnitError.code, System.currentTimeMillis(),
                            channel.getAuthToken().getContactId());
                    // 频道状态
                    this.channel.setProcessing(false);
                    // 回调错误
                    this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                    return;
                }

                // 是否被中断
                if (this.service.getCellet().isInterruption(dialect)) {
                    Logger.d(AIGCService.class, "Channel interrupted: " + this.channel.getCode());
                    this.channel.setProcessing(false);
                    // 回调错误
                    this.listener.onFailed(this.channel, AIGCStateCode.Interrupted);
                    return;
                }

                Packet response = new Packet(dialect);
                JSONObject payload = Packet.extractDataPayload(response);

                String responseText = "";
                String thoughtText = "";
                JSONObject resultPayload = null;
                Usage usage = null;
                try {
                    responseText = payload.getString("response");
                    thoughtText = payload.has("thought") ? payload.getString("thought") : "";
                    if (payload.has("resultPayload")) {
                        resultPayload = payload.getJSONObject("resultPayload");
                    }
                    if (payload.has("performance")) {
                        usage = new Usage(payload.getJSONObject("performance"));
                    }
                } catch (Exception e) {
                    Logger.w(AIGCService.class, "Unit respond failed - channel: " + this.channel.getCode(), e);
                    // 记录故障
                    this.unit.markFailure(AIGCStateCode.Failure.code, System.currentTimeMillis(),
                            channel.getAuthToken().getContactId());
                    // 频道状态
                    this.channel.setProcessing(false);
                    // 回调错误
                    this.listener.onFailed(this.channel, AIGCStateCode.Failure);
                    return;
                }

                // 过滤中文字符
                responseText = this.filterChinese(this.unit, responseText);
                result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        (null != this.originalQuery) ? this.originalQuery : this.content,
                        responseText.trim(), thoughtText.trim(), resultPayload, complexContext);
                // 设置用量
                result.usage = usage;
            }
        }
        else {
            // 复杂上下文：提取资源内容后进行推理
            ResourceAnswer resourceAnswer = new ResourceAnswer(complexContext);
            // 提取内容
            String content = resourceAnswer.extractContent(this.service, this.channel.getAuthToken());
            String answer = resourceAnswer.answer(content);
            result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                    (null != this.originalQuery) ? this.originalQuery : this.content,
                    answer.trim(), "", null, complexContext);
        }

        if (complexContext.isSimplified()) {
            if (this.networkingEnabled) {
                // 缓存上下文
                Explorer.getInstance().cacheComplexContext(complexContext);
            }
        }
        else {
            // 缓存上下文
            Explorer.getInstance().cacheComplexContext(complexContext);
        }

        this.history.answerContactId = unit.getContact().getId();
        this.history.answerTime = System.currentTimeMillis();
        this.history.answerContent = result.answer;
        this.history.thought = result.thought;

        // 设置上下文
        this.history.context = complexContext;

        // 重置状态位
        this.channel.setProcessing(false);

        this.listener.onGenerated(this.channel, result);

        final Usage usage = result.usage;
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 更新用量
                if (null != usage) {
                    service.getStorage().updateUsage(history.queryContactId, history.unit,
                            usage.outputTokens, usage.inputTokens);
                }
                else {
                    List<String> tokens = calcTokens(prompt.toString());
                    int promptTokens = tokens.size();
                    tokens = calcTokens(history.answerContent);
                    int completionTokens = tokens.size();
                    service.getStorage().updateUsage(history.queryContactId, history.unit,
                            completionTokens, promptTokens);
                }

                // 保存历史记录
                if (recordHistoryEnabled) {
                    service.getStorage().writeHistory(history);
                }
            }
        });
    }

    protected String filterChinese(AIGCUnit unit, String text) {
        if (unit.getCapability().getName().equalsIgnoreCase(ModelConfig.BAIZE_UNIT)) {
            if (TextUtils.containsChinese(text)) {
                return text.replaceAll(",", "，");
            }
            else {
                return text;
            }
        }
        else {
            return text;
        }
    }

    protected void performSearchPageQA(String query, String unitName, SearchResult searchResult,
                                       ComplexContext context, int maxPages) {
        Object mutex = new Object();
        AtomicInteger pageCount = new AtomicInteger(0);

        List<String> urlList = new ArrayList<>();
        for (SearchResult.OrganicResult or : searchResult.organicResults) {
            if (Explorer.getInstance().isIgnorableUrl(or.link)) {
                // 跳过忽略的 URL
                continue;
            }

            urlList.add(or.link);
            if (urlList.size() >= maxPages) {
                break;
            }
        }

        List<Page> pages = new ArrayList<>();

        for (String url : urlList) {
            Explorer.getInstance().readPageContent(url, new ReadPageListener() {
                @Override
                public void onCompleted(String url, Page page) {
                    pageCount.incrementAndGet();

                    if (null != page) {
                        pages.add(page);
                    }

                    if (pageCount.get() >= urlList.size()) {
                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }
                }
            });
        }

        synchronized (mutex) {
            try {
                mutex.wait(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        StringBuilder pageContent = new StringBuilder();

        for (Page page : pages) {
            StringBuilder buf = new StringBuilder();
            for (String text : page.textList) {
                buf.append(text).append("\n");
                if (buf.length() > ModelConfig.BAIZE_CONTEXT_LIMIT) {
                    break;
                }
            }

            if (buf.length() > 2) {
                buf.delete(buf.length() - 1, buf.length());
                // 提取页面与提问匹配的信息
                String prompt = Consts.formatExtractContent(buf.toString(), query);
                GeneratingRecord answer = this.service.syncGenerateText(this.channel.getAuthToken(), ModelConfig.BAIZE_UNIT, prompt,
                        new GeneratingOption());
                if (null != answer) {
                    // 记录内容
                    pageContent.append(answer.answer);
                }
            }
        }

        if (pageContent.length() <= 10) {
            Logger.d(this.getClass(), "#performSearchPageQA - No page content, cid:"
                    + this.channel.getAuthToken().getContactId());
            // 使用 null 值填充
            context.fixNetworkingResult(null, null);
            return;
        }

        final int lengthLimit = ModelConfig.getPromptLengthLimit(unitName);
        if (pageContent.length() > lengthLimit) {
            String[] tmp = pageContent.toString().split("。");
            pageContent = new StringBuilder();
            for (String text : tmp) {
                pageContent.append(text).append("。");
                if (pageContent.length() >= lengthLimit) {
                    break;
                }
            }
            pageContent.delete(pageContent.length() - 1, pageContent.length());
        }

        // 对提取出来的内容进行推理
        String prompt = Consts.formatQuestion(pageContent.toString(), query);
        GeneratingRecord result = this.service.syncGenerateText(this.channel.getAuthToken(), unitName, prompt, new GeneratingOption());
        if (null == result) {
            Logger.w(this.getClass(), "#performSearchPageQA - Infers page content failed, cid:"
                    + this.channel.getAuthToken().getContactId());
            // 使用 null 值填充
            context.fixNetworkingResult(null, null);
            return;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#performSearchPageQA - Result length: " + result.answer.length());
        }

        // 将页面推理结果填充到上下文
        context.fixNetworkingResult(pages, result.answer);
    }

    protected void fillRecords(List<GeneratingRecord> recordList, List<String> categories, int lengthLimit,
                               String unitName) {
        int total = 0;
        for (String category : categories) {
            List<KnowledgeParaphrase> list = this.service.getStorage().readKnowledgeParaphrases(category);
            for (KnowledgeParaphrase paraphrase : list) {
                total += paraphrase.getWord().length() + paraphrase.getParaphrase().length();
                if (total > lengthLimit) {
                    break;
                }

                GeneratingRecord record = new GeneratingRecord(unitName,
                        paraphrase.getWord(), paraphrase.getParaphrase());
                recordList.add(record);
            }

            if (total > lengthLimit) {
                break;
            }
        }
    }

    /**
     * 解析 categories 里配置的 SKILL 名称，从 {@link SkillRegistry} 加载技能内容并拼装 SKILL 指令段。
     * 未被识别为技能的分类名称收集到 knowledgeCategories 里，用于加载知识释义。
     *
     * <p>技能受独立预算约束：总长超过 {@code budget} 时先按剩余额度截断，剩余额度连
     * {@link #MIN_SKILL_SECTION_LENGTH} 都放不下则整体丢弃，并把被截断/丢弃的技能名记录到
     * {@link #truncatedSkills} 留痕，绝不静默丢内容。</p>
     *
     * @param categories 分类名称列表，SKILL 名称也从此列表指定。
     * @param knowledgeCategories 输出参数，收集未被识别为 SKILL 的分类名称。
     * @param budget SKILL 指令可占用的最大字符数。
     * @return 返回拼装好的 SKILL 指令文本，如果没有任何 SKILL 被加载则返回 null。
     */
    protected String loadSkills(List<String> categories, List<String> knowledgeCategories, int budget) {
        if (null == categories || categories.isEmpty()) {
            return null;
        }

        final SkillRegistry registry = this.service.getSkillRegistry();
        final String domain = this.channel.getAuthToken().getDomain();
        final int limit = Math.max(0, budget);

        StringBuilder buf = new StringBuilder();
        for (String category : categories) {
            String skillName = (null == category) ? null : category.trim();
            if (null == skillName || skillName.isEmpty()) {
                continue;
            }

            SkillMeta skill = registry.getSkill(skillName, domain);
            if (null == skill || !skill.enabled) {
                // 不是技能名称，按知识释义分类处理
                if (null != knowledgeCategories) {
                    knowledgeCategories.add(category);
                }
                continue;
            }

            String content = (null == skill.content) ? "" : skill.content.trim();
            String section = String.format(SKILL_SECTION_FORMAT, skill.name, content);
            int separatorLength = (buf.length() > 0) ? 2 : 0;
            int available = limit - buf.length() - separatorLength;

            if (limit <= 0 || available < section.length()) {
                // 预算不足：按剩余额度截断，剩余额度不足以承载最小可用长度时整体丢弃
                boolean truncated = available > MIN_SKILL_SECTION_LENGTH;
                if (truncated) {
                    if (buf.length() > 0) {
                        buf.append("\n\n");
                    }
                    buf.append(GenerateTextUnitMeta.truncateSkillSection(skill.name, content, available));
                    this.injectedSkills.add(skill.name);
                }

                this.truncatedSkills.add(skill.name);
                Logger.w(GenerateTextUnitMeta.class, "#loadSkills - Skill \"" + skill.name
                        + "\" exceeds the skill budget, needed: " + section.length()
                        + ", available: " + Math.max(0, available) + ", truncated: " + truncated);
                continue;
            }

            if (buf.length() > 0) {
                buf.append("\n\n");
            }
            buf.append(section);
            this.injectedSkills.add(skill.name);

            if (Logger.isDebugLevel()) {
                Logger.d(GenerateTextUnitMeta.class, "#loadSkills - Skill \"" + skill.name
                        + "\" loaded, length: " + content.length());
            }
        }

        return (buf.length() > 0) ? buf.toString() : null;
    }

    /**
     * 按可用长度截断技能段落：保留开头部分并追加截断提示，返回长度不超过 {@code available}。
     */
    protected static String truncateSkillSection(String skillName, String content, int available) {
        String header = String.format(SKILL_SECTION_FORMAT, skillName, "");
        int bodyBudget = available - header.length() - SKILL_TRUNCATED_NOTICE.length();
        if (bodyBudget <= 0) {
            return header + SKILL_TRUNCATED_NOTICE;
        }

        if (content.length() <= bodyBudget) {
            // 内容本身放得下，不截断也不加提示，避免误导模型
            return String.format(SKILL_SECTION_FORMAT, skillName, content);
        }

        return header + content.substring(0, bodyBudget) + SKILL_TRUNCATED_NOTICE;
    }

    /**
     * 记录技能调用留痕：与对话历史通过 {@code sn} 关联，落库到 {@code aigc_skill_invocation}。
     * 没有任何技能参与时不产生记录；写入是异步的，不阻塞本次生成。
     *
     * @param skillInstruction 最终拼装的 SKILL 指令，可为 {@code null}。
     * @param contextLimit 上下文窗口长度。
     * @param skillBudget SKILL 指令的预算上限。
     * @param remaining 扣除用户内容与技能指令后剩余的预算。
     */
    protected void recordSkillInvocation(String skillInstruction, int contextLimit, int skillBudget, int remaining) {
        if (this.injectedSkills.isEmpty() && this.truncatedSkills.isEmpty()) {
            return;
        }

        final JSONObject trace = new JSONObject();
        trace.put("sn", this.sn);
        trace.put("channel", this.channel.getCode());
        trace.put("unit", this.unit.getCapability().getName());
        trace.put("domain", this.channel.getAuthToken().getDomain());
        trace.put("contactId", this.channel.getAuthToken().getContactId());

        JSONArray injected = new JSONArray();
        for (String name : this.injectedSkills) {
            injected.put(name);
        }
        trace.put("skills", injected);

        JSONArray truncated = new JSONArray();
        for (String name : this.truncatedSkills) {
            truncated.put(name);
        }

        JSONObject detail = new JSONObject();
        detail.put("injected", injected);
        detail.put("truncated", truncated);

        JSONObject budget = new JSONObject();
        budget.put("context", contextLimit);
        budget.put("skillBudget", skillBudget);
        budget.put("skillUsed", (null != skillInstruction) ? skillInstruction.length() : 0);
        budget.put("remaining", remaining);
        trace.put("budget", budget);

        trace.put("timestamp", System.currentTimeMillis());

        final AIGCService service = this.service;
        service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != service.getStorage()) {
                        service.getStorage().writeSkillInvocation(trace);
                    }
                } catch (Exception e) {
                    Logger.w(GenerateTextUnitMeta.class, "#recordSkillInvocation", e);
                }
            }
        });
    }
}
