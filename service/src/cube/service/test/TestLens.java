package cube.service.test;

import cube.service.aigc.dataset.LensDataset;

import java.io.File;

public class TestLens {

    public static void testDownloadDataset() {
        LensDataset lensDataset = new LensDataset();

        File file = new File("./storage/tmp/lens_reports.json");
        lensDataset.writeToFile(file);
    }

    public static void testSave() {
        File srcFile = new File("./storage/tmp/lens_reports.json");
        File destFile = new File("./storage/tmp/lens_dataset.csv");

        LensDataset lensDataset = new LensDataset();
        lensDataset.saveAsScoreDataset(srcFile, destFile);
    }

    public static void main(String[] args) {
        testDownloadDataset();

//        testSave();
    }
}
