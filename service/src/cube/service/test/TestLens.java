package cube.service.test;

import cube.service.aigc.dataset.LensDataToolkit;
import cube.service.aigc.dataset.ReportDataset;

import java.io.File;

public class TestLens {

    public static void testSaveScore() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_score.csv");

        LensDataToolkit lensDataToolkit = new LensDataToolkit();
        lensDataToolkit.saveScoreDataset(srcFile, destFile);
    }

    public static void testSaveVision() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_vision.csv");

        LensDataToolkit lensDataToolkit = new LensDataToolkit();
        lensDataToolkit.saveVisionDataset(srcFile, destFile);
    }

    public static void testSaveVisionScore() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_dataset.csv");

        LensDataToolkit lensDataToolkit = new LensDataToolkit();
        lensDataToolkit.saveVisionScoreDataset(srcFile, destFile);
    }

    public static void makePaintingDataset() {
        ReportDataset dataset = new ReportDataset();
        File reportDataFile = new File("storage/tmp/lens_export_reports.json");
        File scaleDataFile = new File("storage/tmp/lens_export_data.csv");
        File datasetFile = new File("storage/tmp/lens_dataset_1116.csv");

        try {
            dataset.makePaintingDatasetFromScaleData(reportDataFile, scaleDataFile, datasetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMakeNormalizationFile() {
        File file = new File("storage/tmp/lens_dataset_1030.csv");
        ReportDataset dataset = new ReportDataset();
        dataset.makeNormalizationFile(file);
    }

    public static void downloadDataset() {
        LensDataToolkit lensDataToolkit = new LensDataToolkit();

//        File file = new File("./storage/tmp/lens_reports.json");
//        lensDataset.downloadAllReports(file);

        File destFile = new File("storage/tmp/lens_export_reports_p2.json");
        lensDataToolkit.downloadBySNList(new File("storage/tmp/lens_export_data_p2.csv"), destFile);
    }

    public static void makeEvaluationDataset() {
        ReportDataset dataset = new ReportDataset();
        File reportDataFile = new File("storage/tmp/lens_export_reports_all.json");
        File scaleDataFile = new File("storage/tmp/lens_export_data_all.csv");

        File datasetSCL = new File("storage/tmp/lens_dataset_scl.csv");
        File datasetPANAS = new File("storage/tmp/lens_dataset_panas.csv");
        File datasetBFP = new File("storage/tmp/lens_dataset_bfp.csv");

        try {
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_SCL,
                    reportDataFile, scaleDataFile, datasetSCL);
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_PANAS,
                    reportDataFile, scaleDataFile, datasetPANAS);
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_BFP,
                    reportDataFile, scaleDataFile, datasetBFP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mergeData() {
        File[] csvFiles = new File[] {
                new File("storage/tmp/lens_export_data_p1.csv"),
                new File("storage/tmp/lens_export_data_p2.csv")
        };
        File csvFile = new File("storage/tmp/lens_export_data_all.csv");

        File[] jsonFiles = new File[] {
                new File("storage/tmp/lens_export_reports_p1.json"),
                new File("storage/tmp/lens_export_reports_p2.json")
        };
        File jsonFile = new File("storage/tmp/lens_export_reports_all.json");

        LensDataToolkit lensDataToolkit = new LensDataToolkit();
        lensDataToolkit.mergeScaleCSV(csvFiles, csvFile);
        lensDataToolkit.mergeReportJSON(jsonFiles, jsonFile);
    }

    public static void main(String[] args) {
//        downloadDataset();

//        mergeData();

        makeEvaluationDataset();

//        testSaveScore();

//        testSaveVision();

//        testSaveVisionScore();

//        makePaintingDataset();

//        testMakeNormalizationFile();

//        LensDataToolkit dataset = new LensDataToolkit();
//        dataset.makeNewExportData();
    }
}
