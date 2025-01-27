package cube.service.test;

import cube.service.aigc.scene.PaintingUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class TestPainting {

    public static void main(String[] args) {
        File file4RotateToLandscape = new File("/Users/ambrose/Documents/Repositories/baize/test/data/painting_20250113_6.jpg");
        File file4AdjustAspectRatio = new File("/Users/ambrose/Documents/Repositories/baize/test/data/painting_29.jpg");
        File file4ResizeToDefault = new File("/Users/ambrose/Documents/Repositories/baize/test/data/painting_01.jpg");

        File output = new File("storage/tmp/TestPainting.jpg");

        try {
            BufferedImage image = ImageIO.read(file4ResizeToDefault);

//            BufferedImage result = PaintingUtils.rotateToLandscape(image);
//            BufferedImage result = PaintingUtils.adjustAspectRatio(image);
            BufferedImage result = PaintingUtils.resizeToDefault(image);

            ImageIO.write(result, "JPEG", output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
