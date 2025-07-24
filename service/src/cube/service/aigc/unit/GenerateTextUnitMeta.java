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
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.attachment.FileAttachment;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.Explorer;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.ReadPageListener;
import cube.service.aigc.resource.Agent;
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

            // 提示词长度限制
            int lengthLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());
            lengthLimit -= this.content.length();

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
                Collections.sort(answerList, new Comparator<RetrieveReRankResult.Answer>() {
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
            if (null != this.categories && !this.categories.isEmpty()) {
                this.fillRecords(candidateRecords, this.categories, lengthLimit - lengthCount,
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
                        responseText, "", complexContext);
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
                        responseText, "", complexContext);
            }
            else if (this.service.useAgent) {
                GeneratingRecord generatingRecord =
                        Agent.getInstance().generateText(channel.getCode(), this.unit.getCapability().getName(),
                                this.content, new GeneratingOption(), this.histories);
                if (null != generatingRecord) {
                    // 过滤中文字符
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content,
                            generatingRecord.answer, generatingRecord.thought, complexContext);
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
                try {
                    responseText = payload.getString("response");
                    thoughtText = payload.getString("thought");
                } catch (Exception e) {
                    Logger.w(AIGCService.class, "Unit respond failed - channel: " + this.channel.getCode());
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
                        responseText.trim(), thoughtText.trim(), complexContext);
            }
        }
        else {
            // 进入舞台流程
            if (null != complexContext.stage) {
                GeneratingRecord record = complexContext.stage.perform();
                result = this.channel.appendRecord(this.sn, record);
            }
            else {
                ResourceAnswer resourceAnswer = new ResourceAnswer(complexContext);
                // 提取内容
                String content = resourceAnswer.extractContent(this.service, this.channel.getAuthToken());
                String answer = resourceAnswer.answer(content);
                result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        (null != this.originalQuery) ? this.originalQuery : this.content, answer.trim(), "",
                        complexContext);
            }
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

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 更新用量
                List<String> tokens = calcTokens(prompt.toString());
                long promptTokens = tokens.size();
                tokens = calcTokens(history.answerContent);
                long completionTokens = tokens.size();
                service.getStorage().updateUsage(history.queryContactId, ModelConfig.getModelByUnit(history.unit),
                        completionTokens, promptTokens);

                // 保存历史记录
                if (recordHistoryEnabled) {
                    service.getStorage().writeHistory(history);
                }
            }
        });
    }

    protected String filterChinese(AIGCUnit unit, String text) {
        if (unit.getCapability().getName().equalsIgnoreCase(ModelConfig.CHAT_UNIT)) {
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
                GeneratingRecord answer = this.service.syncGenerateText(this.channel.getAuthToken(), ModelConfig.BAIZE_X_UNIT, prompt,
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
}
