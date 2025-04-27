/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.util.log.Logger;
import cube.aigc.Flowable;
import cube.aigc.Module;
import cube.aigc.Page;
import cube.aigc.Stage;
import cube.aigc.attachment.Attachment;
import cube.aigc.attachment.ThingAttachment;
import cube.aigc.attachment.ui.Button;
import cube.aigc.attachment.ui.ButtonListener;
import cube.aigc.attachment.ui.Event;
import cube.aigc.attachment.ui.EventResult;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.listener.ExtractKeywordsListener;
import cube.service.aigc.listener.ReadPageListener;
import cube.service.aigc.module.AppManager;
import cube.service.aigc.module.ModuleManager;
import cube.service.aigc.resource.BaiduSearcher;
import cube.service.aigc.resource.BingSearcher;
import cube.service.aigc.resource.ResourceSearcher;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * 可被忽略的 URL ，这些 URL 不宜通过一般爬虫爬取数据。
     */
    private final String[] ignorableUrls = new String[] {
            "zhihu.com"
    };

    private AIGCService service;

    private Tokenizer tokenizer;

    private String pageReaderUrl;

    private String searcherName = "baidu";

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

    /**
     * 页面读取任务队列。
     */
    private Queue<PageReaderTask> pageReaderTaskQueue;

    /**
     * 任务计数器。
     */
    private AtomicInteger pageReaderTaskCount;

    private final int maxPageReaders = 5;

    public final static Explorer getInstance() {
        return Explorer.instance;
    }

    private Explorer() {
        this.complexContextMap = new ConcurrentHashMap<>();
        this.contactSearchResults = new ConcurrentHashMap<>();
        this.attachmentResourceMap = new ConcurrentHashMap<>();
        this.pageReaderTaskQueue = new ConcurrentLinkedQueue<>();
        this.pageReaderTaskCount = new AtomicInteger(0);
    }

    public void setup(AIGCService service, Tokenizer tokenizer) {
        this.service = service;
        this.tokenizer = tokenizer;

        ModuleManager.getInstance().addModule(new AppManager(service));
        ModuleManager.getInstance().start();

        (new Thread() {
            @Override
            public void run() {
                buildInData((AuthService) service.getKernel().getModule(AuthService.NAME));
            }
        }).start();
    }

    public void teardown() {
        ModuleManager.getInstance().stop();
    }

    public void config(String pageReaderUrl, String searcherName) {
        this.pageReaderUrl = pageReaderUrl;
        this.searcherName = searcherName;
    }

    private void buildInData(AuthService authService) {
        // 创建单元用户
        long id = 200101;
        for (int i = 0; i < 10; ++i) {
            Contact contact = new Contact(id + i, AuthConsts.DEFAULT_DOMAIN, "Unit-" + (id + i));
            ContactManager.getInstance().newContact(contact);

            // 授权码
            if (null == authService.getToken(AuthConsts.DEFAULT_DOMAIN, contact.getId())) {
                authService.applyToken(AuthConsts.DEFAULT_DOMAIN, AuthConsts.DEFAULT_APP_KEY,
                        contact.getId(), 10L * 365 * 24 * 60 * 60 * 1000);
            }
        }

        // 创建 API 用户
        id = 102001;
        for (int i = 0; i < 10; ++i) {
            Contact contact = new Contact(id + i, AuthConsts.DEFAULT_DOMAIN, "API-" + (id + i));
            ContactManager.getInstance().newContact(contact);

            // 授权码
            if (null == authService.getToken(AuthConsts.DEFAULT_DOMAIN, contact.getId())) {
                authService.applyToken(AuthConsts.DEFAULT_DOMAIN, AuthConsts.DEFAULT_APP_KEY,
                        contact.getId(), 10L * 365 * 24 * 60 * 60 * 1000);
            }
        }
    }

    public String getPageReaderUrl() {
        return this.pageReaderUrl;
    }

    public String getSearcherName() {
        return this.searcherName;
    }

    /**
     * 是否是可被忽略的 URL 。
     *
     * @param url
     * @return
     */
    public boolean isIgnorableUrl(String url) {
        for (String u : this.ignorableUrls) {
            if (url.contains(u)) {
                return true;
            }
        }
        return false;
    }

    public void cacheComplexContext(ComplexContext context) {
        this.complexContextMap.put(context.getId(), context);
    }

    public ComplexContext getComplexContext(long id) {
        return this.complexContextMap.get(id);
    }

    /**
     * 使用爬虫节点进行搜索。
     *
     * @param query
     * @param authToken
     * @return
     */
    public SearchResult search(String query, AuthToken authToken) {
        final SearchResult result = new SearchResult(query);
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
            public void onFailed(String text, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        synchronized (result) {
            if (success && null == result.keywords) {
                try {
                    result.wait(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (null == result.keywords || result.keywords.isEmpty()) {
            // 使用 TFIDF 提取关键字
            TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
            List<Keyword> keywords = analyzer.analyze(query, 5);
            if (!keywords.isEmpty()) {
                List<String> words = new ArrayList<>();
                for (Keyword keyword : keywords) {
                    words.add(keyword.getWord());
                }
                result.keywords = words;
            }

            if (null == result.keywords || result.keywords.isEmpty()) {
                result.keywords = new ArrayList<>();
                result.keywords.add(query);
                Logger.i(this.getClass(), "#search - Keywords is empty, use query text as keywords");
            }
        }

        // 构建关键词
        List<String> keywords = new ArrayList<>();
        if (query.length() > 30) {
            keywords.addAll(result.keywords);
        }
        else {
            keywords.add(query);
        }

        ResourceSearcher searcher = null;
        if (this.searcherName.equalsIgnoreCase("baidu")) {
            keywords.add(result.keywords.get(0));
            searcher = new BaiduSearcher(this.service);
        }
        else {
            searcher = new BingSearcher(this.service);
        }

        if (searcher.search(keywords)) {
            // 搜索关键词
            searcher.fillSearchResult(result);
        }

        if (result.hasResult()) {
            // 缓存结果，以便客户端读取数据
            this.cacheSearchResult(authToken, result);
        }
        else {
            Logger.w(this.getClass(), "Search result is null - cid:" + authToken.getContactId());
        }

        return result;
    }

    private void cacheSearchResult(AuthToken authToken, SearchResult result) {
        result.authToken = authToken;
        synchronized (this.contactSearchResults) {
            LinkedList<SearchResult> list = this.contactSearchResults.computeIfAbsent(authToken.getContactId(), k -> new LinkedList<>());
            list.addFirst(result);
            Logger.d(this.getClass(), "#cacheSearchResult - Cache search result for " + result.authToken.getContactId());
        }
    }

    public List<SearchResult> querySearchResults(AuthToken token) {
        LinkedList<SearchResult> result = new LinkedList<>();
        synchronized (this.contactSearchResults) {
            LinkedList<SearchResult> list = this.contactSearchResults.get(token.getContactId());
            if (null != list) {
                for (SearchResult sr : list) {
                    if (!sr.popup) {
                        sr.popup = true;
                        result.add(sr);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 读取指定页面内容。
     *
     * @param url
     * @param listener
     */
    public void readPageContent(String url, ReadPageListener listener) {
        if (null == this.pageReaderUrl) {
            listener.onCompleted(url, null);
            return;
        }

        this.pageReaderTaskQueue.offer(new PageReaderTask(url, listener));

        if (this.pageReaderTaskCount.get() < this.maxPageReaders) {
            this.pageReaderTaskCount.incrementAndGet();

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    PageReaderTask task = pageReaderTaskQueue.poll();

                    while (null != task) {
                        task.run();

                        task = pageReaderTaskQueue.poll();
                    }

                    pageReaderTaskCount.decrementAndGet();
                }
            })).start();
        }
    }

    /**
     * 尝试匹配场景数据。
     *
     * @param content
     * @return
     */
    public Stage perform(AuthToken authToken, String content) {
        Stage stage = new Stage(authToken);

        for (Module mod : ModuleManager.getInstance().getModuleList()) {
            Flowable flowable = mod.match(content);
            if (null != flowable) {
                stage.module = mod;
                stage.flowable = flowable;
                break;
            }
        }

        return stage;

        /*TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<Keyword> keywordList = analyzer.analyze(content, 10);
        List<String> words = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            words.add(keyword.getWord());
        }

        if (words.isEmpty()) {
            // 没有词
            words.add(content);
            Stage stage = new Stage(words);
            stage.inference = false;
            return stage;
        }

        Stage stage = new Stage(words);

        try {
            ChartInference chartInference = this.inferChart(words);
            if (null != chartInference) {
                stage.chartResources.addAll(chartInference.chartResources);
                stage.attachmentResources.addAll(chartInference.attachmentResources);
            }

            // 判断上下文是否需要进行推算
            boolean inference = false;
            if (words.size() >= 2 && !this.hitChartsKeywords(words.get(0))
                    && !this.hitChartsKeywords(words.get(1))) {
                // 前2个关键词都没有图表相关词，进行推理
                inference = true;
            }

            stage.inference = inference;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#infer", e);
        }

        return stage;*/
    }

    /*private ChartInference inferChart(List<String> words) {
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

        if (!collider.chartList.isEmpty()) {

            for (Chart chart : collider.chartList) {
                // 创建资源
                ChartResource resource = new ChartResource(chart.desc, chart);
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
                        Chart chart = matchChartSeries(collider.labelList,
                                collider.recommendYear,
                                collider.recommendMonth > 0 ? collider.recommendMonth : collider.month,
                                collider.date);
                        if (null != chart) {
                            ChartResource resource = new ChartResource(chart.desc, chart);
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
    }*/

    /*private boolean hitChartsKeywords(String word) {
        for (String chartName : this.chartKeywords) {
            if (chartName.equals(word)) {
                return true;
            }
        }
        return false;
    }*/

    /*private Chart matchChartSeries(List<String> labels, int year, int month, int date) {
        AtomCollider collider = new AtomCollider(this.service.getStorage());
        collider.collapse(labels, year, month, date);
        if (collider.chartList.isEmpty()) {
            return null;
        }

        return collider.chartList.get(0);
    }*/

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


    private class PageReaderTask implements Runnable {

        protected String url;

        protected ReadPageListener listener;

        protected PageReaderTask(String url, ReadPageListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        public void run() {
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#run - Request " + this.url);
            }

            if (HttpStatus.OK_200 != this.execute(true)) {
                // 重试
                this.execute(false);
            }
        }

        private int execute(boolean retry) {
            HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();
            try {
                JSONObject requestParam = new JSONObject();
                requestParam.put("url", this.url);
                StringContentProvider provider = new StringContentProvider(requestParam.toString());
                ContentResponse response = client.POST(pageReaderUrl).timeout(1, TimeUnit.MINUTES)
                        .header("Content-Type", "application/json")
                        .content(provider).send();
                if (response.getStatus() == HttpStatus.OK_200) {
                    JSONObject responseData = new JSONObject(response.getContentAsString());
                    if (responseData.getInt("code") == 200) {
                        Page page = new Page(responseData);
                        // 过滤短文本
                        page.filterShortText();
                        this.listener.onCompleted(this.url, page);
                    }
                    else {
                        this.listener.onCompleted(this.url, null);
                        if (Logger.isDebugLevel()) {
                            Logger.d(this.getClass(), "#execute - Interface return error state: "
                                    + responseData.getInt("code"));
                        }
                    }
                }
                else {
                    Logger.w(this.getClass(), "#execute - Reader response error: " + response.getStatus());
                    if (!retry) {
                        this.listener.onCompleted(this.url, null);
                    }
                }

                return response.getStatus();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#execute", e);
                if (!retry) {
                    this.listener.onCompleted(this.url, null);
                }
                return HttpStatus.INTERNAL_SERVER_ERROR_500;
            } finally {
                HttpClientFactory.getInstance().returnHttpClient(client);
            }
        }
    }
}
