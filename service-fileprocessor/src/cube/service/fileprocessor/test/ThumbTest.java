/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
