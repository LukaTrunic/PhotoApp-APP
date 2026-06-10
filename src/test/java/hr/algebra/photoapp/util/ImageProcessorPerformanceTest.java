package hr.algebra.photoapp.util;

import hr.algebra.photoapp.support.TestImageFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple before/after measurements for LO4 (profiling and performance improvement).
 * Run in IntelliJ and read the console output for presentation screenshots.
 */
@Tag("performance")
class ImageProcessorPerformanceTest {

    private static final int IMAGE_WIDTH = 800;
    private static final int IMAGE_HEIGHT = 600;
    private static final int RUNS = 10;

    @Test
    void sepiaOptimizedRunsFasterThanLegacyApproach() {
        BufferedImage image = createTestImage(IMAGE_WIDTH, IMAGE_HEIGHT);

        warmUp(image);

        long legacyNs = averageNanos(() -> applySepiaLegacy(image), RUNS);
        long optimizedNs = averageNanos(() -> ImageProcessor.applySepia(image), RUNS);

        System.out.printf("Sepia filter (%dx%d, %d runs)%n", IMAGE_WIDTH, IMAGE_HEIGHT, RUNS);
        System.out.printf("  Legacy (Color per pixel): %d ms%n", legacyNs / 1_000_000);
        System.out.printf("  Optimized (RGB bit ops):  %d ms%n", optimizedNs / 1_000_000);
        System.out.printf("  Improvement: %.1f%%%n",
                (1.0 - (double) optimizedNs / legacyNs) * 100);

        assertTrue(optimizedNs < legacyNs,
                "Optimized sepia should be faster than the legacy Color-based loop");
    }

    @Test
    void metadataReadUsesLessMemoryThanFullDecode() throws IOException {
        byte[] jpeg = TestImageFactory.minimalJpeg(IMAGE_WIDTH, IMAGE_HEIGHT);

        Runtime runtime = Runtime.getRuntime();
        gc();

        long beforeMetadata = usedMemory(runtime);
        Dimension size = ImageProcessor.getImageDimensions(jpeg);
        long metadataMemory = usedMemory(runtime) - beforeMetadata;

        gc();

        long beforeDecode = usedMemory(runtime);
        BufferedImage decoded = ImageProcessor.loadImage(jpeg);
        long decodeMemory = usedMemory(runtime) - beforeDecode;

        System.out.printf("Image dimensions via metadata: %dx%d%n", size.width, size.height);
        System.out.printf("  Approx. memory delta (metadata read): %d KB%n", metadataMemory / 1024);
        System.out.printf("  Approx. memory delta (full decode):   %d KB%n", decodeMemory / 1024);
        System.out.printf("  Decoded image size: %dx%d%n", decoded.getWidth(), decoded.getHeight());

        assertEquals(IMAGE_WIDTH, size.width);
        assertEquals(IMAGE_HEIGHT, size.height);
        assertTrue(metadataMemory <= decodeMemory,
                "Reading metadata should not allocate more than decoding the full image");
    }

    private static void warmUp(BufferedImage image) {
        for (int i = 0; i < 3; i++) {
            applySepiaLegacy(image);
            ImageProcessor.applySepia(image);
        }
    }

    private static long averageNanos(Runnable task, int runs) {
        long total = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            task.run();
            total += System.nanoTime() - start;
        }
        return total / runs;
    }

    private static BufferedImage createTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new GradientPaint(0, 0, Color.RED, width, height, Color.BLUE));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    /** Previous implementation kept for LO4 before/after comparison in tests. */
    private static BufferedImage applySepiaLegacy(BufferedImage original) {
        BufferedImage sepia = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color color = new Color(original.getRGB(x, y));
                int tr = Math.min(255, (int) (0.393 * color.getRed() + 0.769 * color.getGreen() + 0.189 * color.getBlue()));
                int tg = Math.min(255, (int) (0.349 * color.getRed() + 0.686 * color.getGreen() + 0.168 * color.getBlue()));
                int tb = Math.min(255, (int) (0.272 * color.getRed() + 0.534 * color.getGreen() + 0.131 * color.getBlue()));
                sepia.setRGB(x, y, new Color(tr, tg, tb).getRGB());
            }
        }
        return sepia;
    }

    private static void gc() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static long usedMemory(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
