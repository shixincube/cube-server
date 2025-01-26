/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.test;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ThumbTest {

    public static void main(String[] args) {
        String path = "/Users/ambrose/Documents/Repositories/Cube3/cube-server/service/storage/files/xwwwwrLABQBGvNuxuvHFrzxRoxFAuIGzrxwwwwmNkRPMiKOxaPZJJZPaxOKiMPRk";

        String output = "/Users/ambrose/Documents/Repositories/Cube3/cube-server/service/storage/tmp/ABC";

        File file = new File(path);

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);

            Thumbnails.Builder<? extends InputStream> fileBuilder = Thumbnails.of(fis).
                    scale(0.5).outputQuality(1.0).outputFormat("jpg");

            fileBuilder.toFile(output);

            System.out.println("Size: " + (new File(output + ".jpg")).length());

//            BufferedImage src = fileBuilder.asBufferedImage();

//            System.out.println("width : " + src.getWidth());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
