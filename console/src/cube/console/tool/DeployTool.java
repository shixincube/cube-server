/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
