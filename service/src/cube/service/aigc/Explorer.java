/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.aigc;

import cell.util.log.Logger;
import cube.aigc.attachment.Attachment;
import cube.aigc.attachment.ThingAttachment;
import cube.aigc.attachment.ui.Button;
import cube.aigc.attachment.ui.ButtonListener;
import cube.aigc.attachment.ui.Event;
import cube.aigc.attachment.ui.EventResult;
import cube.auth.AuthToken;
import cube.common.entity.*;
import cube.service.aigc.listener.ExtractKeywordsListener;
import cube.service.aigc.module.ModuleManager;
import cube.service.aigc.module.PublicOpinion;
import cube.service.aigc.module.Stage;
import cube.service.aigc.resource.AtomCollider;
import cube.service.aigc.resource.AttachmentBuilder;
import cube.service.aigc.resource.BaiduSearcher;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源中心。
 */
public class Explorer {

    private final static Explorer instance = new Explorer();

    private final String[] chartKeywords = new String[] {
            "数据", "报告",
            "图表", "统计图", "曲线图", "线图", "折线图", "柱图", "柱状图", "柱形图",
            "饼图", "饼状图", "饼形图", "圆瓣图", "环形图"
    };

    private AIGCService service;

    private Tokenizer tokenizer;

    private Map<Long, ComplexContext> complexContextMap;

    /**
     * Key：Contact ID
     */
    private Map<Long, LinkedList<SearchResult>> contactSearchResults;

    /**
     * 缓存的超时时间。
     */
    private long cacheTimeout = 24 * 60 * 60 * 1000;

    /**
     * Key：Resource SN
     */
    private Map<Long, AttachmentResource> attachmentResourceMap;

    public final static Explorer getInstance() {
        return Explorer.instance;
    }

    private Explorer() {
        this.complexContextMap = new ConcurrentHashMap<>();
        this.contactSearchResults = new ConcurrentHashMap<>();
        this.attachmentResourceMap = new ConcurrentHashMap<>();
    }

    public void setup(AIGCService service, Tokenizer tokenizer) {
        this.service = service;
        this.tokenizer = tokenizer;

        ModuleManager.getInstance().addModule(new PublicOpinion());
        ModuleManager.getInstance().start();
    }

    public void teardown() {
        ModuleManager.getInstance().stop();
    }

    public void cacheComplexContext(ComplexContext context) {
        this.complexContextMap.put(context.getId(), context);
    }

    public SearchResult search(String query, String answer, ComplexContext context) {
        final SearchResult result = new SearchResult();
        // 提取问句的关键词
        boolean success = this.service.extractKeywords(query, new ExtractKeywordsListener() {
            @Override
            public void onCompleted(String text, List<String> words) {
                result.keywords = words;

                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed() {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success && null == result.keywords) {
            synchronized (result) {
                try {
                    result.wait(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (null == result.keywords) {
            Logger.i(this.getClass(), "#search - Keywords is empty");
            return result;
        }

        BaiduSearcher searcher = new BaiduSearcher(this.service);
        if (searcher.search(result.keywords)) {
            // 搜索关键词
            searcher.fillSearchResult(result);
        }

        return result;
    }

    public void cacheSearchResult(AuthToken authToken, SearchResult result) {
        result.authToken = authToken;
        synchronized (this.contactSearchResults) {
            LinkedList<SearchResult> list = this.contactSearchResults.computeIfAbsent(authToken.getContactId(), k -> new LinkedList<>());
            list.addFirst(result);
        }
    }

    public List<SearchResult> querySearchResults(AuthToken token) {
        LinkedList<SearchResult> result = new LinkedList<>();
        LinkedList<SearchResult> list = this.contactSearchResults.get(token.getContactId());
        if (null != list) {
            for (SearchResult sr : list) {
                if (!sr.popup) {
                    sr.popup = true;
                    result.add(sr);
                }
            }
        }
        return result;
    }

    public Stage infer(String content) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<Keyword> keywordList = analyzer.analyze(content, 10);
        List<String> words = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            words.add(keyword.getWord());
        }

        Stage stage = new Stage(words);

        ChartInference chartInference = this.inferChart(words);
        if (null != chartInference) {
            stage.chartResources.addAll(chartInference.chartResources);
            stage.attachmentResources.addAll(chartInference.attachmentResources);
        }

        // 判断上下文是否需要进行推算
        boolean inference = false;
        if (!this.hitChartsKeywords(words.get(0))
                && !this.hitChartsKeywords(words.get(1))) {
            // 前2个关键词都没有图表相关词，进行推理
            inference = true;
        }

        stage.inference = inference;

        return stage;
    }

    private ChartInference inferChart(List<String> words) {
        boolean hit = false;
        for (String word : words) {
            for (String chartName : this.chartKeywords) {
                if (chartName.equals(word)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                break;
            }
        }

        if (!hit) {
            // 没有命中关键词
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#inferChart - No key words hit：" + words.get(0));
            }
            return null;
        }

        // 先尝试从 Atom 库里提取数据
        AtomCollider collider = new AtomCollider(this.service.getStorage());
        collider.collapse(words);

        ChartInference chartInference = new ChartInference();

        if (!collider.chartSeriesList.isEmpty()) {
            /*
            // 判断上下文是否需要进行推算
            boolean inference = false;
            if (!this.hitChartsKeywords(words.get(0))
                    && !this.hitChartsKeywords(words.get(1))) {
                 前2个关键词都没有图表相关词，进行推理
                inference = true;
            }
            if (inference) {
                this.records = null;
                this.histories = 0;
                ResourceAnswer answer = new ResourceAnswer(complexContext);
                String question = answer.ask(this.content);
                data.put("content", question);
            }
            */

            for (ChartSeries chartSeries : collider.chartSeriesList) {
                // 创建资源
                ChartResource resource = new ChartResource(chartSeries.desc, chartSeries);
                chartInference.chartResources.add(resource);
            }
        }
        else {
            if (null != collider.recommendWord) {
                // 有推荐数据
                AttachmentBuilder builder = new AttachmentBuilder();

                String label = "";
                if (collider.recommendMonth > 0) {
                    label = "查看" + collider.recommendYear + "年" + collider.recommendMonth + "月数据";
                }
                else {
                    label = "查看" + collider.recommendYear + "年数据";
                }

                ThingAttachment attachment = builder.buildThing(collider.recommendWord,
                        new Button(label, new ButtonListener() {
                    @Override
                    public void onClick(Event event) {
                        AtomCollider collider = (AtomCollider) event.target.getContext();
                        ChartSeries chartSeries = matchChartSeries(collider.labelList,
                                collider.recommendYear,
                                collider.recommendMonth > 0 ? collider.recommendMonth : collider.month,
                                collider.date);
                        if (null != chartSeries) {
                            ChartResource resource = new ChartResource(chartSeries.desc, chartSeries);
                            event.finish(resource);
                        }
                    }
                }, collider));
                AttachmentResource resource = new AttachmentResource(attachment);
                chartInference.attachmentResources.add(resource);

                // 缓存附件资源
                this.attachmentResourceMap.put(resource.sn, resource);
            }
        }

        return chartInference;
    }

    private boolean hitChartsKeywords(String word) {
        for (String chartName : this.chartKeywords) {
            if (chartName.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private ChartSeries matchChartSeries(List<String> labels, int year, int month, int date) {
        AtomCollider collider = new AtomCollider(this.service.getStorage());
        collider.collapse(labels, year, month, date);
        if (collider.chartSeriesList.isEmpty()) {
            return null;
        }

        return collider.chartSeriesList.get(0);
    }

    public EventResult fireEvent(Event event) {
        AttachmentResource resource = this.attachmentResourceMap.get(event.resourceSn);
        if (null == resource) {
            Logger.w(this.getClass(), "#fireEvent - Can NOT find attachment resource: " + event.resourceSn);
            return null;
        }

        // 绑定资源
        event.resource = resource;

        Attachment attachment = resource.getAttachment(event.attachmentId);
        if (null == attachment) {
            Logger.w(this.getClass(), "#fireEvent - Can NOT find attachment: " + event.attachmentId);
            return null;
        }

        if (attachment instanceof ThingAttachment) {
            ThingAttachment thing = (ThingAttachment) attachment;

            // 绑定附件
            event.attachment = thing;

            Button button = thing.getActionButton(event.componentId);
            if (null == button) {
                Logger.w(this.getClass(), "#fireEvent - Can NOT find button in attachment: " + event.componentId);
                return null;
            }

            // 绑定目标
            event.target = button;

            ButtonListener listener = button.getListener();
            if (null == listener) {
                Logger.w(this.getClass(), "#fireEvent - The button has not event listener: " + event.componentId);
                return null;
            }

            if (event.name.equals("click")) {
                // 触发事件
                listener.onClick(event);

                EventResult result = new EventResult(event);
                return result;
            }
            else {
                Logger.w(this.getClass(), "#fireEvent - Unknown event: " + event.name);
            }
        }
        else {
            Logger.w(this.getClass(), "#fireEvent - Attachment instance type error");
        }

        return null;
    }

    /*
    private ChartSeries matchChartSeries(List<String> words) {
        boolean hit = false;
        for (String word : words) {
            for (String chartName : this.chartKeywords) {
                if (chartName.equals(word)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                break;
            }
        }

        if (!hit) {
            // 没有命中关键词
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#matchChartSeries - No key words hit：" + words.get(0));
            }
            return null;
        }

        // 先尝试从 Atom 库里提取数据
        AtomCollider atomCollider = new AtomCollider(this.service.getStorage());
        ChartSeries chartSeries = atomCollider.collapse(words);
        if (null != chartSeries) {
            return chartSeries;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#matchChartSeries - Can NOT find data series in atom system: "
                    + words.get(0));
        }

        // 词必须和 Chart Reaction 的 primary 匹配，然后匹配余下词
        List<ChartReaction> reactions = new ArrayList<>();
        for (String word : words) {
            List<ChartReaction> list = this.service.getStorage().readChartReactions(word,
                    null, null, null);
            for (ChartReaction cr : list) {
                if (!reactions.contains(cr)) {
                    reactions.add(cr);
                }
            }
        }

        // primary 没有匹配的就不再匹配
        if (reactions.isEmpty()) {
            return null;
        }

        List<ChartReactionWrap> wrapList = new ArrayList<>();
        ChartReactionWrap mostMatching = null;
        for (ChartReaction cr : reactions) {
            // 当前的 Reaction 匹配的关键字数量
            int num = cr.matchWordNum(words);
            ChartReactionWrap wrap = new ChartReactionWrap(cr, num);

            if (null == mostMatching) {
                mostMatching = wrap;
            }
            else {
                if (wrap.matchingNum > mostMatching.matchingNum) {
                    mostMatching = wrap;
                }
            }

            wrapList.add(wrap);
        }

        // 如果匹配关键词数量少于2，则不匹配
        if (mostMatching.matchingNum < 2) {
            return null;
        }

        chartSeries = this.service.getStorage().readLastChartSeries(mostMatching.reaction.seriesName);
        return chartSeries;
    }*/

    public void onTick(long now) {
        this.complexContextMap.entrySet().removeIf(e -> now - e.getValue().getTimestamp() > this.cacheTimeout);

        Iterator<Map.Entry<Long, LinkedList<SearchResult>>> iter = this.contactSearchResults.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, LinkedList<SearchResult>> e = iter.next();
            LinkedList<SearchResult> list = e.getValue();
            list.removeIf(v -> now - v.created > this.cacheTimeout);
        }
    }


    private class ChartInference {

        protected List<ChartResource> chartResources = new ArrayList<>();

        protected List<AttachmentResource> attachmentResources = new ArrayList<>();

        protected ChartInference() {
        }
    }

    private class ChartReactionWrap {

        protected ChartReaction reaction;

        protected int matchingNum;

        protected ChartReactionWrap(ChartReaction reaction, int matchingNum) {
            this.reaction = reaction;
            this.matchingNum = matchingNum;
        }
    }
}
