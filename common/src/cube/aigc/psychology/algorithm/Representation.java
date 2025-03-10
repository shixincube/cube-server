/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.Resource;
import cube.aigc.psychology.material.Thing;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 表征含义。
 */
public class Representation implements JSONable {

    public final static String HighTrick = "明显";
    public final static String LowTrick = "缺乏"; // "不足";
    public final static String NormalTrick = "具有";

    public final static String HighTendency = "倾向高";
    public final static String LowTendency = "倾向低";
    public final static String NormalTendency = "倾向中等";

    public final static String Strong = "强";
    public final static String Weak = "弱";
    public final static String Normal = "一般";
    public final static String High = "高";

    public KnowledgeStrategy knowledgeStrategy;

    public int positiveCorrelation = 0;

    public int negativeCorrelation = 0;

    public String description = "";

    public List<PerceptronThing> things;

    public Representation(KnowledgeStrategy knowledgeStrategy) {
        this.knowledgeStrategy = knowledgeStrategy;
        this.things = new ArrayList<>();
    }

    public Representation(JSONObject json) {
        this.knowledgeStrategy = new KnowledgeStrategy(json.getJSONObject("knowledgeStrategy"));
        if (this.knowledgeStrategy.isEmpty()) {
            // 重新从资源中加载数据
            KnowledgeStrategy ks = Resource.getInstance().getTermInterpretation(this.knowledgeStrategy.getTerm());
            if (null != ks) {
                this.knowledgeStrategy = ks;
            }
        }
        this.positiveCorrelation = json.getInt("positiveCorrelation");
        this.negativeCorrelation = json.getInt("negativeCorrelation");
        this.description = json.getString("description");
        this.things = new ArrayList<>();
        if (json.has("things")) {
            JSONArray thingArray = json.getJSONArray("things");
            for (int i = 0; i < thingArray.length(); ++i) {
                PerceptronThing thing = new PerceptronThing(thingArray.getJSONObject(i));
                this.things.add(thing);
            }
        }
    }

    public void addThings(Thing[] things) {
        if (null == things || things.length == 0) {
            return;
        }
        for (Thing thing : things) {
            PerceptronThing pt = new PerceptronThing(thing);
            this.things.add(pt);
        }
    }

    public void makeDescription() {
        String marked = null;
        // 趋势
//        if (this.positiveCorrelation == this.negativeCorrelation) {
//            marked = NormalTrick + this.knowledgeStrategy.getTerm().word;
//        }

        if (this.negativeCorrelation > 0 &&
                this.positiveCorrelation < this.negativeCorrelation) {
            switch (this.knowledgeStrategy.getTerm()) {
                case Depression:
                case Anxiety:
                case Suspiciousness:
                    marked = this.knowledgeStrategy.getTerm().word + LowTendency;
                    break;
                case SenseOfReality:
                case SenseOfSecurity:
                    marked = this.knowledgeStrategy.getTerm().word + Weak;
                    break;
                case SelfEsteem:
                    marked = this.knowledgeStrategy.getTerm().word + Normal;
                    break;
                default:
                    marked = LowTrick + this.knowledgeStrategy.getTerm().word;
                    break;
            }
        }
        else if (this.positiveCorrelation >= 2 ||
                (this.positiveCorrelation - this.negativeCorrelation) >= 4) {
            switch (this.knowledgeStrategy.getTerm()) {
                case Depression:
                case Anxiety:
                case Suspiciousness:
                    marked = this.knowledgeStrategy.getTerm().word + HighTendency;
                    break;
                case SenseOfReality:
                case SenseOfSecurity:
                    marked = this.knowledgeStrategy.getTerm().word + Strong;
                    break;
                case SelfEsteem:
                    marked = this.knowledgeStrategy.getTerm().word + High;
                    break;
                default:
                    marked = HighTrick + this.knowledgeStrategy.getTerm().word;
                    break;
            }
        }
        else {
            switch (this.knowledgeStrategy.getTerm()) {
                case Depression:
                case Anxiety:
                case Suspiciousness:
                    marked = this.knowledgeStrategy.getTerm().word + NormalTendency;
                    break;
                case SenseOfReality:
                case SenseOfSecurity:
                    marked = this.knowledgeStrategy.getTerm().word + Normal;
                    break;
                case SelfEsteem:
                    marked = this.knowledgeStrategy.getTerm().word + Normal;
                    break;
                default:
                    marked = NormalTrick + this.knowledgeStrategy.getTerm().word;
                    break;
            }
        }
        // 设置短描述
        this.description = marked;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Representation) {
            Representation other = (Representation) obj;
            if (other.knowledgeStrategy.getTerm() == this.knowledgeStrategy.getTerm()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.knowledgeStrategy.getTerm().hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("knowledgeStrategy", this.knowledgeStrategy.toJSON());
        json.put("positiveCorrelation", this.positiveCorrelation);
        json.put("negativeCorrelation", this.negativeCorrelation);
        json.put("description", this.description);
        JSONArray array = new JSONArray();
        for (PerceptronThing thing : this.things) {
            array.put(thing.toJSON());
        }
        json.put("things", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("knowledgeStrategy", this.knowledgeStrategy.toCompactJSON());
        json.put("positiveCorrelation", this.positiveCorrelation);
        json.put("negativeCorrelation", this.negativeCorrelation);
        json.put("description", this.description);
        JSONArray array = new JSONArray();
        for (PerceptronThing thing : this.things) {
            array.put(thing.toCompactJSON());
        }
        json.put("things", array);
        return json;
    }

    public JSONObject toStrictJSON() {
        JSONObject json = new JSONObject();
        json.put("knowledgeStrategy", this.knowledgeStrategy.toCompactJSON());
        json.put("positiveCorrelation", this.positiveCorrelation);
        json.put("negativeCorrelation", this.negativeCorrelation);
        json.put("description", this.description);
        JSONArray array = new JSONArray();
        for (PerceptronThing thing : this.things) {
            array.put(thing.toJSON());
        }
        json.put("things", array);
        return json;
    }
}
