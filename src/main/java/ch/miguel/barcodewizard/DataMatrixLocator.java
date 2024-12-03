package ch.miguel.barcodewizard;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class DataMatrixLocator {

    public DataMatrixRegion locateDataMatrix(BufferedImage image) {
        try {
            // Step 3: Detect edges
            BufferedImage edges = detectEdges(image);

            // Step 4: Find contours (potential regions)
            List<Contour> contours = findContours(edges);

            List<Contour> filteredContours = ContourUtils.filterWrongSizedContours(contours);
            filteredContours = ContourUtils.filterContainedContours(filteredContours);

            contours = filteredContours;

            List<Rectangle> boundingBoxes = new ArrayList<>();
            for (Contour contour : contours) {
                boundingBoxes.add(contour.getBoundingBox());
            }

            BufferedImage debugImage = DebuggingUtils.drawBoundingBoxes(image, boundingBoxes);

            saveImage(debugImage, "debugImage.png");

            // Step 5: Validate contours to locate Data Matrix
            for (Contour contour : contours) {
                if (isValidDataMatrix(image, contour)) {
                    // Extract and return the region
                    return extractRegion(contour);
                }
            }

            } catch (Exception e) {
                System.err.println("Error locating Data Matrix: " + e.getMessage());
                e.printStackTrace();
            }
        return null; // No Data Matrix found
    }

    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    private BufferedImage binarizeImage(BufferedImage image) {
        BufferedImage binaryImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int gray = new Color(image.getRGB(x, y)).getRed();
                int binaryColor = gray < 128 ? 0 : 255;
                binaryImage.setRGB(x, y, new Color(binaryColor, binaryColor, binaryColor).getRGB());
            }
        }
        return binaryImage;
    }

    private BufferedImage detectEdges(BufferedImage binaryImage) {
        int width = binaryImage.getWidth();
        int height = binaryImage.getHeight();
        BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Sobel kernels
        int[][] sobelX = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
        };

        int[][] sobelY = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
        };

        // Apply Sobel filter
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int gx = 0;
                int gy = 0;

                // Convolve the Sobel kernels
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int pixel = new Color(binaryImage.getRGB(x + i, y + j)).getRed();
                        gx += sobelX[i + 1][j + 1] * pixel;
                        gy += sobelY[i + 1][j + 1] * pixel;
                    }
                }

                // Compute gradient magnitude
                int magnitude = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));

                // Set the edge intensity
                Color edgeColor = new Color(magnitude, magnitude, magnitude);
                edgeImage.setRGB(x, y, edgeColor.getRGB());
            }
        }

        saveImage(edgeImage, "edgeImage.png");

        return edgeImage;
    }

    private List<Contour> findContours(BufferedImage edges) {
        int width = edges.getWidth();
        int height = edges.getHeight();

        // Visited pixels tracker to avoid revisiting
        boolean[][] visited = new boolean[width][height];
        List<Contour> contours = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!visited[x][y] && isEdgePixel(edges, x, y)) {
                    // Start a flood fill for this new contour
                    Contour contour = new Contour();
                    floodFill(edges, x, y, visited, contour);
                    if (isValidContour(contour)) {
                        contours.add(contour);
                    }
                }
            }
        }
        return contours;
    }

       private boolean isEdgePixel(BufferedImage image, int x, int y) {
        // Check if a pixel is white (edge pixel in a binary image)
        int color = new Color(image.getRGB(x, y)).getRed();
        return color > 0; // Non-zero indicates an edge in a binary image
    }

    private void floodFill(BufferedImage image, int x, int y, boolean[][] visited, Contour contour) {
        int width = image.getWidth();
        int height = image.getHeight();
        List<Point> stack = new ArrayList<>();
        stack.add(new Point(x, y));

        while (!stack.isEmpty()) {
            Point point = stack.remove(stack.size() - 1);
            int px = point.x;
            int py = point.y;

            if (px < 0 || px >= width || py < 0 || py >= height || visited[px][py] || !isEdgePixel(image, px, py)) {
                continue;
            }

            // Mark as visited and add to contour
            visited[px][py] = true;
            contour.addPoint(px, py);

            // Push neighbors to the stack
            stack.add(new Point(px - 1, py));
            stack.add(new Point(px + 1, py));
            stack.add(new Point(px, py - 1));
            stack.add(new Point(px, py + 1));
        }
    }

    private boolean isValidContour(Contour contour) {
        // Example validation: Check for minimum size or shape
        Rectangle boundingBox = contour.getBoundingBox();
        int area = boundingBox.width * boundingBox.height;
        return area > 50; // Minimum area threshold
    }

    private boolean isValidDataMatrix(BufferedImage image, Contour contour) {
        Rectangle boundingBox = contour.getBoundingBox();
        int width = boundingBox.width;
        int height = boundingBox.height;

        // Validate the aspect ratio (close to 1:1 for square Data Matrix)
        double aspectRatio = (double) width / height;
        if (aspectRatio < 0.8 || aspectRatio > 1.2) {
            return false; // Not a valid square or nearly square Data Matrix
        }

        // Minimum size threshold (to filter out noise)
        if (width < 10 || height < 10) {
            return false; // Too small to be a Data Matrix
        }

        // Check if it has the L-shaped finder pattern
        if (!hasFinderPattern(image, contour)) {
            return false; // Does not have the required finder pattern
        }

        // Additional validation criteria can be added here
        return true; // The contour is a valid Data Matrix
    }

    private boolean hasFinderPattern(BufferedImage image, Contour contour) {
        Rectangle boundingBox = contour.getBoundingBox();
        BufferedImage region = extractRegion(image, boundingBox);

        int width = region.getWidth();
        int height = region.getHeight();

        // Check for the solid black border on the left and bottom sides
        boolean hasLeftBorder = checkSolidBorder(region, 5, 5, 5, height - 5);
        boolean hasBottomBorder = checkSolidBorder(region, 5, height - 5, width - 5, height - 5);

        // Check for the broken border on the top and right sides
        boolean hasTopBrokenBorder = checkBrokenBorder(region, 5, 5, width - 5, 5);
        boolean hasRightBrokenBorder = checkBrokenBorder(region, width - 5, 5, width - 5, height - 5);

        return hasLeftBorder && hasBottomBorder && hasTopBrokenBorder && hasRightBrokenBorder;
    }

    private BufferedImage extractRegion(BufferedImage image, Rectangle boundingBox) {
        return image.getSubimage(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
    }

    private boolean checkSolidBorder(BufferedImage image, int x1, int y1, int x2, int y2) {
        int dx = (x2 - x1 == 0) ? 0 : 1; // Direction along x
        int dy = (y2 - y1 == 0) ? 0 : 1; // Direction along y

        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));

        for (int i = 0; i <= steps; i++) {
            int x = x1 + i * dx;
            int y = y1 + i * dy;

            int intensity = new Color(image.getRGB(x, y)).getRed();
            if (intensity > 50) { // Pixel is not black
                return false;
            }
        }
        return true; // All pixels are black
    }

    private boolean checkBrokenBorder(BufferedImage image, int x1, int y1, int x2, int y2) {
        int dx = (x2 - x1 == 0) ? 0 : 1; // Direction along x
        int dy = (y2 - y1 == 0) ? 0 : 1; // Direction along y

        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        boolean hasWhite = false;
        boolean hasBlack = false;

        for (int i = 0; i <= steps; i++) {
            int x = x1 + i * dx;
            int y = y1 + i * dy;

            int intensity = new Color(image.getRGB(x, y)).getRed();
            if (intensity < 50) {
                hasBlack = true; // Pixel is black
            } else {
                hasWhite = true; // Pixel is white
            }

            // If both white and black pixels are detected, it's a broken pattern
            if (hasWhite && hasBlack) {
                return true;
            }
        }
        return false; // Not a broken pattern
    }

    private DataMatrixRegion extractRegion(Contour contour) {
        // Extract and return the region based on the contour
        return new DataMatrixRegion(contour.getBoundingBox());
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