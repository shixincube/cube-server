/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.test;

import cube.aigc.psychology.PaintingReport;
import org.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestPanorama {

    private static String sReportDataFile = "./storage/tmp/lens_export_reports_all.json";

    public static void testGeneratePanorama() {
        List<PaintingReport> list = new ArrayList<>();
        try {
            byte[] data = Files.readAllBytes(Paths.get(sReportDataFile));
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        testGeneratePanorama();
    }
}
