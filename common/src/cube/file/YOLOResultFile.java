/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

import cube.common.JSONable;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YOLO 结果文件。
 */
public class YOLOResultFile implements JSONable {

    private File sourceFile;

    private File resultDir;
    private File resultFile;

    private Map<String, List<File>> resultObjects;

    public YOLOResultFile(File sourceFile, File resultDir) {
        this.sourceFile = sourceFile;
        this.resultDir = resultDir;
        this.resultFile = new File(resultDir, sourceFile.getName());
        this.resultObjects = new HashMap<>();
    }

    public void analysisResult() {
        File cropsDir = new File(this.resultDir, "crops");
        if (cropsDir.isDirectory()) {
            File[] files = cropsDir.listFiles();
            if (null == files || files.length == 0) {
                return;
            }

            for (File dir : files) {
                String name = dir.getName();

            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
