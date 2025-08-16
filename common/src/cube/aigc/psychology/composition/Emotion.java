/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONObject;

public enum Emotion {

    Angry("愤怒", "生气"),

    Fearful("恐惧", "害怕"),

    Happy("快乐", "开心"),

    Neutral("中性", "平和"),

    Sad("悲伤", "伤心"),

    Surprise("惊讶", "吃惊"),

    Disgusted("厌恶", "讨厌"),

    Other("其他", "其他"),

    Unknown("未知", "未知"),

    None("无", "无"),

    ;

    public final String primaryWord;

    public final String secondaryWord;

    Emotion(String primaryWord, String secondaryWord) {
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

    public static Emotion[] commonEmotions() {
        return new Emotion[]{
                Emotion.Angry,
                Emotion.Fearful,
                Emotion.Happy,
                Emotion.Neutral,
                Emotion.Sad,
                Emotion.Surprise
        };
    }

    public static Emotion parse(String word) {
        for (Emotion emotion : Emotion.values()) {
            if (emotion.name().equalsIgnoreCase(word) ||
                    emotion.primaryWord.equalsIgnoreCase(word) ||
                    emotion.secondaryWord.equalsIgnoreCase(word)) {
                return emotion;
            }
        }
        return None;
    }
}
