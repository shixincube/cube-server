/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.resource;

import cube.common.entity.SearchResult;
import cube.service.aigc.AIGCService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 资源搜索器。
 */
public abstract class ResourceSearcher {

    protected final AIGCService service;

    public ResourceSearcher(AIGCService service) {
        this.service = service;
    }

    /**
     * 执行搜索。
     *
     * @param words
     * @return 返回是否执行成功。
     */
    public abstract boolean search(List<String> words);

    /**
     * 填充搜索结果。
     *
     * @param searchResult
     */
    public abstract void fillSearchResult(SearchResult searchResult);

    /**
     * 创建搜索链接。
     *
     * @param words
     * @return
     */
    protected abstract String makeURL(List<String> words);
}
