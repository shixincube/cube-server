/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2025 Ambrose Xu.
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
