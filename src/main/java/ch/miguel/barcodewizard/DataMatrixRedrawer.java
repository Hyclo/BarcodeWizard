package ch.miguel.barcodewizard;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class DataMatrixRedrawer {

    public BufferedImage redrawDataMatrix(BufferedImage image, int gridSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the size of each module (chunk)
        int cellWidth = width / gridSize;
        int cellHeight = height / gridSize;

        // Create a new blank image
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = newImage.createGraphics();

        // Set background to white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Iterate through the grid and redraw each module
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                // Calculate the boundaries of the current cell
                int startX = i * cellWidth;
                int startY = j * cellHeight;
                int endX = startX + cellWidth;
                int endY = startY + cellHeight;

                // Determine if the original cell is black or white
                if (isCellBlack(image, startX, startY, endX, endY)) {
                    g2d.setColor(Color.BLACK);
                } else {
                    g2d.setColor(Color.WHITE);
                }

                // Draw the perfect rectangle for the module
                g2d.fillRect(startX, startY, cellWidth, cellHeight);
            }
        }

        g2d.dispose();
        return newImage;
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
        return blackPixelCount > (totalPixelCount / 1.75);
    }
}

