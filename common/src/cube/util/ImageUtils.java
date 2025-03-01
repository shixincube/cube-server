/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static BufferedImage rotateToLandscape(BufferedImage image) {
        if (image.getWidth() > image.getHeight()) {
            return image;
        }

        int newWidth = image.getHeight();
        int newHeight = image.getWidth();
        // 旋转 90
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(Math.toRadians(-90), newWidth / 2.0, newHeight / 2.0);

        BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = newImage.createGraphics();
        g2d.setTransform(affineTransform);
        g2d.drawImage(image, (newWidth - image.getWidth()) / 2, (newHeight - image.getHeight()) / 2, null);
        g2d.dispose();
        return newImage;
    }

    public static BufferedImage adjustAspectRatio(BufferedImage image) {
        double width = image.getWidth();
        double height = image.getHeight();
        double ratio = width / height;

        if (ratio > 1.63) {
            double newWidth = height * 1.63;
            double delta = ((width - newWidth) * 0.5);
            BufferedImage newImage = new BufferedImage((int)newWidth, (int)height, image.getType());
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(image, (int)(-delta), 0, null);
            g2d.dispose();
            return newImage;
        }

        return image;
    }

    public static BufferedImage resizeToDefault(BufferedImage image) {
        if (1280 == image.getWidth()) {
            return image;
        }

        double width = image.getWidth();
        double ratio = 1280 / width;
        double newWidth = width * ratio;
        double newHeight = image.getHeight() * ratio;

        BufferedImage newImage = new BufferedImage((int)newWidth, (int)newHeight, image.getType());
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(image.getScaledInstance((int)newWidth, (int)newHeight, Image.SCALE_AREA_AVERAGING),
                0, 0, null);
        g2d.dispose();
        return newImage;
    }

    public static void main(String[] args) {
        String filePath = "storage/tmp/painting_12.jpg";
        String outputPath = "storage/tmp/painting_12_output.jpg";

        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            BufferedImage output = ImageUtils.rotateToLandscape(image);
            output = ImageUtils.adjustAspectRatio(output);
            output = ImageUtils.resizeToDefault(output);
            ImageIO.write(output, "jpeg", new File(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
