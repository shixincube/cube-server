/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2023 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import cube.vision.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 编码串辅助函数库。
 */
public class CodeUtils {

    private CodeUtils() {
    }

    public static String extractProtocol(String codeString) {
        byte[] bytes = codeString.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[bytes.length];
        int length = 0;
        for (int i = 0; i < bytes.length; ++i) {
            if (bytes[i] == ':') {
                break;
            }

            buf[i] = bytes[i];
            ++length;
        }
        return new String(buf, 0, length, StandardCharsets.UTF_8);
    }

    public static String[] extractCubeResourceSegments(String codeString) {
        int index = codeString.indexOf("//");
        if (index < 0) {
            return null;
        }

        String string = codeString.substring(index + 2);
        return string.split("\\.");
    }

    public static boolean isBoxDomain(String codeString) {
        try {
            URL url = new URL(codeString);
            if (url.getHost().equalsIgnoreCase("box.shixincube.com")) {
                if (url.getPath().startsWith("/box")) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    public static String extractBoxDomain(String codeString) {
        return extractURLLastPath(codeString);
    }

    public static String extractURLLastPath(String codeString) {
        String path = null;
        try {
            URL url = new URL(codeString);
            path = url.getPath().trim();
            int index = path.lastIndexOf("/");
            path = path.substring(index + 1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * 生成二维码图片。
     *
     * @param outputFile
     * @param content
     * @param imageWidth
     * @param imageHeight
     * @param color
     * @return
     */
    public static boolean generateQRCode(File outputFile, String content,
                                         int imageWidth, int imageHeight, Color color) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // 设置内容编码
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // 设置图片间隙
        hints.put(EncodeHintType.MARGIN, 1);
        // 设置纠错级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, imageWidth, imageHeight, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();

            // 缓存图片
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    int rgb = matrix.get(x, y) ? color.color() : 0xFFFFFF;
                    image.setRGB(x, y, rgb);
                }
            }

            String ext = FileUtils.extractFileExtension(outputFile.getName());
            ImageIO.write(image, ext, outputFile);
        } catch (WriterException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        String string = "https://box.shixincube.com/box/first-prototype-box";
        String protocol = CodeUtils.extractProtocol(string);
        System.out.println("Protocol: " + protocol);

        if (protocol.equals("cube")) {
            String[] segments = CodeUtils.extractCubeResourceSegments(string);
            System.out.println("Segment: " + segments[0]);
            System.out.println("Segment: " + segments[1]);
        }
        else {
            System.out.println("Box: " + CodeUtils.isBoxDomain(string));
            System.out.println("Domain: " + CodeUtils.extractBoxDomain(string));
        }

//        File qrFile = new File("service/storage/tmp/qrcode-yyzj.jpg");
//        boolean success = CodeUtils.generateQRCode(qrFile, string, 400, 400,
//                new Color("#000000"));
//        System.out.println("Generate QRCode - " + success);
    }
}
