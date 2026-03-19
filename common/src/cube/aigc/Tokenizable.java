/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc;

import java.util.List;

public interface Tokenizable {

    /**
     * 执行分词。
     *
     * @param text
     * @return
     */
    List<String> segment(String text);

    /**
     * 分析关键词。
     *
     * @param text
     * @param topN
     * @return
     */
    List<String> analyze(String text, int topN);
}
