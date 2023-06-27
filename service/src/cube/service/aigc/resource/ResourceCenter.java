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

package cube.service.aigc.resource;

import cell.util.log.Logger;
import cube.aigc.atom.Molecule;
import cube.auth.AuthToken;
import cube.common.entity.ChartReaction;
import cube.common.entity.ChartSeries;
import cube.common.entity.ComplexContext;
import cube.common.entity.SearchResult;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ExtractKeywordsListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源中心。
 */
public class ResourceCenter {

    private final static ResourceCenter instance = new ResourceCenter();

    private AIGCService service;

    private Map<Long, ComplexContext> complexContextMap;

    /**
     * Key：Contact ID
     */
    private Map<Long, LinkedList<SearchResult>> contactSearchResults;

    /**
     * 缓存的超时时间。
     */
    private long cacheTimeout = 24 * 60 * 60 * 1000;

    private final String[] chartKeywords = new String[] {
            "图表", "统计图", "曲线图", "线图", "折线图", "柱图", "柱状图", "柱形图",
            "饼图", "饼状图", "饼形图", "圆瓣图", "环形图"
    };

    public final static ResourceCenter getInstance() {
        return ResourceCenter.instance;
    }

    private ResourceCenter() {
        this.complexContextMap = new ConcurrentHashMap<>();
        this.contactSearchResults = new ConcurrentHashMap<>();
    }

    public void setService(AIGCService service) {
        this.service = service;
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

    public ChartSeries matchChartSeries(List<String> words) {
        boolean hit = false;
        for (String word : words) {
            for (String chartName : this.chartKeywords) {
                if (word.equals(chartName)) {
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

    private class ChartReactionWrap {

        protected ChartReaction reaction;

        protected int matchingNum;

        protected ChartReactionWrap(ChartReaction reaction, int matchingNum) {
            this.reaction = reaction;
            this.matchingNum = matchingNum;
        }
    }
}
