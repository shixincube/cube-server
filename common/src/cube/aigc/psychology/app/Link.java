/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.app;

public class Link {

    public final static String PromptDirect = "aixinli://prompt.direct/";

    public final static String formatPromptDirect(String value) {
        return String.format("%s%s", PromptDirect, value.replaceAll(" ", ""));
    }

    public final static String formatPromptDirectMarkdown(String title, String value) {
        return String.format("[%s](%s%s)", title.replaceAll(" ", ""),
                PromptDirect, value.replaceAll(" ", ""));
    }

    public final static String ScaleAnswer = "aixinli://scale.answer/";

    public final static String GuideAnswer = "aixinli://guide.answer";

    private Link() {
    }
}
