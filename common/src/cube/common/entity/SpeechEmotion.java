/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 物体检测结果。
 */
public class SpeechEmotion implements JSONable {

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

    public FileLabel file;

    public long elapsed;

    public Emotion emotion;

    public double score;

    public SpeechEmotion(JSONObject json) {
        this.file = new FileLabel(json.getJSONObject("file"));
        this.elapsed = json.getLong("elapsed");
        if (json.has("emotion")) {
            Object obj = json.get("emotion");
            if (obj instanceof String) {
                this.emotion = Emotion.parse(obj.toString());
            }
            else {
                this.emotion = Emotion.parse(json.getJSONObject("emotion").getString("name"));
            }
        }
        else {
            this.emotion = Emotion.None;
        }
        this.score = json.getDouble("score");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("file", this.file.toJSON());
        json.put("elapsed", this.elapsed);
        json.put("emotion", this.emotion.toJSON());
        json.put("score", this.score);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
