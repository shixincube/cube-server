/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.opinion;

/**
 * 舆情任务名称。
 */
public enum OpinionTaskName {

    /**
     * 文章情感概述。
     */
    ArticleSentimentSummary("ArticleSentimentSummary"),

    /**
     * 文章情感分类。
     */
    ArticleSentimentClassification("ArticleSentimentClassification"),

    /**
     * 未知。
     */
    Unknown("Unknown")

    ;

    public final String name;

    OpinionTaskName(String name) {
        this.name = name;
    }

    public static OpinionTaskName parse(String name) {
        for (OpinionTaskName potn : OpinionTaskName.values()) {
            if (potn.name.equals(name)) {
                return potn;
            }
        }

        return null;
    }
}
