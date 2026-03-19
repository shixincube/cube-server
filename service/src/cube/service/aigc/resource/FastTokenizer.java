/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.resource;

import cube.aigc.Tokenizable;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.List;

public class FastTokenizer implements Tokenizable {

    private final Tokenizer tokenizer;

    public FastTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public List<String> segment(String text) {
        return this.tokenizer.sentenceProcess(text);
    }

    @Override
    public List<String> analyze(String text, int topN) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        return analyzer.analyzeOnlyWords(text, topN);
    }
}
