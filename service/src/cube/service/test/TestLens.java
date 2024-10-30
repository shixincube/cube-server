package cube.service.test;

import cube.service.aigc.dataset.LensDataset;
import cube.service.aigc.dataset.ReportDataset;

import java.io.File;

public class TestLens {

    public static void testDownloadDataset() {
        LensDataset lensDataset = new LensDataset();

//        File file = new File("./storage/tmp/lens_reports.json");
//        lensDataset.downloadAllReports(file);

        File destFile = new File("storage/tmp/lens_export_reports.json");
        lensDataset.downloadBySNList(new File("storage/tmp/lens_export_data.csv"), destFile);
    }

    public static void testSaveScore() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_score.csv");

        LensDataset lensDataset = new LensDataset();
        lensDataset.saveScoreDataset(srcFile, destFile);
    }

    public static void testSaveVision() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_vision.csv");

        LensDataset lensDataset = new LensDataset();
        lensDataset.saveVisionDataset(srcFile, destFile);
    }

    public static void testSaveVisionScore() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_dataset.csv");

        LensDataset lensDataset = new LensDataset();
        lensDataset.saveVisionScoreDataset(srcFile, destFile);
    }

    public static void testMakeDataset() {
        ReportDataset dataset = new ReportDataset();
        File reportDataFile = new File("storage/tmp/lens_export_reports.json");
        File scaleDataFile = new File("storage/tmp/lens_export_data.csv");
        File datasetFile = new File("storage/tmp/lens_dataset_1030.csv");

        try {
            dataset.makeDatasetFromScaleData(reportDataFile, scaleDataFile, datasetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        testDownloadDataset();

//        testSaveScore();

//        testSaveVision();

//        testSaveVisionScore();

        testMakeDataset();
    }
}
