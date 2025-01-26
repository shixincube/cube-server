/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.tool;

import cube.util.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 部署工具。
 */
public final class DeployTool {

    public final static String[] CONSOLE_PROP_FILES = new String[] {
            "console_dev.properties",
            "console.properties",
            "config/console.properties",
            "deploy/config/console.properties"
    };

    private DeployTool() {
    }

    public static Path searchDeploySource() {
        String[] pathList = new String[] {
                "deploy",
                "../deploy",
                "../server"
        };

        File path = null;
        String pathString = null;
        for (String p : pathList) {
            path = new File(p);
            if (path.exists() && path.isDirectory()) {
                pathString = path.getAbsolutePath();
                break;
            }
        }

        if (null == pathString) {
            return null;
        }

        pathString = FileUtils.fixFilePath(pathString);

        Path cellJar = Paths.get(pathString, "bin/cell.jar");
        if (!Files.exists(cellJar)) {
            return null;
        }

        Path libsPath = Paths.get(pathString, "libs");
        File pathFile = new File(libsPath.toString());
        if (!pathFile.isDirectory()) {
            return null;
        }

        boolean hasCommon = false;
        boolean hasDispatcher = false;
        boolean hasService = false;

        File[] files = pathFile.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("cube-common")) {
                hasCommon = true;
            }
            else if (file.getName().startsWith("cube-dispatcher")) {
                hasDispatcher = true;
            }
            else if (file.getName().startsWith("cube-service")) {
                hasService = true;
            }
        }

        if (!hasCommon || !hasDispatcher || !hasService) {
            return null;
        }

        return Paths.get(pathString);
    }
}
