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

package cube.util;

import java.io.File;

/**
 * 帮助开发人员进行数据重置的工具。
 */
public final class ResetTool {

    private ResetTool() {

    }

    public static void resetContactStorage() {
        String path = ResetTool.guessPath();
        File file = new File(path, "ContactService.db");
        if (file.exists()) {
            file.delete();
            System.out.println("Delete file \"" + file.getAbsolutePath() + "\"");
        }
        else {
            System.out.println("No file \"" + file.getAbsolutePath() + "\"");
        }
    }

    public static void resetMessagingStorage() {
        String path = ResetTool.guessPath();
        File file = new File(path, "MessagingService.db");
        if (file.exists()) {
            file.delete();
            System.out.println("Delete file \"" + file.getAbsolutePath() + "\"");
        }
        else {
            System.out.println("No file \"" + file.getAbsolutePath() + "\"");
        }
    }

    public static void resetStoragePath() {
        ResetTool.resetContactStorage();
        ResetTool.resetMessagingStorage();
    }

    private static String guessPath() {
        String path = "storage/";
        File file = new File(path);
        if (file.isDirectory() && file.exists()) {
            return path;
        }

        path = "service/storage/";
        file = new File(path);
        if (file.isDirectory() && file.exists()) {
            return path;
        }

        return "";
    }

    public static void main(String[] args) {
        ResetTool.resetStoragePath();
    }
}
