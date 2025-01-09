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

    public final static Size PagerA4 = new Size(3508, 2479);

    public static BufferedImage createA4Pager() {
        BufferedImage image = new BufferedImage(PagerA4.width, PagerA4.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, PagerA4.width, PagerA4.height);
        graphics.dispose();
        return image;
    }

    public static BufferedImage createA4Pager(BufferedImage content, int x, int y) {
        BufferedImage image = PrintUtils.createA4Pager();
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(content, x, y, content.getWidth(), content.getHeight(), null);
        g2d.dispose();
        return image;
    }
}
