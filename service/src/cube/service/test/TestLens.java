package cube.service.test;

import cube.service.aigc.dataset.LensDataToolkit;
import cube.service.aigc.dataset.ReportDataset;

import java.io.File;

public class TestLens {

    public static void testDownloadDataset() {
        LensDataToolkit lensDataToolkit = new LensDataToolkit();

//        File file = new File("./storage/tmp/lens_reports.json");
//        lensDataset.downloadAllReports(file);

        File destFile = new File("storage/tmp/lens_export_reports.json");
        lensDataToolkit.downloadBySNList(new File("storage/tmp/lens_export_data.csv"), destFile);
    }

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

    public static void makeEvaluationDataset() {
        ReportDataset dataset = new ReportDataset();
        File reportDataFile = new File("storage/tmp/lens_export_reports.json");
        File scaleDataFile = new File("storage/tmp/lens_export_data.csv");
        File datasetFile = new File("storage/tmp/lens_dataset_1117.csv");

        try {
            dataset.makeEvaluationDatasetFromScaleData(reportDataFile, scaleDataFile, datasetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMakeNormalizationFile() {
        File file = new File("storage/tmp/lens_dataset_1030.csv");
        ReportDataset dataset = new ReportDataset();
        dataset.makeNormalizationFile(file);
    }

    public static void main(String[] args) {
//        testDownloadDataset();

//        testSaveScore();

//        testSaveVision();

//        testSaveVisionScore();

        makePaintingDataset();

//        makeEvaluationDataset();

//        testMakeNormalizationFile();

//        LensDataToolkit dataset = new LensDataToolkit();
//        dataset.makeNewExportData();
    }
}
