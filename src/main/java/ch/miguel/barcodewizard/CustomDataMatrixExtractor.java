package ch.miguel.barcodewizard;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;

public class CustomDataMatrixExtractor {

    /**
     * Reads a TIFF byte array and processes it to extract Data Matrix content.
     *
     * @param tiffByteArray The TIFF image as a byte array.
     * @return The decoded Data Matrix content, or null if decoding fails.
     */
    public String extractDataMatrix(byte[] tiffByteArray) {
        try {
            // Step 1: Decode TIFF image from byte array
            BufferedImage image = Imaging.getBufferedImage(new ByteArrayInputStream(tiffByteArray));

            // Step 2: Preprocess the image (grayscale, binarization, etc.)
            BufferedImage processedImage = preprocessImage(image);

            // Step 3: Locate the Data Matrix in the image
            var locator = new DataMatrixLocator();
            DataMatrixRegion region = locator.locateDataMatrix(processedImage);
            
            if (region == null) {
                System.out.println("No Data Matrix found.");
                return null;
            }
            region.setImage(processedImage);
            
            // Step 4: Decode the Data Matrix
            DataMatrixDecoder decoder = new DataMatrixDecoder();
            String data = decoder.decode(region);

            return data;

        } catch (ImageReadException | IOException e) {
            System.err.println("Error reading the TIFF image: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing the image: " + e.getMessage());
        }
        return null;
    }

    private BufferedImage preprocessImage(BufferedImage image) {
        // Step 1: Convert to grayscale
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // Step 2: Apply Gaussian blur for noise reduction
        BufferedImage blurredImage = applyGaussianBlur(grayImage);

        // Step 3: Binarize the image using adaptive thresholding
        BufferedImage binaryImage = binarizeImageAdaptive(blurredImage);

        saveImage(grayImage, "grayImage.png");
        saveImage(blurredImage, "blurredImage.png");
        saveImage(binaryImage, "binaryImage.png");

        return binaryImage;
    }

    private BufferedImage applyGaussianBlur(BufferedImage image) {
        // Use a simple box blur or implement a Gaussian kernel
        int[][] kernel = {
            {1, 4, 6, 4, 1},
            {4, 16, 24, 16, 4},
            {6, 24, 36, 24, 6},
            {4, 16, 24, 16, 4},
            {1, 4, 6, 4, 1}
        };
        int kernelWeight = 256; // Sum of all kernel values

        BufferedImage blurredImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        for (int x = 2; x < image.getWidth() - 2; x++) {
            for (int y = 2; y < image.getHeight() - 2; y++) {
                int sum = 0;

                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        int pixel = new Color(image.getRGB(x + i, y + j)).getRed();
                        sum += kernel[i + 2][j + 2] * pixel;
                    }
                }

                int blurredValue = sum / kernelWeight;
                Color blurredColor = new Color(blurredValue, blurredValue, blurredValue);
                blurredImage.setRGB(x, y, blurredColor.getRGB());
            }
        }
        return blurredImage;
    }

    private BufferedImage binarizeImageAdaptive(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        int blockSize = 15; // Size of the neighborhood
        int c = 10; // Constant to subtract from mean
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int mean = calculateLocalMean(image, x, y, blockSize);
                int grayValue = new Color(image.getRGB(x, y)).getRed();
                int binaryValue = grayValue > (mean - c) ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                binaryImage.setRGB(x, y, binaryValue);
            }
        }
        return binaryImage;
    }

    private int calculateLocalMean(BufferedImage image, int x, int y, int blockSize) {
        int sum = 0, count = 0;
        for (int i = -blockSize / 2; i <= blockSize / 2; i++) {
            for (int j = -blockSize / 2; j <= blockSize / 2; j++) {
                int nx = x + i, ny = y + j;
                if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                    sum += new Color(image.getRGB(nx, ny)).getRed();
                    count++;
                }
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private void saveImage(BufferedImage image, String filename) {
        try {
            File output = new File(filename);
            ImageIO.write(image, "png", output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

