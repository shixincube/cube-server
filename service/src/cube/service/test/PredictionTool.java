package cube.service.test;

import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Reference;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.FactorSet;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.List;

public class PredictionTool {

    public PredictionTool() {

    }

    public PaintingReport read(String filepath) {
        JSONObject json = ConfigUtils.readJsonFile(filepath);
        PaintingReport report = new PaintingReport(json);
        return report;
    }

    public void printIndicator(PaintingReport report) {
        System.out.print(report.getEvaluationReport().getReference() == Reference.Normal ? "1" : "0");
        System.out.print(",");

        List<EvaluationScore> list = report.getEvaluationReport().getFullEvaluationScores();
        for (EvaluationScore score : list) {
//            System.out.println(score.indicator.name + " : " + score.calcScore());
            System.out.print(score.calcScore());
            System.out.print(",");
        }

        FactorSet factorSet = report.getEvaluationReport().getFactorSet();
        System.out.print(factorSet.symptomFactor.somatization);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.obsession);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.interpersonal);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.depression);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.anxiety);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.hostile);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.horror);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.paranoid);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.psychosis);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.sleepDiet);
        System.out.print(",");
        System.out.print(factorSet.symptomFactor.total);
    }

    public static void main(String[] args) {
        PredictionTool tool = new PredictionTool();

        String filepath = "/Users/ambrose/Public/年度工作/2025年/11月/志愿者数据/json/076.json";
        PaintingReport report = tool.read(filepath);
        tool.printIndicator(report);
    }
}
