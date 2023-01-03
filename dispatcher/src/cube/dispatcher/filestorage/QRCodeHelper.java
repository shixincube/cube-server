/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.dispatcher.filestorage;

import cell.util.Base64;
import cell.util.log.Logger;
import cube.util.CodeUtils;
import cube.vision.Color;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 二维码文件辅助操作函数库。
 */
public class QRCodeHelper {

    private QRCodeHelper() {
    }

    /**
     * 获取二维码图片的 Base64 数据。
     *
     * @param workPath
     * @param sharingCode
     * @param url
     * @return
     */
    public static String getQRCodeImageBase64(Path workPath, String sharingCode, String url) {
        boolean secure = url.indexOf("https://") == 0;
        String base64 = null;
        File file = new File(workPath.toFile(),
                sharingCode + "_" + (secure ? "s" : "n") + ".base64");
        if (!file.exists()) {
            // 创建二维码图片
            File qrCodeFile = new File(workPath.toFile(), sharingCode + "_" + (secure ? "s" : "n") + ".png");
            CodeUtils.generateQRCode(qrCodeFile, url,
                    300, 300, new Color(5, 5, 5));

            if (qrCodeFile.exists()) {
                byte[] data = null;
                try {
                    data = Files.readAllBytes(Paths.get(qrCodeFile.getAbsolutePath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 删除图片文件
                qrCodeFile.delete();

                // 将文件数据编码为 Base64
                base64 = Base64.encodeBytes(data);
                try {
                    Files.write(Paths.get(file.getAbsolutePath()),
                            base64.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (null == base64) {
            try {
                byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                base64 = new String(data, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return base64;
    }

    public static byte[] getQRCodeImageData(Path workPath, String sharingCode, String url) {
        String base64 = QRCodeHelper.getQRCodeImageBase64(workPath, sharingCode, url);
        byte[] data = null;
        try {
            data = Base64.decode(base64);
        } catch (IOException e) {
            Logger.w(QRCodeHelper.class, "#getQRCodeImageData", e);
        }
        return data;
    }
}
