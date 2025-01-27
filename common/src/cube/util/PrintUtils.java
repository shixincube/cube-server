/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cube.vision.Size;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 打印实用函数。
 */
public class PrintUtils {

    public final static Size PaperA4Ultra = new Size(3508, 2480);

    public final static Size PaperA4Normal = new Size(1754, 1240);

    public final static Size PaperA4Small = new Size(1402, 992);

    public static BufferedImage createPaper(Size size) {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, size.width, size.height);
        graphics.dispose();
        return image;
    }

    public static BufferedImage createPaper(Size size, BufferedImage content, int x, int y) {
        BufferedImage image = PrintUtils.createPaper(size);
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(content, x, y, content.getWidth(), content.getHeight(), null);
        g2d.dispose();
        return image;
    }
}
