package cube.aigc.psychology.composition;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 预测出的因子集合。
 */
public class FactorSet {

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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("symptoms", this.symptomFactor.toArray());
        json.put("personalities", this.personalityFactor.toArray());
        json.put("affects", this.affectFactor.toArray());
        return json;
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
    }
}
