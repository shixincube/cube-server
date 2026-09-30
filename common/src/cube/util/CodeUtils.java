/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.log.Logger;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import cube.vision.Color;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
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
            Logger.e(CodeUtils.class, "#extractURLLastPath - Malformed URL", e);
        }
        return path;
    }

    /**
     * 生成二维码图片。
     *
     * @param output
     * @param content
     * @param imageWidth
     * @param imageHeight
     * @param color
     * @return
     */
    public static boolean generateQRCode(File output, String content,
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

            String ext = FileUtils.extractFileExtension(output.getName());
            ImageIO.write(image, ext, output);
        } catch (WriterException e) {
            Logger.e(CodeUtils.class, "#generateQRCode - writer error", e);
            return false;
        } catch (IOException e) {
            Logger.e(CodeUtils.class, "#generateQRCode - I/O error", e);
            return false;
        }

        return true;
    }

    /**
     * 生成条形码图片。
     *
     * @param output
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static boolean generateBarCode(File output, String data, int width, int height) {
        try {
            BufferedImage image = CodeUtils.generateBarCode(data, width, height, null, null, 0);
            String ext = FileUtils.extractFileExtension(output.getName());
            ImageIO.write(image, ext, output);
        } catch (Exception e) {
            Logger.e(CodeUtils.class, "#generateBarCode - Unhandled exception", e);
            return false;
        }
        return true;
    }

    /**
     * 生成条形码。
     *
     * @param data
     * @param width
     * @param height
     * @param header
     * @param footer
     * @param fontSize
     * @return
     */
    public static BufferedImage generateBarCode(String data, int width, int height, String header, String footer, int fontSize) {
        BufferedImage image = null;
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, width, height, hints);

            int hOffset = fontSize * 2 + 10;
            int bHeight = height - hOffset;

            int barX = 0;

            // 创建图像
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (y <= hOffset || y > bHeight) {
                        image.setRGB(x, y, 0xFFFFFF);
                    }
                    else {
                        // 黑白条
                        image.setRGB(x, y, bitMatrix.get(x, y) ? 0 : 0xFFFFFF);
                        if (barX == 0 && bitMatrix.get(x, y)) {
                            barX = x;
                        }
                    }
                }
            }

            Graphics2D g2d = (Graphics2D) image.getGraphics();

            Font font = null;
            try {
//                font = Font.createFont(Font.TRUETYPE_FONT, new File("assets/SIMHEI.TTF"));
//                font = font.deriveFont(Font.PLAIN, fontSize);
                font = new Font("SimHei", Font.PLAIN, fontSize);
            } catch (Exception e) {
                Logger.e(CodeUtils.class, "#generateBarCode", e);
            }

            if (null != header) {
                // 字体抗锯齿
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setFont(font);
                g2d.setColor(java.awt.Color.BLACK);
                g2d.drawString(header, barX, fontSize + (int)(fontSize * 0.4) + 5);
            }
            if (null != footer) {
                // 字体抗锯齿
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setFont(font);
                g2d.setColor(java.awt.Color.BLACK);
                g2d.drawString(footer, barX, height - (int)(fontSize * 0.5) - 5);
            }
        } catch (Exception e) {
            Logger.e(CodeUtils.class, "#generateBarCode - Unhandled exception", e);
        }
        return image;
    }

    /**
     * 扫描图中条形码数据。
     *
     * @param input
     * @return
     */
    public static String scanBarCode(File input) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(input);
            BufferedImage image = ImageIO.read(fis);
            if (null == image) {
                return null;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (Exception e) {
            Logger.e(CodeUtils.class, "#scanBarCode - Unhandled exception", e);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // 释放失败不影响主流程，仅记录
                    Logger.d(CodeUtils.class, "#scanBarCode - Failed to close 'fis': " + e.getMessage());
                }
            }
        }

        return null;
    }

    public static void main(String[] args) {


        String data = "532201-0703-0011";

        try {
            int width = 500;
            int height = 200;


            int offsetX = PrintUtils.PaperA4Ultra.width - width;
            int offsetY = PrintUtils.PaperA4Ultra.height - height - 10;
            BufferedImage code = CodeUtils.generateBarCode(data, width, height,
                    "曲靖市第一中学", "高2011班    张伟", 32);
            BufferedImage paper = PrintUtils.createPaper(PrintUtils.PaperA4Ultra, code, offsetX, offsetY);
            File paperFile = new File("service/storage/tmp/paper-demo.jpg");
            ImageIO.write(paper, "jpg", paperFile);
        } catch (Exception e) {
            Logger.e(CodeUtils.class, "#main - Unhandled exception", e);
        }

    }
}
