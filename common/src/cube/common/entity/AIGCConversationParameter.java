/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.common.entity;

import java.util.List;

/**
 * 会话参数。
 */
public class AIGCConversationParameter {

    public double temperature = 0.3;

    public double topP = 0.95;

    public double repetitionPenalty = 1.3;

    public int topK = 50;

    public int maxNewTokens = 2048;

    public List<String> categories;

    public List<GenerativeRecord> records;

    public int histories = 0;

    public boolean recordable = false;

    public boolean searchable = false;

    public boolean networking = false;

    public AIGCConversationParameter(double temperature, double topP, double repetitionPenalty, int maxNewTokens,
                                     List<GenerativeRecord> records, List<String> categories, int histories,
                                     boolean recordable, boolean searchable, boolean networking) {
        this.temperature = temperature;
        this.topP = topP;
        this.repetitionPenalty = repetitionPenalty;
        this.maxNewTokens = maxNewTokens;
        this.records = records;
        this.categories = categories;
        this.histories = histories;
        this.recordable = recordable;
        this.searchable = searchable;
        this.networking = networking;
    }

    public GenerativeOption toGenerativeOption() {
        return new GenerativeOption(this.temperature, this.topP, this.repetitionPenalty, this.maxNewTokens, this.topK);
    }
}
