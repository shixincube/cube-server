/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.tool;

import cell.util.log.Logger;

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
        String root = null;
        for (String p : pathList) {
            path = new File(p);
            if (path.exists() && path.isDirectory()) {
                root = path.getAbsolutePath();
                break;
            }
        }

        if (null == root) {
            Logger.e(DeployTool.class, "#searchDeploySource - Can NOT find path");
            return null;
        }

        root = Paths.get(root).normalize().toString();

        Path cellJar = Paths.get(root, "bin/cell.jar");
        if (!Files.exists(cellJar)) {
            Logger.e(DeployTool.class, "#searchDeploySource - Can NOT find cell.jar");
            return null;
        }

        Path libsPath = Paths.get(root, "libs");
        File pathFile = new File(libsPath.toString());
        if (!pathFile.isDirectory()) {
            Logger.e(DeployTool.class, "#searchDeploySource - Can NOT find `libs/` path");
            return null;
        }

        boolean hasCommon = false;
        boolean hasDispatcher = false;
        boolean hasService = false;

        File[] files = pathFile.listFiles();
        if (null == files) {
            Logger.e(DeployTool.class, "#searchDeploySource - list files is NULL");
            return null;
        }
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
            Logger.e(DeployTool.class, "#searchDeploySource - Can NOT find path");
            return null;
        }

        return Paths.get(root);
    }
}
