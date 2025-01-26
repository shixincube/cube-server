/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP 操作实用函数。
 */
public class ZipUtils {

    private ZipUtils() {
    }

    /**
     * 将列表的文件添加到 ZIP 压缩包。
     *
     * @param fileList
     * @param outputStream
     */
    public static void toZip(List<File> fileList, OutputStream outputStream) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(outputStream);
            for (File file : fileList) {
                // 创建 Entry
                zos.putNextEntry(new ZipEntry(file.getName()));

                FileInputStream fis = null;
                byte[] buf = new byte[4096];
                int length = 0;
                try {
                    fis = new FileInputStream(file);
                    while ((length = fis.read(buf)) > 0) {
                        zos.write(buf, 0, length);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    if (null != fis) {
                        fis.close();
                    }
                }

                // 关闭 Entry
                zos.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != zos) {
                try {
                    zos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void compress(File sourceFile, ZipOutputStream outputStream, boolean keepDirStructure)
        throws IOException {
        byte[] buf = new byte[4096];

        String name = FileUtils.extractFileName(sourceFile.getName());

        if (sourceFile.isFile()) {
            FileInputStream fis = null;

            try {
                outputStream.putNextEntry(new ZipEntry(name));

                // 将文件数据写入 ZIP 流
                fis = new FileInputStream(sourceFile);
                int length = 0;
                while ((length = fis.read(buf)) > 0) {
                    outputStream.write(buf, 0, length);
                }

                outputStream.closeEntry();
            } catch (IOException e) {
                throw e;
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        else {
            File[] files = sourceFile.listFiles();
            if (null == files || files.length == 0) {
                if (keepDirStructure) {
                    outputStream.putNextEntry(new ZipEntry(name + "/"));
                    outputStream.closeEntry();
                }
            }
            else {
                for (File file : files) {
                    ZipUtils.compress(file, outputStream, keepDirStructure);
                }
            }
        }
    }
}
