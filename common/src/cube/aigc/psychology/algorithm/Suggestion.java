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

package cube.aigc.psychology.algorithm;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 测评建议。
 */
public class Suggestion implements JSONable {

    public static final Suggestion NoIntervention = new Suggestion("无需干预",
            "目前不需要采取其他针对性措施。",
            "NoIntervention");

    public static final Suggestion ChattingService = new Suggestion("AI陪聊",
            "AI陪聊通过模拟人类的对话和情感交流，能够为用户提供情感上的支持和陪伴。AI陪聊可以成为一个倾听者和安慰者，帮助他们缓解负面情绪，获得情感上的满足和安宁。",
            "ChattingService");

    public static final Suggestion PsychologicalCounseling = new Suggestion("心理咨询",
            "心理咨询通过倾听和共情，心理咨询师能够帮助个体缓解情绪压力，增强情绪管理能力。通过专业的技巧和策略，心理咨询师帮助个体克服心理障碍，恢复心理健康。",
            "PsychologicalCounseling");

    public static final Suggestion PsychiatryDepartment = new Suggestion("寻求就医",
            "心理疾病的种类繁多，症状各异，就医后，医生会通过详细的病史询问、心理评估和必要的身体检查，对患者进行全面的评估，从而确定患者是否患有心理疾病，以及病情的严重程度。这为后续制定个性化的治疗方案提供了科学依据。",
            "PsychiatryDepartment");

    public final String title;

    public final String description;

    public final String action;

    public Suggestion(String title, String description, String action) {
        this.title = title;
        this.description = description;
        this.action = action;
    }

    public Suggestion(JSONObject json) {
        this.title = json.getString("title");
        this.description = json.getString("description");
        this.action = json.getString("action");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        json.put("description", this.description);
        json.put("action", this.action);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
