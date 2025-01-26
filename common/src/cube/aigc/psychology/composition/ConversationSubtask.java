/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 谈话子任务。
 */
public enum ConversationSubtask {

    PredictPainting("predict_painting"),

    ExplainPainting("explain_painting"),

    Unknown("unknown"),

    ;

    private static final Pattern sPattern = Pattern.compile("\\{\\{([^}])*\\}\\}");

    public final String name;

    ConversationSubtask(String name) {
        this.name = name;
    }

    public static ConversationSubtask extract(String text) {
        Matcher matcher = sPattern.matcher(text);
        if (!matcher.find()) {
            return Unknown;
        }
        String task = matcher.group(0);
        task = task.replaceAll("\\{", "");
        task = task.replaceAll("\\}", "");
        task = task.trim();
        for (ConversationSubtask subtask : ConversationSubtask.values()) {
            if (subtask.name.equalsIgnoreCase(task)) {
                return subtask;
            }
        }
        return Unknown;
    }
}
