package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 预测出的因子集合。
 */
public class FactorSet implements JSONable {

    public final static String Somatization = "somatization";
    public final static String Obsession = "obsession";
    public final static String Interpersonal = "interpersonal";
    public final static String Depression = "depression";
    public final static String Anxiety = "anxiety";
    public final static String Hostile = "hostile";
    public final static String Horror = "horror";
    public final static String Paranoid = "paranoid";
    public final static String Psychosis = "psychosis";

    public class NormRange {
        public final String symptom;

        public final double low;
        public final double high;

        public final double value;
        public final boolean norm;

        public NormRange(String symptom, double low, double high, double value) {
            this.symptom = symptom;
            this.low = low;
            this.high = high;
            this.value = value;
            this.norm = value >= low && value <= high;
        }
    }

    public SymptomFactor symptomFactor;

    public PersonalityFactor personalityFactor;

    public AffectFactor affectFactor;

    public FactorSet(JSONObject json) {
        JSONArray symptomArray = json.getJSONArray("symptoms");
        JSONArray personalityArray = json.getJSONArray("personalities");
        JSONArray affectArray = json.getJSONArray("affects");

        this.symptomFactor = new SymptomFactor(symptomArray);
        this.personalityFactor = new PersonalityFactor(personalityArray);
        this.affectFactor = new AffectFactor(affectArray);
    }

    public int calcSymptomTotal() {
        return (int) (this.symptomFactor.somatization * 12 +
                this.symptomFactor.obsession * 10 +
                this.symptomFactor.interpersonal * 9 +
                this.symptomFactor.depression * 13 +
                this.symptomFactor.anxiety * 10 +
                this.symptomFactor.hostile * 6 +
                this.symptomFactor.horror * 7 +
                this.symptomFactor.paranoid * 6 +
                this.symptomFactor.psychosis * 10 +
                this.symptomFactor.sleepDiet * 7);
    }

    public NormRange normSomatization() {
        return new NormRange("somatization", 0.89, 1.85, this.symptomFactor.somatization);
    }

    public NormRange normObsession() {
        return new NormRange("obsession", 1.04, 2.20, this.symptomFactor.obsession);
    }

    public NormRange normInterpersonal() {
        return new NormRange("interpersonal", 1.04, 2.26, this.symptomFactor.interpersonal);
    }

    public NormRange normDepression() {
        return new NormRange("depression", 0.91, 2.09, this.symptomFactor.depression);
    }

    public NormRange normAnxiety() {
        return new NormRange("anxiety", 0.96, 1.82, this.symptomFactor.anxiety);
    }

    public NormRange normHostile() {
        return new NormRange("hostile", 0.91, 2.01, this.symptomFactor.hostile);
    }

    public NormRange normHorror() {
        return new NormRange("horror", 0.82, 1.64, this.symptomFactor.horror);
    }

    public NormRange normParanoid() {
        return new NormRange("paranoid", 0.86, 2.00, this.symptomFactor.paranoid);
    }

    public NormRange normPsychosis() {
        return new NormRange("psychosis", 0.87, 1.71, this.symptomFactor.psychosis);
    }

    public NormRange normSymptom(String symptom) {
        if (symptom.equalsIgnoreCase("somatization") || symptom.equals("躯体化")) {
            return normSomatization();
        }
        else if (symptom.equalsIgnoreCase("obsession") || symptom.equals("强迫")) {
            return normObsession();
        }
        else if (symptom.equalsIgnoreCase("interpersonal") || symptom.equals("人际关系")) {
            return normInterpersonal();
        }
        else if (symptom.equalsIgnoreCase("depression") || symptom.equals("抑郁")) {
            return normDepression();
        }
        else if (symptom.equalsIgnoreCase("anxiety") || symptom.equals("焦虑")) {
            return normAnxiety();
        }
        else if (symptom.equalsIgnoreCase("hostile") || symptom.equals("敌对")) {
            return normHostile();
        }
        else if (symptom.equalsIgnoreCase("horror") || symptom.equals("恐怖")) {
            return normHorror();
        }
        else if (symptom.equalsIgnoreCase("paranoid") || symptom.equals("偏执")) {
            return normParanoid();
        }
        else if (symptom.equalsIgnoreCase("psychosis") || symptom.equals("精神病性")) {
            return normPsychosis();
        }
        else {
            return null;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("symptoms", this.symptomFactor.toArray());
        json.put("personalities", this.personalityFactor.toArray());
        json.put("affects", this.affectFactor.toArray());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public class SymptomFactor {

        public double somatization;

        public double obsession;

        public double interpersonal;

        public double depression;

        public double anxiety;

        public double hostile;

        public double horror;

        public double paranoid;

        public double psychosis;

        public double sleepDiet;

        public int total;

        public SymptomFactor(JSONArray array) {
            this.somatization = array.getJSONObject(0).getDouble("value");
            this.obsession = array.getJSONObject(1).getDouble("value");
            this.interpersonal = array.getJSONObject(2).getDouble("value");
            this.depression = array.getJSONObject(3).getDouble("value");
            this.anxiety = array.getJSONObject(4).getDouble("value");
            this.hostile = array.getJSONObject(5).getDouble("value");
            this.horror = array.getJSONObject(6).getDouble("value");
            this.paranoid = array.getJSONObject(7).getDouble("value");
            this.psychosis = array.getJSONObject(8).getDouble("value");
            this.sleepDiet = array.getJSONObject(9).getDouble("value");

            this.total = (int) (this.somatization * 12 +
                    this.obsession * 10 +
                    this.interpersonal * 9 +
                    this.depression * 13 +
                    this.anxiety * 10 +
                    this.hostile * 6 +
                    this.horror * 7 +
                    this.paranoid * 6 +
                    this.psychosis * 10 +
                    this.sleepDiet * 7);
        }

        public JSONArray toArray() {
            JSONObject somatizationJson = new JSONObject();
            somatizationJson.put("name", "somatization");
            somatizationJson.put("value", this.somatization);

            JSONObject obsessionJson = new JSONObject();
            obsessionJson.put("name", "obsession");
            obsessionJson.put("value", this.obsession);

            JSONObject interpersonalJson = new JSONObject();
            interpersonalJson.put("name", "interpersonal");
            interpersonalJson.put("value", this.interpersonal);

            JSONObject depressionJson = new JSONObject();
            depressionJson.put("name", "depression");
            depressionJson.put("value", this.depression);

            JSONObject anxietyJson = new JSONObject();
            anxietyJson.put("name", "anxiety");
            anxietyJson.put("value", this.anxiety);

            JSONObject hostileJson = new JSONObject();
            hostileJson.put("name", "hostile");
            hostileJson.put("value", this.hostile);

            JSONObject horrorJson = new JSONObject();
            horrorJson.put("name", "horror");
            horrorJson.put("value", this.horror);

            JSONObject paranoidJson = new JSONObject();
            paranoidJson.put("name", "paranoid");
            paranoidJson.put("value", this.paranoid);

            JSONObject psychosisJson = new JSONObject();
            psychosisJson.put("name", "psychosis");
            psychosisJson.put("value", this.psychosis);

            JSONObject sleepDietJson = new JSONObject();
            sleepDietJson.put("name", "sleepDiet");
            sleepDietJson.put("value", this.sleepDiet);

            JSONArray array = new JSONArray();
            array.put(somatizationJson);
            array.put(obsessionJson);
            array.put(interpersonalJson);
            array.put(depressionJson);
            array.put(anxietyJson);
            array.put(hostileJson);
            array.put(horrorJson);
            array.put(paranoidJson);
            array.put(psychosisJson);
            array.put(sleepDietJson);
            return array;
        }
    }


    public class PersonalityFactor {

        public double obligingness;

        public double conscientiousness;

        public double extraversion;

        public double achievement;

        public double neuroticism;

        public PersonalityFactor(JSONArray array) {
            this.obligingness = array.getJSONObject(0).getDouble("value");
            this.conscientiousness = array.getJSONObject(1).getDouble("value");
            this.extraversion = array.getJSONObject(2).getDouble("value");
            this.achievement = array.getJSONObject(3).getDouble("value");
            this.neuroticism = array.getJSONObject(4).getDouble("value");
        }

        public JSONArray toArray() {
            JSONObject obligingnessJson = new JSONObject();
            obligingnessJson.put("name", "obligingness");
            obligingnessJson.put("value", this.obligingness);

            JSONObject conscientiousnessJson = new JSONObject();
            conscientiousnessJson.put("name", "conscientiousness");
            conscientiousnessJson.put("value", this.conscientiousness);

            JSONObject extraversionJson = new JSONObject();
            extraversionJson.put("name", "extraversion");
            extraversionJson.put("value", this.extraversion);

            JSONObject achievementJson = new JSONObject();
            achievementJson.put("name", "achievement");
            achievementJson.put("value", this.achievement);

            JSONObject neuroticismJson = new JSONObject();
            neuroticismJson.put("name", "neuroticism");
            neuroticismJson.put("value", this.neuroticism);

            JSONArray array = new JSONArray();
            array.put(obligingnessJson);
            array.put(conscientiousnessJson);
            array.put(extraversionJson);
            array.put(achievementJson);
            array.put(neuroticismJson);
            return array;
        }
    }

    public class AffectFactor {

        public final static double Low = 20;
        public final static double Median = 30;
        public final static double High = 40;

        public double positive;

        public double negative;

        public AffectFactor(JSONArray array) {
            this.positive = array.getJSONObject(0).getDouble("value");
            this.negative = array.getJSONObject(1).getDouble("value");
        }

        public JSONArray toArray() {
            JSONObject positiveJson = new JSONObject();
            positiveJson.put("name", "positive");
            positiveJson.put("value", this.positive);

            JSONObject negativeJson = new JSONObject();
            negativeJson.put("name", "negative");
            negativeJson.put("value", this.negative);

            JSONArray array = new JSONArray();
            array.put(positiveJson);
            array.put(negativeJson);
            return array;
        }

        public String makeContentMarkdown() {
            StringBuffer buf = new StringBuffer();
            buf.append("## 情绪表现\n\n");
            if (this.positive >= this.negative) {
                buf.append("受测人的积极情绪高于消极情绪。\n\n");
            }
            else {
                buf.append("受测人的积极情绪低于消极情绪。\n\n");
            }

            if (this.positive >= High) {
                buf.append("- 受测人精力旺盛，能保持全神贯注，常处于积极的情绪状况。");
            }
            else if (this.positive <= Low) {
                buf.append("- 受测人情绪状况淡漠，对周围环境、事件或人际关系表现出缺乏情绪反应的状态。");
            }
            else {
                buf.append("- 受测人在绝大多数时间里都保持积极的情绪状况。");
            }

            buf.append("\n\n");

            if (this.negative >= High) {
                buf.append("- 受测人主观感觉情绪困惑，有痛苦的情绪状态。");
            }
            else if (this.negative <= Low) {
                buf.append("- 受测人情绪状况镇定，能够保持冷静、稳定和自我控制的情绪状态。");
            }
            else {
                buf.append("- 受测人有一定的情绪管理能力，面对适度压力时可以避免被负面情绪左右。");
            }
            return buf.toString();
        }
    }
}
