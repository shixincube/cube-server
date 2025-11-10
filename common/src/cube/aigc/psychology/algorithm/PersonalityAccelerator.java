/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cell.util.log.Logger;
import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.composition.BigFiveFactor;
import cube.aigc.psychology.composition.FactorSet;
import cube.common.JSONable;
import cube.util.FloatUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 人格分析催化剂。
 */
public class PersonalityAccelerator implements JSONable  {

    private final static BigFivePersonality[] sBigFivePersonalities = new BigFivePersonality[] {
            BigFivePersonality.Architect,
            BigFivePersonality.Expert,
            BigFivePersonality.Guide,
            BigFivePersonality.Generalist,
            BigFivePersonality.Idealist,
            BigFivePersonality.Supporter,
            BigFivePersonality.Developer,
            BigFivePersonality.Advocate,
            BigFivePersonality.Realist,
            BigFivePersonality.Instructor,
            BigFivePersonality.Promoter,
            BigFivePersonality.Explorer,
            BigFivePersonality.Traditionalist,
            BigFivePersonality.Adapter,
            BigFivePersonality.Entrepreneur,
            BigFivePersonality.Demonstrator,
            BigFivePersonality.Controller
    };

    private final static MBTIFeature[] sMBTIFeatures = new MBTIFeature[] {
            MBTIFeature.INTJ,
            MBTIFeature.INTP,
            MBTIFeature.ENTJ,
            MBTIFeature.ENTP,
            MBTIFeature.INFJ,
            MBTIFeature.INFP,
            MBTIFeature.ENFJ,
            MBTIFeature.ENFP,
            MBTIFeature.ISTJ,
            MBTIFeature.ISFJ,
            MBTIFeature.ESTJ,
            MBTIFeature.ESFJ,
            MBTIFeature.ISTP,
            MBTIFeature.ISFP,
            MBTIFeature.ESTP,
            MBTIFeature.ESFP,
            MBTIFeature.ISFJ
    };

    private List<FiveFactor> fiveFactorList;

    private BigFivePersonality bigFivePersonality;

    /**
     * 置信度。
     */
    public boolean obligingnessConfidence = false;
    public boolean conscientiousnessConfidence = false;
    public boolean extraversionConfidence = false;
    public boolean achievementConfidence = false;
    public boolean neuroticismConfidence = false;

    public PersonalityAccelerator(JSONObject json) {
        this.fiveFactorList = new ArrayList<>();
        JSONArray fiveFactors = json.getJSONArray("fiveFactors");
        for (int i = 0; i < fiveFactors.length(); ++i) {
            this.fiveFactorList.add(new FiveFactor(fiveFactors.getJSONObject(i)));
        }
        if (json.has("bigFivePersonality")) {
            this.bigFivePersonality = new BigFivePersonality(json.getJSONObject("bigFivePersonality"));
        }
        else if (json.has("bigFiveFeature")) {
            this.bigFivePersonality = new BigFivePersonality(json.getJSONObject("bigFiveFeature"));
        }
    }

    public PersonalityAccelerator(List<EvaluationFeature> evaluationFeatureList) {
        this.build(evaluationFeatureList);
    }

    public PersonalityAccelerator(FactorSet factorSet) {
        this.fiveFactorList = new ArrayList<>();
        this.fiveFactorList.add(
                new FiveFactor(BigFiveFactor.Obligingness, factorSet.personalityFactor.obligingness));
        this.fiveFactorList.add(
                new FiveFactor(BigFiveFactor.Conscientiousness, factorSet.personalityFactor.conscientiousness));
        this.fiveFactorList.add(
                new FiveFactor(BigFiveFactor.Extraversion, factorSet.personalityFactor.extraversion));
        this.fiveFactorList.add(
                new FiveFactor(BigFiveFactor.Achievement, factorSet.personalityFactor.achievement));
        this.fiveFactorList.add(
                new FiveFactor(BigFiveFactor.Neuroticism, factorSet.personalityFactor.neuroticism));

        this.bigFivePersonality = new BigFivePersonality(factorSet.personalityFactor.obligingness,
                factorSet.personalityFactor.conscientiousness,
                factorSet.personalityFactor.extraversion,
                factorSet.personalityFactor.achievement,
                factorSet.personalityFactor.neuroticism);

        this.obligingnessConfidence = true;
        this.conscientiousnessConfidence = true;
        this.extraversionConfidence = true;
        this.achievementConfidence = true;
        this.neuroticismConfidence = true;
    }

    private void build(List<EvaluationFeature> evaluationFeatureList) {
        this.fiveFactorList = new ArrayList<>();

        FiveFactor obligingness = new FiveFactor(BigFiveFactor.Obligingness);
        FiveFactor conscientiousness = new FiveFactor(BigFiveFactor.Conscientiousness);
        FiveFactor extraversion = new FiveFactor(BigFiveFactor.Extraversion);
        FiveFactor achievement = new FiveFactor(BigFiveFactor.Achievement);
        FiveFactor neuroticism = new FiveFactor(BigFiveFactor.Neuroticism);

        for (EvaluationFeature ef : evaluationFeatureList) {
            List<EvaluationFeature.FiveFactor> list = ef.getFiveFactors();
            for (EvaluationFeature.FiveFactor factor : list) {
                switch (factor.bigFiveFactor) {
                    case Obligingness:
                        obligingness.score += factor.source;
                        obligingness.total += 1;
                        break;
                    case Conscientiousness:
                        conscientiousness.score += factor.source;
                        conscientiousness.total += 1;
                        break;
                    case Extraversion:
                        extraversion.score += factor.source;
                        extraversion.total += 1;
                        break;
                    case Achievement:
                        achievement.score += factor.source;
                        achievement.total += 1;
                        break;
                    case Neuroticism:
                        neuroticism.score += factor.source;
                        neuroticism.total += 1;
                        break;
                    default:
                        break;
                }
            }
        }

        if (obligingness.total == 0) {
            obligingness.score = FloatUtils.random(5.0, 5.5);
            obligingness.total = 1;

            Logger.d(this.getClass(), "#build - No obligingness");
        }
        else {
            this.obligingnessConfidence = true;
        }

        if (conscientiousness.total == 0) {
            conscientiousness.score = FloatUtils.random(5.0, 5.5);
            conscientiousness.total = 1;

            Logger.d(this.getClass(), "#build - No conscientiousness");
        }
        else {
            this.conscientiousnessConfidence = true;
        }

        if (extraversion.total == 0) {
            extraversion.score = FloatUtils.random(5.0, 5.5);
            extraversion.total = 1;

            Logger.d(this.getClass(), "#build - No extraversion");
        }
        else {
            this.extraversionConfidence = true;
        }

        if (achievement.total == 0) {
            achievement.score = FloatUtils.random(5.0, 5.5);
            achievement.total = 1;

            Logger.d(this.getClass(), "#build - No achievement");
        }
        else {
            this.achievementConfidence = true;
        }

        if (neuroticism.total == 0) {
            neuroticism.score = FloatUtils.random(5.0, 5.5);
            neuroticism.total = 1;

            Logger.d(this.getClass(), "#build - No neuroticism");
        }
        else {
            this.neuroticismConfidence = true;
        }

        this.fiveFactorList.add(obligingness);
        this.fiveFactorList.add(conscientiousness);
        this.fiveFactorList.add(extraversion);
        this.fiveFactorList.add(achievement);
        this.fiveFactorList.add(neuroticism);

        double obligingnessScore = obligingness.score / obligingness.total;
        double conscientiousnessScore = conscientiousness.score / conscientiousness.total;
        double extraversionScore = extraversion.score / extraversion.total;
        double achievementScore = achievement.score / achievement.total;
        double neuroticismScore = neuroticism.score / neuroticism.total;

        this.bigFivePersonality = new BigFivePersonality(obligingnessScore, conscientiousnessScore, extraversionScore,
                achievementScore, neuroticismScore);
    }

    public BigFivePersonality getBigFivePersonality() {
        return this.bigFivePersonality;
    }

    public void reset(double obligingness, double conscientiousness,
                      double extraversion, double achievement, double neuroticism) {
        this.fiveFactorList.get(0).score = obligingness;
        this.fiveFactorList.get(1).score = conscientiousness;
        this.fiveFactorList.get(2).score = extraversion;
        this.fiveFactorList.get(3).score = achievement;
        this.fiveFactorList.get(4).score = neuroticism;
        this.bigFivePersonality.reset(obligingness, conscientiousness, extraversion, achievement, neuroticism);
    }

    public MBTIFeature getMBTIFeature() {
        for (int i = 0; i < sBigFivePersonalities.length; ++i) {
            BigFivePersonality bigFivePersonality = sBigFivePersonalities[i];
            if (this.bigFivePersonality.getName().equals(bigFivePersonality.getName())) {
                return new MBTIFeature(sMBTIFeatures[i].getCode());
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        JSONArray fiveFactors = new JSONArray();
        for (FiveFactor factor : this.fiveFactorList) {
            fiveFactors.put(factor.toJSON());
        }
        json.put("fiveFactors", fiveFactors);

        json.put("bigFivePersonality", this.bigFivePersonality.toJSON());
        json.put("bigFiveFeature", this.bigFivePersonality.toJSON());

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public class FiveFactor {

        public BigFiveFactor bigFiveFactor;

        public double score = 0;

        public double total = 0;

        public FiveFactor(BigFiveFactor bigFiveFactor) {
            this.bigFiveFactor = bigFiveFactor;
        }

        public FiveFactor(BigFiveFactor bigFiveFactor, double score) {
            this.bigFiveFactor = bigFiveFactor;
            this.score = score;
            this.total = 1;
        }

        public FiveFactor(JSONObject json) {
            this.bigFiveFactor = BigFiveFactor.parse(json.getString("bigFive"));
            this.score = json.getDouble("score");
            this.total = json.getDouble("total");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bigFive", this.bigFiveFactor.code);
            json.put("score", this.score);
            json.put("total", this.total);
            return json;
        }
    }
}
