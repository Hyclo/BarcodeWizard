package ch.miguel.barcodewizard;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class DataMatrixDecoder {
    public String decode(DataMatrixRegion region) {
        try {
            // Step 1: Extract pixel data
            Rectangle boundingBox = region.getBoundingBox();

            // Step 2: Crop the image to the region
            BufferedImage dataMatrixImage = region.getImage().getSubimage(
                boundingBox.x + 1, boundingBox.y + 1, boundingBox.width - 1, boundingBox.height - 1
            );
            saveImage(dataMatrixImage, "dataMatrixImage.png");

            DataMatrixRedrawer redrawer = new DataMatrixRedrawer();
            BufferedImage cleanedImage = redrawer.redrawDataMatrix(dataMatrixImage, gridSize(dataMatrixImage));

            // Save the new image for verification
            saveImage(cleanedImage, "perfectDataMatrix.png");

            // Step 2: Analyze the grid
            boolean[][] grid = analyzeGrid(cleanedImage);

            // Step 3: Decode binary data
            String decodedData = decodeGrid(grid);

            return decodedData;

        } catch (Exception e) {
            System.err.println("Error decoding Data Matrix: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private boolean[][] analyzeGrid(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Estimate grid size based on image dimensions
        int gridSize = gridSize(image);

        // Create a grid to store module states (true = black, false = white)
        boolean[][] grid = new boolean[gridSize][gridSize];

        // Analyze the image to populate the grid
        int cellWidth = width / gridSize;
        int cellHeight = height / gridSize;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int startX = i * cellWidth;
                int startY = j * cellHeight;
                int endX = startX + cellWidth;
                int endY = startY + cellHeight;

                // Determine if the cell is black or white
                grid[i][j] = isCellBlack(image, startX, startY, endX, endY);
            }
        }
        return grid;
    }

    private boolean isCellBlack(BufferedImage image, int startX, int startY, int endX, int endY) {
        int blackPixelCount = 0;
        int totalPixelCount = 0;

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                int grayValue = new Color(image.getRGB(x, y)).getRed();
                if (grayValue < 128) { // Black pixel
                    blackPixelCount++;
                }
                totalPixelCount++;
            }
        }

        // A cell is considered black if more than half of its pixels are black
        return blackPixelCount > (totalPixelCount / 2);
    }

    private String decodeGrid(boolean[][] grid) {
        // Extract binary payload
        String binaryData = extractDataPayload(grid);

        // Debug: Output extracted binary data
        System.out.println("Extracted Binary Data: " + binaryData);

        // Decode numeric data
        return decodeNumericData(binaryData);
    }

    private String decodeNumericData(String binaryData) {
        StringBuilder decodedData = new StringBuilder();
        int index = 0;

        while (index + 10 <= binaryData.length()) {
            // Group 10 bits for 2 digits
            String group = binaryData.substring(index, index + 10);
            int value = Integer.parseInt(group, 2);
            decodedData.append(String.format("%02d", value)); // Always 2 digits
            index += 10;
        }

        // Handle trailing digits (4 bits for 1 digit)
        if (index + 4 <= binaryData.length()) {
            String trailingBits = binaryData.substring(index, index + 4);
            int value = Integer.parseInt(trailingBits, 2);
            decodedData.append(value);
        }

        return decodedData.toString();
    }

    private String extractDataPayload(boolean[][] grid) {
        StringBuilder binaryData = new StringBuilder();

        int rows = grid.length;
        int cols = grid[0].length;

        for (int row = 1; row < rows - 1; row++) { // Skip top and bottom finder patterns
            for (int col = 1; col < cols - 1; col++) { // Skip left and right finder patterns
                binaryData.append(grid[row][col] ? "1" : "0");
            }
        }

        return binaryData.toString();
    }

    private int gridSize(BufferedImage image) {
        int steps = image.getWidth();
        int size = 0;

        Color lastColor = new Color(Color.green.getRGB());


        for (int i = 0; i < steps; i++) {
            if (lastColor.getRGB() != image.getRGB(i, 5)) {
                size++;
            }
            lastColor = new Color(image.getRGB(i, 5));
        }

        return size - 1;
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
