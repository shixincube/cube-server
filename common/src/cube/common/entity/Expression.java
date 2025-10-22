/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

public enum Expression {

    Angry("愤怒", "生气"),

    Disgust("厌恶", "讨厌"),

    Fearful("恐惧", "害怕"),

    Happy("快乐", "开心"),

    Neutral("中性", "平和"),

    Sad("悲伤", "伤心"),

    Surprised("惊讶", "诧异"),

    Other("其他", "其他")

    ;

    public final String primaryWord;

    public final String secondaryWord;

    Expression(String primaryWord, String secondaryWord) {
        this.primaryWord = primaryWord;
        this.secondaryWord = secondaryWord;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name());
        json.put("primaryWord", this.primaryWord);
        json.put("secondaryWord", this.secondaryWord);
        return json;
    }

    public static Expression parse(String word) {
        for (Expression expression : Expression.values()) {
            if (expression.name().equalsIgnoreCase(word) ||
                    expression.primaryWord.equalsIgnoreCase(word) ||
                    expression.secondaryWord.equalsIgnoreCase(word)) {
                return expression;
            }
        }
        return Other;
    }
}
