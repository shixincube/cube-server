/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Natural Language Task
 * @deprecated
 */
public class NLTask implements JSONable {

    public final static String Sentiment = "情感分析";

    public final static String TextClassification = "文本分类";

    public final static String NewsClassification = "新闻分类";

    public final static String IntentionClassification = "意图识别";

    public final static String NLInference = "自然语言推理";

    public final static String SemanticMatching = "语义匹配";

    public final static String AnaphoraResolution = "指代消解";

    public final static String MultipleChoice = "多项选择";

    public final static String ExtractiveReadingComprehension = "抽取式阅读理解";

    public final static String NamedEntityRecognition = "实体识别";

    public final static String KeywordExtraction = "关键词抽取";

    public final static String KeywordRecognition = "关键词识别";

    public final static String GenerativeSummary = "生成式摘要";

    public final static String[] DataSetNewsClassificationList = new String[] {
            "故事", "文化", "娱乐", "体育", "财经", "房产", "汽车", "教育", "科技", "数码"
    };

    public final static String[] DataSetNLInference = new String[] {
            "无关", "矛盾", "蕴含"
    };

    public final static String[] DataSetSemanticMatching = new String[] {
            "相似", "不相似"
    };

    public final static String[] DataSetNamedEntityRecognition = new String[] {
            "人名", "地名", "组织", "机构", "时间点", "日期", "百分比", "货币额度",
            "序数词", "计量规格词", "民族", "职业", "邮箱", "国家", "节日",
            "公司名", "品牌名", "职业", "职位", "邮箱", "手机号码", "电话号码", "IP地址", "身份证号", "网址",
            "电影名", "动漫", "书名", "互联网", "歌名", "产品名", "电视剧名", "电视节目"
    };

    public String type;
    public String textA;
    public String textB;
    public JSONArray choices;
    public String question;
    public JSONArray result;

    public NLTask(String type, JSONArray result) {
        this.type = type;
        this.result = result;
    }

    public NLTask(JSONObject json) {
        this.type = json.getString("type");
        this.textA = json.has("textA") ? json.getString("textA") : null;
        this.textB = json.has("textB") ? json.getString("textB") : null;
        this.choices = json.has("choices") ? json.getJSONArray("choices") : null;
        this.question = json.has("question") ? json.getString("question") : null;
        this.result = json.has("result") ? json.getJSONArray("result") : null;
    }

    /**
     * 自检数据。
     *
     * @return
     */
    public boolean check() {
        if (this.type.equals(GenerativeSummary) || this.type.equals(KeywordExtraction)) {
            // 参数 textA
            if (null != this.textA) {
                return true;
            }
        }
        else if (this.type.equals(Sentiment) || this.type.equals(TextClassification) ||
            this.type.equals(NewsClassification) || this.type.equals(IntentionClassification)) {
            // 参数 textA 和 choices
            if (null != this.textA && null != this.choices && this.choices.length() > 1) {
                return true;
            }
        }
        else if (this.type.equals(NLInference) || this.type.equals(SemanticMatching)) {
            // 参数 textA、textB 和 choices
            if (null != this.textA && null != this.textB && null != this.choices && this.choices.length() > 1) {
                return true;
            }
        }
        else if (this.type.equals(ExtractiveReadingComprehension) || this.type.equals(NamedEntityRecognition)) {
            // 参数 textA 和 question
            if (null != this.textA && null != this.question) {
                return true;
            }
        }
        else if (this.type.equals(AnaphoraResolution) || this.type.equals(MultipleChoice) ||
            this.type.equals(KeywordRecognition)) {
            // 参数 textA、question 和 choices
            if (null != this.textA && null != this.question && null != this.choices && this.choices.length() > 1) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", this.type);

        if (null != this.textA) {
            json.put("textA", this.textA);
        }

        if (null != this.textB) {
            json.put("textB", this.textB);
        }

        if (null != this.choices) {
            json.put("choices", this.choices);
        }

        if (null != this.question) {
            json.put("question", this.question);
        }

        if (null != this.result) {
            json.put("result", this.result);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
