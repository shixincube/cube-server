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
import cube.service.aigc.guidance.PromptComposer;
import cube.service.aigc.guidance.SkillMeta;
import cube.service.aigc.guidance.SkillRegistry;
import cube.service.aigc.guidance.SkillSession;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.ReadPageListener;
import cube.service.aigc.resource.Relay;
import cube.service.aigc.resource.ResourceAnswer;
import cube.service.contact.ContactManager;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GenerateTextUnitMeta extends UnitMeta {

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

    /**
     * 本次请求由关键词自动装载的技能名称，用于留痕。
     */
    protected final List<String> autoSkills = new ArrayList<>();

    /**
     * 本次请求最终生效的技能集合（含会话已绑定的技能），用于留痕。
     */
    protected final List<String> boundSkills = new ArrayList<>();

    /**
     * 提示词分段与预算的留痕数据。
     */
    protected JSONObject promptTrace = null;

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

            // 上下文窗口长度（Token）
            final int contextLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());
            final String domain = this.channel.getAuthToken().getDomain();
            final SkillRegistry registry = this.service.getSkillRegistry();

            // 解析本次请求生效的技能：调用方显式指定 + 关键词自动装载 + 会话已绑定
            List<String> knowledgeCategories = new ArrayList<>();
            final List<String> effectiveSkills = this.resolveSkills(registry, domain, knowledgeCategories);

            // 技能全文（按名称排序，保证提示词稳定）
            List<PromptComposer.NamedText> skillSections = new ArrayList<>();
            for (String name : effectiveSkills) {
                SkillMeta skill = registry.getSkill(name, domain);
                if (null == skill || !skill.enabled) {
                    continue;
                }
                skillSections.add(new PromptComposer.NamedText(skill.name,
                        (null != skill.content) ? skill.content : ""));
                this.injectedSkills.add(skill.name);
            }
            skillSections = PromptComposer.sortSkills(skillSections);

            // 技能目录：把「平台上有哪些技能」告知模型，这是自动引入的可发现面
            String catalogText = this.service.isSkillCatalogEnabled()
                    ? registry.buildCatalog(domain) : null;

            // 已知信息：附件检索结果 + 知识释义，独立成段（不再伪装成历史对话）
            String retrievalText = joinText(this.buildAttachmentText(),
                    this.buildRetrievalText(knowledgeCategories));

            // 历史候选记录
            List<GeneratingRecord> candidateRecords = this.collectHistoryCandidates(recommendHistories);

            // 分段组装与 Token 预算
            PromptComposer.Input input = new PromptComposer.Input();
            input.contextLimitTokens = contextLimit;
            input.catalogText = catalogText;
            input.skills = skillSections;
            input.retrievalText = retrievalText;
            input.historyCap = (null == this.histories) ? 0 : recommendHistories;
            // 编排器要求历史按「由旧到新」排列：频道历史本身为正序；
            // 调用方给定的列表按旧版本语义从头开始取用，故倒序交给预算裁剪，裁剪结果再映射回原顺序。
            if (null != this.histories) {
                for (int i = candidateRecords.size() - 1; i >= 0; --i) {
                    input.historyTexts.add(candidateRecords.get(i).toJSON().toString());
                }
            }
            else {
                for (GeneratingRecord record : candidateRecords) {
                    input.historyTexts.add(record.toJSON().toString());
                }
            }
            input.userText = this.content;

            PromptComposer.Result composed = this.service.getPromptComposer().compose(input);
            this.promptTrace = composed.toTraceJSON();
            this.truncatedSkills.addAll(composed.truncatedSkills);

            if (composed.truncated) {
                Logger.w(this.getClass(), "#process - Prompt truncated - context: " + contextLimit
                        + ", budget: " + composed.inputBudgetTokens
                        + ", estimated: " + composed.estimatedInputTokens
                        + ", channel: " + this.channel.getCode());
            }

            prompt.delete(0, prompt.length());
            prompt.append(composed.content);

            // 历史记录按预算裁剪：频道历史保留最新的若干条；调用方给定列表保持旧版本的从头取用语义
            int historyLimit = Math.min(composed.historyLimit, candidateRecords.size());
            if (historyLimit < candidateRecords.size()) {
                if (null == this.histories) {
                    candidateRecords = new ArrayList<>(candidateRecords.subList(
                            candidateRecords.size() - historyLimit, candidateRecords.size()));
                }
                else {
                    candidateRecords = new ArrayList<>(candidateRecords.subList(0, historyLimit));
                }
            }

            JSONObject data = new JSONObject();
            data.put("unit", this.unit.getCapability().getName());
            data.put("content", prompt.toString());
            data.put("participant", this.participant.toCompactJSON());
            data.put("option", this.option.toJSON());

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
        final String promptText = prompt.toString();
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 更新用量
                if (null != usage) {
                    service.getStorage().updateUsage(history.queryContactId, history.unit,
                            usage.outputTokens, usage.inputTokens);

                    // 用模型单元返回的真实用量校准 Token 估算器，使后续请求的预算更贴近实际
                    service.getTokenEstimator().observe(promptText.length(), usage.inputTokens);
                }
                else {
                    List<String> tokens = calcTokens(promptText);
                    int promptTokens = tokens.size();
                    tokens = calcTokens(history.answerContent);
                    int completionTokens = tokens.size();
                    service.getStorage().updateUsage(history.queryContactId, history.unit,
                            completionTokens, promptTokens);
                }

                // 技能调用与提示词预算留痕
                writeSkillTrace(usage);

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

    /**
     * 解析本次请求生效的技能集合。
     *
     * <p>技能来源有三个，按以下顺序合并：</p>
     * <ol>
     *     <li><b>调用方显式指定</b>：请求里的 {@code categories}；</li>
     *     <li><b>关键词自动装载</b>：用用户原始请求去匹配技能声明的 {@code keywords}。
     *         只有作者显式声明了关键词的技能才会被自动装载——声明关键词即视为作者的授权；</li>
     *     <li><b>会话已绑定</b>：本会话此前启用过的技能（会话级绑定，DB 持久化，多实例一致）。</li>
     * </ol>
     *
     * <p>{@code categories} 中的绑定指令：{@code *} 清空绑定，{@code -name} 解绑。</p>
     *
     * <p>未被识别为技能的显式分类仍然收集到 {@code knowledgeCategories}，按知识释义加载，
     * 保持与旧版本一致的行为。</p>
     *
     * @param registry 技能注册表。
     * @param domain 域名。
     * @param knowledgeCategories 输出参数，收集未被识别为技能的分类名称。
     * @return 返回最终生效的技能名称列表。
     */
    protected List<String> resolveSkills(SkillRegistry registry, String domain,
                                         List<String> knowledgeCategories) {
        List<String> requested = (null != this.categories) ? this.categories : new ArrayList<String>();

        // 关键词自动装载
        List<String> auto = new ArrayList<>();
        if (this.service.isSkillAutoEnabled() && this.service.getSkillAutoLimit() > 0
                && registry.size() > 0) {
            try {
                List<SkillMeta> matched = registry.matchSkills(this.content, this.calcTokens(this.content),
                        this.service.getSkillAutoLimit(), domain);
                for (SkillMeta skill : matched) {
                    auto.add(skill.name);
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#resolveSkills - Auto match failed", e);
            }
        }
        this.autoSkills.addAll(auto);

        // 会话级绑定：显式 ∪ 自动 ∪ 本会话此前已启用
        List<String> effective = this.service.getSkillSessionStore().resolve(
                this.channel.getCode(), domain, this.channel.getAuthToken().getContactId(),
                requested, auto);
        this.boundSkills.addAll(effective);

        // 未被识别为技能的显式分类，仍按知识释义分类处理
        if (null != knowledgeCategories) {
            for (String category : requested) {
                String name = (null == category) ? null : category.trim();
                if (null == name || name.isEmpty()) {
                    continue;
                }
                if (SkillSession.DIRECTIVE_CLEAR.equals(name)
                        || name.startsWith(SkillSession.DIRECTIVE_UNBIND_PREFIX)) {
                    continue;
                }
                SkillMeta skill = registry.getSkill(name, domain);
                if (null == skill || !skill.enabled) {
                    knowledgeCategories.add(category);
                }
            }
        }

        return effective;
    }

    /**
     * 构建附件检索结果文本（「已知信息」段的一部分）。
     * 同时把附件文件登记到对话历史，保持与旧版本一致的行为。
     *
     * @return 没有附件或没有检索到内容时返回 {@code null}。
     */
    protected String buildAttachmentText() {
        if (null == this.attachments) {
            return null;
        }

        List<FileLabel> fileList = new ArrayList<>();
        for (Attachment attachment : this.attachments) {
            if (attachment.getType().equals(FileAttachment.TYPE)) {
                FileAttachment fileAttachment = (FileAttachment) attachment;
                if (null == this.history.queryFileLabels) {
                    this.history.queryFileLabels = new ArrayList<>();
                }
                this.history.queryFileLabels.add(fileAttachment.fileLabel);
                fileList.add(fileAttachment.fileLabel);
            }
        }

        if (fileList.isEmpty()) {
            return null;
        }

        List<RetrieveReRankResult> retrieveReRankList = analyseFiles(fileList, this.content);
        List<RetrieveReRankResult.Answer> answerList = new ArrayList<>();
        for (RetrieveReRankResult rrr : retrieveReRankList) {
            answerList.addAll(rrr.getAnswerList());
        }
        // 按照得分从高到低
        answerList.sort(new Comparator<RetrieveReRankResult.Answer>() {
            @Override
            public int compare(RetrieveReRankResult.Answer a1, RetrieveReRankResult.Answer a2) {
                return (int) Math.round((a2.score - a1.score) * 100);
            }
        });

        StringBuilder buf = new StringBuilder();
        for (RetrieveReRankResult.Answer answer : answerList) {
            buf.append(answer.content).append("\n");
        }

        return (buf.length() > 0) ? buf.toString().trim() : null;
    }

    /**
     * 构建知识释义文本（「已知信息」段的一部分）。
     *
     * <p>旧版本把知识释义包装成历史对话记录，导致「指令 / 模板 / 问题」三层嵌套、指令与数据难区分。
     * 这里改为明确的「已知信息」数据区。</p>
     *
     * @param knowledgeCategories 知识释义分类名称。
     * @return 没有释义时返回 {@code null}。
     */
    protected String buildRetrievalText(List<String> knowledgeCategories) {
        if (null == knowledgeCategories || knowledgeCategories.isEmpty()) {
            return null;
        }

        final int limit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());
        StringBuilder buf = new StringBuilder();
        for (String category : knowledgeCategories) {
            List<KnowledgeParaphrase> list = this.service.getStorage().readKnowledgeParaphrases(category);
            for (KnowledgeParaphrase paraphrase : list) {
                if (buf.length() >= limit) {
                    break;
                }
                buf.append("- ").append(paraphrase.getWord())
                        .append("：").append(paraphrase.getParaphrase()).append("\n");
            }
            if (buf.length() >= limit) {
                break;
            }
        }

        return (buf.length() > 0) ? buf.toString().trim() : null;
    }

    /**
     * 收集历史候选记录。长度（Token）裁剪统一由 {@link PromptComposer} 完成。
     *
     * <p>候选范围与旧版本保持一致：</p>
     * <ul>
     *     <li>频道历史：取最近 {@code maxHistories} 条（{@code getLastHistory} 返回时间正序）；</li>
     *     <li>调用方给定列表：跳过携带附件/附加内容的记录，从头取用，上限 {@code recommendHistories} 条。</li>
     * </ul>
     *
     * @param recommendHistories 调用方给定列表的条数上限。
     * @return 返回候选记录列表。
     */
    protected List<GeneratingRecord> collectHistoryCandidates(int recommendHistories) {
        List<GeneratingRecord> candidates = new ArrayList<>();

        if (null == this.histories) {
            if (this.maxHistories > 0) {
                candidates.addAll(this.channel.getLastHistory(this.maxHistories));
            }
        }
        else {
            for (int i = 0; i < this.histories.size(); ++i) {
                GeneratingRecord record = this.histories.get(i);
                if (record.hasQueryFile() || record.hasQueryAddition()) {
                    // 为了兼容旧版本，排除掉附件类型
                    continue;
                }
                candidates.add(record);
                if (candidates.size() >= recommendHistories) {
                    break;
                }
            }
        }

        return candidates;
    }

    /**
     * 拼接两段文本，跳过空值。
     */
    protected static String joinText(String first, String second) {
        boolean hasFirst = (null != first && !first.trim().isEmpty());
        boolean hasSecond = (null != second && !second.trim().isEmpty());

        if (hasFirst && hasSecond) {
            return first.trim() + "\n\n" + second.trim();
        }
        if (hasFirst) {
            return first.trim();
        }
        if (hasSecond) {
            return second.trim();
        }
        return null;
    }

    /**
     * 记录技能调用与提示词预算留痕，落库到 {@code aigc_skill_invocation}（与对话历史通过 {@code sn} 关联）。
     *
     * <p>只要本次请求涉及技能（注入、自动装载、被截断）或发生了提示词截断就会写下一条记录，
     * 便于回答「模型为什么没看到某个技能」；纯粹的普通对话不产生记录，避免留痕表膨胀。</p>
     *
     * <p>调用方已处于异步执行上下文中，故本方法同步写库。</p>
     *
     * @param usage 模型单元返回的用量，可为 {@code null}。
     */
    protected void writeSkillTrace(Usage usage) {
        boolean skillsInvolved = !this.injectedSkills.isEmpty() || !this.truncatedSkills.isEmpty()
                || !this.autoSkills.isEmpty() || !this.boundSkills.isEmpty();
        boolean truncated = (null != this.promptTrace) && this.promptTrace.optBoolean("truncated", false);
        if (!skillsInvolved && !truncated) {
            return;
        }

        JSONObject trace = new JSONObject();
        trace.put("sn", this.sn);
        trace.put("channel", this.channel.getCode());
        trace.put("unit", this.unit.getCapability().getName());
        trace.put("domain", this.channel.getAuthToken().getDomain());
        trace.put("contactId", this.channel.getAuthToken().getContactId());

        JSONArray injected = toJSONArray(this.injectedSkills);
        trace.put("skills", injected);

        JSONObject detail = new JSONObject();
        detail.put("injected", injected);
        detail.put("truncated", toJSONArray(this.truncatedSkills));
        detail.put("auto", toJSONArray(this.autoSkills));
        detail.put("effective", toJSONArray(this.boundSkills));
        if (null != this.promptTrace) {
            detail.put("prompt", this.promptTrace);
        }
        trace.put("detail", detail);

        JSONObject budget = new JSONObject();
        if (null != this.promptTrace && this.promptTrace.has("budget")) {
            JSONObject source = this.promptTrace.getJSONObject("budget");
            budget.put("context", source.optInt("context"));
            budget.put("outputReserve", source.optInt("outputReserve"));
            budget.put("inputBudget", source.optInt("inputBudget"));
            budget.put("estimatedInput", source.optInt("estimatedInput"));
        }
        budget.put("charsPerToken", this.service.getTokenEstimator().getCharsPerToken());
        if (null != usage) {
            budget.put("actualInput", usage.inputTokens);
            budget.put("actualOutput", usage.outputTokens);
            budget.put("model", usage.model);
        }
        trace.put("budget", budget);
        trace.put("timestamp", System.currentTimeMillis());

        try {
            if (null != this.service.getStorage()) {
                this.service.getStorage().writeSkillInvocation(trace);
            }
        } catch (Exception e) {
            Logger.w(GenerateTextUnitMeta.class, "#writeSkillTrace", e);
        }
    }

    private static JSONArray toJSONArray(List<String> list) {
        JSONArray array = new JSONArray();
        for (String item : list) {
            array.put(item);
        }
        return array;
    }
}
