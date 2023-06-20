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
import cube.auth.AuthToken;
import cube.common.entity.ComplexContext;
import cube.common.entity.SearchResult;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ExtractKeywordsListener;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    public void onTick(long now) {
        this.complexContextMap.entrySet().removeIf(e -> now - e.getValue().getTimestamp() > this.cacheTimeout);

        Iterator<Map.Entry<Long, LinkedList<SearchResult>>> iter = this.contactSearchResults.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, LinkedList<SearchResult>> e = iter.next();
            LinkedList<SearchResult> list = e.getValue();
            list.removeIf(v -> now - v.created > this.cacheTimeout);
        }
    }
}
