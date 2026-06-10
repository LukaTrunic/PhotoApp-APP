package hr.algebra.photoapp.support;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class TestImageFactory {

    private TestImageFactory() {
    }

    public static byte[] minimalJpeg() throws IOException {
        return minimalJpeg(1, 1);
    }

    public static byte[] minimalJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
