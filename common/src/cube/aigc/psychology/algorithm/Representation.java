/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

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
    public final static String NormalTrick = "具有";
    public final static String LowTrick = "缺乏";//"不足";

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
        if (this.positiveCorrelation == this.negativeCorrelation) {
            marked = NormalTrick + this.knowledgeStrategy.getTerm().word;
        }
        else if (this.negativeCorrelation > 0 &&
                this.positiveCorrelation < this.negativeCorrelation) {
            marked = LowTrick + this.knowledgeStrategy.getTerm().word;
        }
        else if (this.positiveCorrelation >= 3 ||
                (this.positiveCorrelation - this.negativeCorrelation) >= 4) {
            marked = HighTrick + this.knowledgeStrategy.getTerm().word;
        }
        else {
            marked = NormalTrick + this.knowledgeStrategy.getTerm().word;
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
}
