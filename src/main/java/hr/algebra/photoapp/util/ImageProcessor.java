package hr.algebra.photoapp.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;


// Utility/Helper Pattern + Strategy Pattern (different filter methods)
// Provides image processing capabilities (resize, filters, format conversion)
public class ImageProcessor {

    public static BufferedImage resize(BufferedImage original, int width, int height) {
        if (width <= 0 && height <= 0) {
            return original;
        }

        if (width <= 0) {
            width = (int) (original.getWidth() * ((double) height / original.getHeight()));
        }
        if (height <= 0) {
            height = (int) (original.getHeight() * ((double) width / original.getWidth()));
        }

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();

        return resized;
    }

    public static BufferedImage applySepia(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage sepia = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int tr = Math.min(255, (int) (0.393 * r + 0.769 * g + 0.189 * b));
                int tg = Math.min(255, (int) (0.349 * r + 0.686 * g + 0.168 * b));
                int tb = Math.min(255, (int) (0.272 * r + 0.534 * g + 0.131 * b));

                sepia.setRGB(x, y, (tr << 16) | (tg << 8) | tb);
            }
        }

        return sepia;
    }

    public static BufferedImage applyBlur(BufferedImage original) {
        float[] matrix = {
                1f/9f, 1f/9f, 1f/9f,
                1f/9f, 1f/9f, 1f/9f,
                1f/9f, 1f/9f, 1f/9f
        };

        Kernel kernel = new Kernel(3, 3, matrix);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        return op.filter(original, null);
    }

    public static byte[] convertFormat(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String outputFormat = format.toLowerCase();
        if (!outputFormat.equals("jpg") && !outputFormat.equals("jpeg") &&
            !outputFormat.equals("png") && !outputFormat.equals("bmp")) {
            outputFormat = "jpg";
        }

        if (outputFormat.equals("jpg") || outputFormat.equals("jpeg")) {
            BufferedImage rgbImage = new BufferedImage(
                    image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = rgbImage;
        }

        ImageIO.write(image, outputFormat, baos);
        return baos.toByteArray();
    }

    public static BufferedImage loadImage(byte[] imageData) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(imageData));
    }

    public static BufferedImage applyFilters(BufferedImage original,
                                            Integer width, Integer height,
                                            boolean sepia, boolean blur) {
        BufferedImage processed = original;

        if (width != null && width > 0 || height != null && height > 0) {
            int w = width != null ? width : 0;
            int h = height != null ? height : 0;
            processed = resize(processed, w, h);
        }

        if (sepia) {
            processed = applySepia(processed);
        }
        if (blur) {
            processed = applyBlur(processed);
        }

        return processed;
    }

    /**
     * Reads width/height from image metadata without decoding the full pixel buffer.
     */
    public static Dimension getImageDimensions(byte[] imageData) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }
}
