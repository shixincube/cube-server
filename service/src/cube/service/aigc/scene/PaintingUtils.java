/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.common.entity.Material;
import cube.vision.Color;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.List;

public class PaintingUtils {

    public final static Color[] ColorPalette = new Color[] {
            new Color(4, 42, 255),
            new Color(11, 219, 235),
            new Color(102, 178, 255),
            new Color(0, 223, 183),
            new Color(17, 31, 104),
            new Color(255, 111, 221),
            new Color(255, 68, 79),
            new Color(204, 237, 0),
            new Color(0, 243, 68),
            new Color(189, 0, 255),
            new Color(0, 180, 255),
            new Color(221, 0, 186),
            new Color(0, 255, 255),
            new Color(38, 192, 0),
            new Color(1, 255, 179),
            new Color(125, 36, 255),
            new Color(123, 0, 104),
            new Color(255, 27, 108),
            new Color(252, 109, 47),
            new Color(162, 255, 11)
    };

    private static int ColorIndex = 0;

    private static Font DefaultFont = null;

    static {
        try {
//            DefaultFont = Font.createFont(Font.TRUETYPE_FONT, new File("assets/SIMHEI.TTF"));
            DefaultFont = new Font("Courier", Font.PLAIN, 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PaintingUtils() {
    }

    public static void drawMaterial(File rawFile, List<Material> materials, boolean bbox, boolean vparam, File outputFile) {
        try {
            BufferedImage image = ImageIO.read(rawFile);

            // 转横版
            image = rotateToLandscape(image);
            // 修正纵横比
            image = adjustAspectRatio(image);
            // 设定图像大小
            image = resizeToDefault(image);

            int fontSize = 16;

            Graphics2D g2d = image.createGraphics();
            Stroke stroke = new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2d.setStroke(stroke);

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(DefaultFont.deriveFont(Font.PLAIN, fontSize));

            for (Material material : materials) {
                g2d.setColor(peekColor());

                if (bbox) {
                    g2d.drawRect(material.boundingBox.x, material.boundingBox.y,
                            material.boundingBox.width, material.boundingBox.height);
                }

                if (vparam) {
                    drawVisionParam(g2d, material, fontSize);
                }
            }
            g2d.dispose();

            ImageIO.write(image, "JPEG", outputFile);
        } catch (Exception e) {
            Logger.e(PaintingUtils.class, "#drawMaterialBox", e);
        }
    }

    private static synchronized java.awt.Color peekColor() {
        Color clr = ColorPalette[ColorIndex];
        java.awt.Color color = new java.awt.Color(clr.red(), clr.green(), clr.blue());
        ++ColorIndex;
        if (ColorIndex >= ColorPalette.length) {
            ColorIndex = 0;
        }
        return color;
    }

    private static void drawVisionParam(Graphics2D g2d, Material material, int fontSize) {
        int x = material.boundingBox.x;
        int y = material.boundingBox.y;
        int xOffset = 4;
        int yOffset = 4 + fontSize;

        g2d.drawString(String.format("T-Max: %.2f", material.texture.max), x + xOffset, y + yOffset);
        g2d.drawString(String.format("T-Avg: %.2f", material.texture.avg), x + xOffset, y + yOffset * 2);
        g2d.drawString(String.format("Hierarchy: %.2f", material.texture.hierarchy), x + xOffset, y + yOffset * 3);
        g2d.drawString(String.format("Density: %.2f", material.texture.density), x + xOffset, y + yOffset * 4);
    }

    public static BufferedImage rotateToLandscape(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (height > width) {
            int newWidth = height;
            int newHeight = width;
            // 逆时针旋转 90
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(Math.toRadians(-90), newWidth / 2.0, newHeight / 2.0);

            BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
            Graphics2D g2d = newImage.createGraphics();
            g2d.setTransform(affineTransform);
            g2d.drawImage(image, (newWidth - image.getWidth()) / 2, (newHeight - image.getHeight()) / 2, null);
            g2d.dispose();
            return newImage;
        }
        else {
            return image;
        }
    }

    public static BufferedImage adjustAspectRatio(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double ratio = (double) width / (double) height;

        if (ratio > 1.63) {
            int newWidth = (int) Math.round((double) height * 1.63d);
            int newHeight = height;
            int x = (int) Math.round((double)(width - newWidth) * 0.5);

            BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(image, -x, 0, new ImageObserver() {
                @Override
                public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                    return false;
                }
            });
            g2d.dispose();
            return newImage;
        }
        else {
            return image;
        }
    }

    public static BufferedImage resizeToDefault(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = 1280;
        int newHeight = 960;
        if (width / height >= newWidth / newHeight) {
            newHeight = Math.round((float)height * (float)newWidth / (float)width);
        }
        else {
            newWidth = Math.round((float)width * (float)newHeight / (float)height);
        }

        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = newImage.createGraphics();
        Image img = image.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return newImage;
    }
}
