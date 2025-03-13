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
public enum Subtask {

    Yes("yes"),

    No("no"),

    EndTopic("end_topic"),

    PredictPainting("predict_painting"),

    QueryReport("query_report"),

    SelectReport("select_report"),

    UnselectReport("unselect_report"),

    ShowPainting("show_painting"),

    ShowIndicator("show_indicator"),

    ShowPersonality("show_personality"),

    ShowCoT("show_cot"),

    None("none"),

    ;

    private static final Pattern sPattern = Pattern.compile("\\{\\{([^}])*\\}\\}");

    public final String name;

    Subtask(String name) {
        this.name = name;
    }

    public static Subtask extract(String text) {
        Matcher matcher = sPattern.matcher(text);
        if (!matcher.find()) {
            return None;
        }
        String task = matcher.group(0);
        task = task.replaceAll("\\{", "");
        task = task.replaceAll("\\}", "");
        task = task.trim();
        for (Subtask subtask : Subtask.values()) {
            if (subtask.name.equalsIgnoreCase(task)) {
                return subtask;
            }
        }
        return None;
    }
}
