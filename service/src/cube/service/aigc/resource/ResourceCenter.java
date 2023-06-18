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

import cube.common.entity.ComplexContext;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ExtractKeywordsListener;

import java.util.List;

/**
 * 资源中心。
 */
public class ResourceCenter {

    private AIGCService service;

    private final static ResourceCenter instance = new ResourceCenter();

    public final static ResourceCenter getInstance() {
        return ResourceCenter.instance;
    }

    private ResourceCenter() {
    }

    public void setService(AIGCService service) {
        this.service = service;
    }

    public void cacheComplexContext(ComplexContext context) {

    }

    public SearchResult search(String query, String answer, ComplexContext context) {
        final SearchResult result = new SearchResult();
        // 提取问句的关键词
        this.service.extractKeywords(query, new ExtractKeywordsListener() {
            @Override
            public void onCompleted(String text, List<String> words) {

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

        synchronized (result) {
            try {
                result.wait(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
