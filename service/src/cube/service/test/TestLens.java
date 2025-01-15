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

        File destFile = new File("storage/tmp/lens_export_reports_p4.json");
        lensDataToolkit.downloadBySNList(new File("storage/tmp/lens_export_data_p4.csv"), destFile);
    }

    public static void remakeEvaluationDataset() {
        ReportDataset dataset = new ReportDataset();
        File reportDataFile = new File("storage/tmp/lens_export_reports_2025.json");
        File scaleDataFile = new File("storage/tmp/lens_export_data_2025.csv");

        File datasetSCL = new File("storage/tmp/lens_dataset_scl_2025.csv");
        File datasetPANAS = new File("storage/tmp/lens_dataset_panas_2025.csv");
        File datasetBFP = new File("storage/tmp/lens_dataset_bfp_2025.csv");

        try {
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_SCL,
                    reportDataFile, scaleDataFile, datasetSCL, true);
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_PANAS,
                    reportDataFile, scaleDataFile, datasetPANAS, true);
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_BFP,
                    reportDataFile, scaleDataFile, datasetBFP, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void makeEvaluationDataset() {
        ReportDataset dataset = new ReportDataset();
        File reportDataFile = new File("storage/tmp/lens_export_reports_2025_updated.json");
        File scaleDataFile = new File("storage/tmp/lens_export_data_2025.csv");

        File datasetSCL = new File("storage/tmp/lens_dataset_scl_2025.csv");
        File datasetPANAS = new File("storage/tmp/lens_dataset_panas_2025.csv");
        File datasetBFP = new File("storage/tmp/lens_dataset_bfp_2025.csv");

        try {
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_SCL,
                    reportDataFile, scaleDataFile, datasetSCL, false);
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_PANAS,
                    reportDataFile, scaleDataFile, datasetPANAS, false);
            dataset.makeEvaluationDatasetFromScaleData(ReportDataset.OUTPUT_BFP,
                    reportDataFile, scaleDataFile, datasetBFP, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void splitTrainTestDataset() {
        ReportDataset dataset = new ReportDataset();

        File sourceSCL = new File("storage/tmp/lens_dataset_scl_2025.csv");
        File trainSCL = new File("storage/tmp/lens_dataset_scl_2025_train.csv");
        File testSCL = new File("storage/tmp/lens_dataset_scl_2025_test.csv");
        dataset.splitTrainTestDataset(sourceSCL, trainSCL, testSCL, 0.1f);

        File sourcePANAS = new File("storage/tmp/lens_dataset_panas_2025.csv");
        File trainPANAS = new File("storage/tmp/lens_dataset_panas_2025_train.csv");
        File testPANAS = new File("storage/tmp/lens_dataset_panas_2025_test.csv");
        dataset.splitTrainTestDataset(sourcePANAS, trainPANAS, testPANAS, 0.1f);

        File sourceBFP = new File("storage/tmp/lens_dataset_bfp_2025.csv");
        File trainBFP = new File("storage/tmp/lens_dataset_bfp_2025_train.csv");
        File testBFP = new File("storage/tmp/lens_dataset_bfp_2025_test.csv");
        dataset.splitTrainTestDataset(sourceBFP, trainBFP, testBFP, 0.1f);
    }

    public static void mergeData() {
        File[] csvFiles = new File[] {
                new File("storage/tmp/lens_export_data_p1.csv"),
                new File("storage/tmp/lens_export_data_p2.csv"),
                new File("storage/tmp/lens_export_data_p3.csv"),
                new File("storage/tmp/lens_export_data_p4.csv")
        };
        File mergedCSVvFile = new File("storage/tmp/lens_export_data_2025.csv");

        File[] jsonFiles = new File[] {
                new File("storage/tmp/lens_export_reports_p1.json"),
                new File("storage/tmp/lens_export_reports_p2.json"),
                new File("storage/tmp/lens_export_reports_p3.json"),
                new File("storage/tmp/lens_export_reports_p4.json")
        };
        File mergedJsonFile = new File("storage/tmp/lens_export_reports_2025.json");

        LensDataToolkit lensDataToolkit = new LensDataToolkit();
        lensDataToolkit.mergeScaleCSV(csvFiles, mergedCSVvFile);
        lensDataToolkit.mergeReportJSON(jsonFiles, mergedJsonFile);
    }

    public static void main(String[] args) {
//        downloadDataset();

//        mergeData();

//        remakeEvaluationDataset();

//        makeEvaluationDataset();

        splitTrainTestDataset();

//        makePaintingDataset();

//        LensDataToolkit toolkit = new LensDataToolkit();
//        File file = new File("storage/tmp/lens_export_reports_2025.json");
//        File newFile = new File("storage/tmp/lens_export_reports_2025_updated.json");
//        toolkit.updateReportPainting(file, newFile);

//        testSaveScore();

//        testSaveVision();

//        testSaveVisionScore();
    }
}
