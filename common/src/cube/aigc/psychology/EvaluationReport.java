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

package cube.aigc.psychology;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.algorithm.*;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.Scale;
import cube.common.JSONable;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 评估报告。
 */
public class EvaluationReport implements JSONable {

    public final static String UNIT = ModelConfig.BAIZE_UNIT;

    private Attribute attribute;

    private Reference reference;

    private List<Representation> representationList;

    private ScoreAccelerator scoreAccelerator;

    private PersonalityAccelerator personalityAccelerator;

    private AttentionSuggestion attentionSuggestion;

    private List<Scale> additionScales;

    private boolean unknown = false;

    private boolean hesitating = false;

    private String version = Version.toVersionString();

    public EvaluationReport(Attribute attribute, Reference reference, EvaluationFeature evaluationFeature) {
        this(attribute, reference, Collections.singletonList((evaluationFeature)));
    }

    public EvaluationReport(Attribute attribute, Reference reference, List<EvaluationFeature> evaluationFeatureList) {
        this.attribute = attribute;
        this.reference = reference;
        this.representationList = new ArrayList<>();
        this.scoreAccelerator = new ScoreAccelerator();
        this.attentionSuggestion = AttentionSuggestion.NoAttention;
        this.personalityAccelerator = new PersonalityAccelerator(evaluationFeatureList);
        this.additionScales = new ArrayList<>();
        this.build(evaluationFeatureList);
    }

    public EvaluationReport(JSONObject json) {
        if (json.has("version")) {
            this.version = json.getString("version");
        }
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        this.reference = json.has("reference") ?
                Reference.parse(json.getString("reference")) : Reference.Normal;

        this.representationList = new ArrayList<>();
        JSONArray array = json.getJSONArray("representationList");
        for (int i = 0; i < array.length(); ++i) {
            this.representationList.add(new Representation(array.getJSONObject(i)));
        }

        this.scoreAccelerator = new ScoreAccelerator(json.getJSONObject("accelerator"));
        this.personalityAccelerator = json.has("personality") ?
                new PersonalityAccelerator(json.getJSONObject("personality")) : null;
        this.attentionSuggestion = AttentionSuggestion.parse(json.getInt("attention"));

        this.additionScales = new ArrayList<>();
        if (json.has("additionScales")) {
            array = json.getJSONArray("additionScales");
            for (int i = 0; i < array.length(); ++i) {
                this.additionScales.add(new Scale(array.getJSONObject(i)));
            }
        }

        this.hesitating = json.has("hesitating") && json.getBoolean("hesitating");
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isUnknown() {
        return this.unknown;
    }

    public boolean isEmpty() {
        return this.representationList.isEmpty();
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public Reference getReference() {
        return this.reference;
    }

    public ScoreAccelerator getScoreAccelerator() {
        return this.scoreAccelerator;
    }

    public PersonalityAccelerator getPersonalityAccelerator() {
        return this.personalityAccelerator;
    }

    public AttentionSuggestion getAttentionSuggestion() {
        return this.attentionSuggestion;
    }

    public boolean isHesitating() {
        return this.hesitating;
    }

    private void build(List<EvaluationFeature> resultList) {
        for (EvaluationFeature result : resultList) {
            if (null != result.getScore(Indicator.Unknown)) {
                this.unknown = true;
                this.attentionSuggestion = AttentionSuggestion.FocusedAttention;
                return;
            }

            for (EvaluationFeature.Feature feature : result.getFeatures()) {
                Representation representation = this.getRepresentation(feature.term);
                if (null == representation) {
                    KnowledgeStrategy interpretation = Resource.getInstance().getTermInterpretation(feature.term);
                    if (null == interpretation) {
                        // 没有对应的释义
                        Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + feature.term.word);
                        continue;
                    }

                    representation = new Representation(interpretation);
                    this.representationList.add(representation);
                }

                if (feature.tendency == Tendency.Negative) {
                    representation.negativeCorrelation += 1;
                }
                if (feature.tendency == Tendency.Positive) {
                    representation.positiveCorrelation += 1;
                }
            }

            // 提取并合并分数
            for (Score score : result.getScores()) {
                this.scoreAccelerator.addScore(score);
            }
        }

        // 排序
        this.representationList.sort(new Comparator<Representation>() {
            @Override
            public int compare(Representation representation1, Representation representation2) {
                int score1 = representation1.positiveCorrelation + representation1.negativeCorrelation;
                int score2 = representation2.positiveCorrelation + representation2.negativeCorrelation;
                return score2 - score1;
            }
        });

        // 计算关注
        if (!this.calcAttention()) {
            this.recheckAttention();
        }

        Logger.i(this.getClass(), "#build - reference: " + this.reference.name);

        // 判断 hesitating
        if (this.representationList.size() <= 7 || this.scoreAccelerator.getEvaluationScores().size() <= 5) {
            this.hesitating = true;
        }
    }

    private boolean calcAttention() {
        StringBuilder script = new StringBuilder();
        script.append("var Attribute = Java.type('cube.aigc.psychology.Attribute');\n");
        script.append("var Indicator = Java.type('cube.aigc.psychology.Indicator');\n");
        script.append("var Reference = Java.type('cube.aigc.psychology.Reference');\n");
        script.append("var IndicatorRate = Java.type('cube.aigc.psychology.algorithm.IndicatorRate');\n");
        script.append("var Attention = Java.type('cube.aigc.psychology.algorithm.AttentionSuggestion');\n");
        script.append("var Score = Java.type('cube.aigc.psychology.composition.EvaluationScore');\n");
        script.append("var Logger = Java.type('cell.util.log.Logger');\n");
        try {
            script.append(Resource.getInstance().loadAttentionScript());
        } catch (Exception e) {
            Logger.e(this.getClass(), "#calcAttention", e);
            return false;
        }

        ScriptObjectMirror returnVal = null;
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        try {
            engine.eval(script.toString());
            Invocable invocable = (Invocable) engine;
            returnVal = (ScriptObjectMirror) invocable.invokeFunction("calc", this.attribute,
                    this.getEvaluationScores(), this.reference);
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.e(this.getClass(), "#calcAttention", e);
            return false;
        }

        if (null == returnVal) {
            Logger.w(this.getClass(), "#calcAttention - Return value is null");
            return false;
        }

        if (returnVal.containsKey("attention")) {
            this.attentionSuggestion = (AttentionSuggestion) returnVal.get("attention");
        }
        if (returnVal.containsKey("reference")) {
            this.reference = (Reference) returnVal.get("reference");
        }
        if (returnVal.containsKey("additionScale")) {
            String scaleName = (String) returnVal.get("additionScale");
            if (scaleName.length() > 2) {
                this.additionScales.add(Resource.getInstance().loadScaleByName(scaleName));
            }
        }

        return true;
    }

    private void recheckAttention() {
//        if (this.reference == Reference.Normal) {
//            this.attentionSuggestion = AttentionSuggestion.NoAttention;
//            return;
//        }

        int score = 0;
        boolean depression = false;
        double depressionScore = 0;
        boolean senseOfSecurity = false;
        boolean stress = false;
        boolean anxiety = false;
        boolean obsession = false;
        boolean optimism = false;
        boolean pessimism = false;

        for (EvaluationScore es : this.scoreAccelerator.getEvaluationScores()) {
            switch (es.indicator) {
                case Psychosis:
                    if (es.positiveScore > 0.9) {
                        score += 4;
                        Logger.d(this.getClass(), "Attention: Psychosis +4");
                    }
                    else if (es.positiveScore > 0.6) {
                        score += 3;
                        Logger.d(this.getClass(), "Attention: Psychosis +3");
                    }
                    else if (es.positiveScore > 0.3) {
                        score += 2;
                        Logger.d(this.getClass(), "Attention: Psychosis +2");
                    }
                    break;
                case SocialAdaptability:
                    double delta = es.negativeScore - es.positiveScore;
                    if (delta > 0) {
                        if (delta > 0.9) {
                            score += 2;
                            Logger.d(this.getClass(), "Attention: SocialAdaptability +2");
                        }
                        else if (delta >= 0.5) {
                            score += 1;
                            Logger.d(this.getClass(), "Attention: SocialAdaptability +1");
                        }
                    }
                    break;
                case Depression:
                    depressionScore = es.positiveScore - es.negativeScore;
                    if (depressionScore >= 1.2) {
                        depression = true;
                        score += 3;
                        Logger.d(this.getClass(), "Attention: Depression +3");
                    }
                    else if (depressionScore > 0.8) {
                        depression = true;
                        score += 2;
                        Logger.d(this.getClass(), "Attention: Depression +2");
                    }
                    else if (depressionScore > 0.4) {
                        depression = true;
                        score += 1;
                        Logger.d(this.getClass(), "Attention: Depression +1");
                    }
                    else if (depressionScore > 0) {
                        depression = true;
                        Logger.d(this.getClass(), "Attention: Depression 0");
                    }
                    else if (depressionScore < 0) {
                        score -= 1;
                        Logger.d(this.getClass(), "Attention: Depression -1");
                    }
                    break;
                case SenseOfSecurity:
                    if (es.negativeScore - es.positiveScore > 0.6) {
                        senseOfSecurity = true;
                        score += 1;
                        Logger.d(this.getClass(), "Attention: SenseOfSecurity +1");
                    }
                    break;
                case Stress:
                    // 压力 0.4 是阈值
                    if (es.positiveScore - es.negativeScore > 0.5) {
                        stress = true;
                    }
                    break;
                case Anxiety:
                    double anxietyScore = es.positiveScore - es.negativeScore;
                    if (anxietyScore > 1.5) {
                        anxiety = true;
                        score += 2;
                        Logger.d(this.getClass(), "Attention: Anxiety +2");
                    }
                    else if (anxietyScore > 0.8) {
                        anxiety = true;
                        score += 1;
                        Logger.d(this.getClass(), "Attention: Anxiety +1");
                    }
                    else if (anxietyScore > 0) {
                        anxiety = true;
                        Logger.d(this.getClass(), "Attention: Anxiety 0");
                    }
                    else if (anxietyScore < 0) {
                        score -= 1;
                        Logger.d(this.getClass(), "Attention: Anxiety -1");
                    }
                    break;
                case Obsession:
                    if (es.positiveScore - es.negativeScore > 0.8) {
                        obsession = true;
                        score += 1;
                        Logger.d(this.getClass(), "Attention: Obsession +1");
                    }
                    break;
                case Optimism:
                    if (es.positiveScore - es.negativeScore > 1.0) {
                        optimism = true;
                        score -= 1;
                        Logger.d(this.getClass(), "Attention: Optimism -1");
                    }
                    else if (es.positiveScore - es.negativeScore > 0.5) {
                        optimism = true;
                    }
                    break;
                case Pessimism:
                    if (es.positiveScore - es.negativeScore > 0.1) {
                        pessimism = true;
                    }
                    break;
                case Unknown:
                    // 绘图未被识别
                    score = 4;
                    Logger.d(this.getClass(), "Attention: Unknown =4");
                    break;
                default:
                    break;
            }
        }

        Logger.d(this.getClass(), "#recheckAttention - Raw Score: " + score);

        if (depression && senseOfSecurity) {
            score += 1;
            Logger.d(this.getClass(), "#recheckAttention - (depression && senseOfSecurity)");
        }
        else if (depression && stress) {
            score += 1;
            Logger.d(this.getClass(), "#recheckAttention - (depression && stress)");
        }
        else if (depression && anxiety) {
            score += 1;
            Logger.d(this.getClass(), "#recheckAttention - (depression && anxiety)");
        }

        if (!depression && !anxiety) {
            score -= 1;
            Logger.d(this.getClass(), "#recheckAttention - (!depression && !anxiety)");
        }

        Logger.d(this.getClass(), "#recheckAttention - score: " + score + " - " +
                "depression:" + depression + " | " +
                "senseOfSecurity:" + senseOfSecurity + " | " +
                "stress:" + stress + " | " +
                "anxiety:" + anxiety + " | " +
                "obsession:" + obsession + " | " +
                "optimism:" + optimism + " | " +
                "pessimism:" + pessimism);

        // 根据 strict 修正
        if (this.attribute.strict) {
            if (optimism) {
                if (depressionScore >= 0.4) {
                    score -= 1;
                    Logger.d(this.getClass(), "Attention: strict -> Optimism -1");
                }
            }
        }

        // 根据年龄就行修正
        if (this.attribute.age > 20 && this.attribute.age < 35) {
            if (score >= 5) {
                score -= 1;
                Logger.d(this.getClass(), "Attention: (age > 20) -=1");
            }
        }
        else if (this.attribute.age >= 35 && this.attribute.age <= 50) {
            if (score >= 5) {
                score -= 2;
                Logger.d(this.getClass(), "Attention: (age >= 35) -=2");
            }
        }
        else if (this.attribute.age > 50) {
            if (score >= 5) {
                score = 3;
            }
            else if (score >= 4) {
                score = 2;
            }
        }

        if (score > 0) {
            if (score >= 5) {
                this.attentionSuggestion = AttentionSuggestion.SpecialAttention;

                this.reference = Reference.Abnormal;
                Logger.d(this.getClass(), "Attention: Fix reference to Abnormal (score>=5)");
            }
            else if (score >= 4) {
                this.attentionSuggestion = AttentionSuggestion.FocusedAttention;
            }
            else {
                this.attentionSuggestion = AttentionSuggestion.GeneralAttention;
            }
        }

        if (score >= 4 || this.reference == Reference.Abnormal) {
            this.additionScales.add(Resource.getInstance().loadScaleByName("SCL-90"));
        }

        if (this.reference == Reference.Abnormal) {
            if (this.attribute.age <= 11) {
                if (this.attentionSuggestion == AttentionSuggestion.FocusedAttention) {
                    // 调整为特殊关注
                    this.attentionSuggestion = AttentionSuggestion.SpecialAttention;
                }
                else if (this.attentionSuggestion == AttentionSuggestion.NoAttention) {
                    // 调整为重点关注
                    this.attentionSuggestion = AttentionSuggestion.FocusedAttention;
                    Logger.d(this.getClass(), "Attention: Focused attention");
                }
            }
            else {
                if (this.attentionSuggestion == AttentionSuggestion.NoAttention) {
                    // 如果非模态，将非关注标注为一般关注
                    this.attentionSuggestion = AttentionSuggestion.GeneralAttention;
                    Logger.d(this.getClass(), "Attention: General attention");
                }
            }
        }
    }

    public Representation getRepresentation(Term term) {
        for (Representation representation : this.representationList) {
            if (representation.knowledgeStrategy.getTerm() == term) {
                return representation;
            }
        }
        return null;
    }

    public int numRepresentations() {
        return this.representationList.size();
    }

    public List<Representation> getRepresentationList() {
        return this.representationList;
    }

    /**
     * 通过和评分结果进行对比排序，返回表征列表。
     *
     * @return
     */
    public List<Representation> getRepresentationListByEvaluationScore(int topNum) {
        List<Representation> result = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getTerm());
            if (null != indicator) {
                result.add(representation);
            }
        }

        if (result.size() == topNum) {
            return result;
        }

        // 补齐 Top N 数量
        if (result.size() < topNum) {
            for (Representation representation : this.representationList) {
                if (result.contains(representation)) {
                    continue;
                }

                result.add(representation);
                if (result.size() >= topNum) {
                    break;
                }
            }
        }
        else {
            while (result.size() > topNum) {
                result.remove(result.size() - 1);
            }
        }

        // "理想化" Idealization
        for (Representation representation : result) {
            if (representation.knowledgeStrategy.getTerm() == Term.Idealization) {
                result.remove(representation);
                result.add(representation);
                break;
            }
        }

        return result;
    }

    /**
     * 返回特征列表，删除与评估分重叠的数据。
     *
     * @return
     */
    public List<Representation> getRepresentationListWithoutEvaluationScore() {
        List<Representation> result = new ArrayList<>();
        result.addAll(this.representationList);

        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getTerm());
            if (null != indicator) {
                result.remove(representation);
            }
        }

        // 删除"理想化" Idealization
        for (Representation representation : result) {
            if (representation.knowledgeStrategy.getTerm() == Term.Idealization) {
                result.remove(representation);
                break;
            }
        }

        return result;
    }

    /**
     * 通过和表征对比、排序，返回评估得分。
     *
     * @return
     */
    public List<EvaluationScore> getEvaluationScoresByRepresentation(int maxNum) {
        List<Indicator> indicators = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getTerm());
            if (null != indicator) {
                indicators.add(indicator);
            }
        }

        List<EvaluationScore> evaluationScores = this.scoreAccelerator.getEvaluationScores();

        // 按照优先级排序
        Collections.sort(evaluationScores, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore es1, EvaluationScore es2) {
                return es2.indicator.priority - es1.indicator.priority;
            }
        });

        List<EvaluationScore> result = new ArrayList<>();
        for (EvaluationScore es : evaluationScores) {
            if (indicators.contains(es.indicator)) {
                result.add(es);
            }
        }

        if (result.size() < maxNum) {
            // 补充
            for (EvaluationScore es : evaluationScores) {
                if (!result.contains(es)) {
                    result.add(es);
                    if (result.size() >= maxNum) {
                        break;
                    }
                }
            }
        }
        else {
            while (result.size() > maxNum) {
                result.remove(result.size() - 1);
            }
        }

        // 按照优先级排序
        Collections.sort(result, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore es1, EvaluationScore es2) {
                return es2.indicator.priority - es1.indicator.priority;
            }
        });

        // "人际关系"移到最后
        if (result.get(0).indicator == Indicator.InterpersonalRelation) {
            EvaluationScore es = result.remove(0);
            result.add(es);
        }
        // "理想主义"移动最后
        for (EvaluationScore es : result) {
            if (es.indicator == Indicator.Idealism) {
                result.remove(es);
                result.add(es);
                break;
            }
        }
        // "自我意识"移动到最后
        for (EvaluationScore es : result) {
            if (es.indicator == Indicator.SelfConsciousness) {
                result.remove(es);
                result.add(es);
                break;
            }
        }

        return result;
    }

    public List<EvaluationScore> getEvaluationScores() {
        List<EvaluationScore> result = new ArrayList<>(this.scoreAccelerator.getEvaluationScores());
        // 排序
        Collections.sort(result, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore s1, EvaluationScore s2) {
                return s2.indicator.priority - s1.indicator.priority;
            }
        });
        return result;
    }

    public int numEvaluationScores() {
        return this.scoreAccelerator.getEvaluationScores().size();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("version", this.version);
        json.put("attribute", this.attribute.toJSON());
        json.put("reference", this.reference.name);

        JSONArray array = new JSONArray();
        for (Representation representation : this.representationList) {
            array.put(representation.toJSON());
        }
        json.put("representationList", array);

        json.put("accelerator", this.scoreAccelerator.toJSON());

        if (null != this.personalityAccelerator) {
            json.put("personality", this.personalityAccelerator.toJSON());
        }

        json.put("attention", this.attentionSuggestion.level);

        array = new JSONArray();
        for (Scale scale : this.additionScales) {
            array.put(scale.toCompactJSON());
        }
        json.put("additionScales", array);

        json.put("hesitating", this.hesitating);

//        JSONArray priorityArray = new JSONArray();
//        for (EvaluationScore es : this.getEvaluationScoresByRepresentation()) {
//            priorityArray.put(es.toJSON());
//        }
//        json.put("priorities", priorityArray);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("version", this.version);
        json.put("attribute", this.attribute.toJSON());
        json.put("reference", this.reference.name);

        JSONArray array = new JSONArray();
        for (Representation representation : this.representationList) {
            array.put(representation.toCompactJSON());
        }
        json.put("representationList", array);

        json.put("accelerator", this.scoreAccelerator.toCompactJSON());

        if (null != this.personalityAccelerator) {
            json.put("personality", this.personalityAccelerator.toCompactJSON());
        }

        json.put("attention", this.attentionSuggestion.level);

        array = new JSONArray();
        for (Scale scale : this.additionScales) {
            array.put(scale.toCompactJSON());
        }
        json.put("additionScales", array);

        json.put("hesitating", this.hesitating);

//        JSONArray priorityArray = new JSONArray();
//        for (EvaluationScore es : this.getEvaluationScoresByRepresentation()) {
//            priorityArray.put(es.toCompactJSON());
//        }
//        json.put("priorities", priorityArray);

        return json;
    }
}
