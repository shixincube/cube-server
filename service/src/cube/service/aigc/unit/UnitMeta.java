/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.RetrieveReRankResult;
import cube.service.aigc.AIGCService;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class UnitMeta {

    protected final AIGCService service;

    public final AIGCUnit unit;

    public UnitMeta(AIGCService service, AIGCUnit unit) {
        this.service = service;
        this.unit = unit;
    }

    protected List<RetrieveReRankResult> analyseFiles(List<FileLabel> fileLabels, String query) {
        List<RetrieveReRankResult> result = new ArrayList<>();
        if (fileLabels.isEmpty()) {
            return result;
        }

        return this.service.syncRetrieveReRank(fileLabels, query);
    }

    /**
     * 计算文本的 Token 列表。
     *
     * @param text
     * @return
     */
    public List<String> calcTokens(String text) {
        List<String> tokens = this.service.getTokenizer().sentenceProcess(text);
        tokens.removeIf(s -> !TextUtils.isChineseWord(s) && !TextUtils.isWord(s));
        return tokens;
    }

    public abstract void process();
}
