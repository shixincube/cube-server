/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.Painting;
import cube.vision.Color;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class PaintingUtils {

    public final static Color[] ColorPalette = new Color[] {
            new Color(4, 42, 255),
            new Color(11, 219, 235),
            new Color(243, 243, 243),
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

    private PaintingUtils() {
    }

    public static void drawPredictView(File file, Painting painting) {
        try {
            BufferedImage image = ImageIO.read(file);
            Graphics2D g2d = image.createGraphics();


        } catch (Exception e) {
            Logger.e(PaintingUtils.class, "#drawPredictView", e);
        }
    }
}
