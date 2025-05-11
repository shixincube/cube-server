/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    public List<GeneratingRecord> records;

    public int histories = 0;

    public boolean recordable = false;

    public boolean networking = false;

    public AIGCConversationParameter(double temperature, double topP, double repetitionPenalty, int maxNewTokens,
                                     List<GeneratingRecord> records, List<String> categories, int histories,
                                     boolean recordable, boolean networking) {
        this.temperature = temperature;
        this.topP = topP;
        this.repetitionPenalty = repetitionPenalty;
        this.maxNewTokens = maxNewTokens;
        this.records = records;
        this.categories = categories;
        this.histories = histories;
        this.recordable = recordable;
        this.networking = networking;
    }

    public GeneratingOption toGenerativeOption() {
        return new GeneratingOption(this.temperature, this.topP, this.repetitionPenalty, this.maxNewTokens, this.topK);
    }
}
