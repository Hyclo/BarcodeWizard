package ch.miguel.barcodewizard;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.awt.Rectangle;

public class DebuggingUtils {

    /**
     * Draws bounding boxes on the given image.
     *
     * @param image The original image where bounding boxes will be drawn.
     * @param boundingBoxes A list of bounding boxes to draw.
     * @return A new BufferedImage with bounding boxes drawn on it.
     */
    public static BufferedImage drawBoundingBoxes(BufferedImage image, List<Rectangle> boundingBoxes) {
        // Create a copy of the original image to avoid modifying it
        BufferedImage debugImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g2d = debugImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        // Set rectangle color and thickness
        g2d.setColor(Color.RED); // Color for the bounding boxes
        g2d.setStroke(new java.awt.BasicStroke(2)); // Thickness of the rectangle

        // Draw each bounding box
        for (Rectangle box : boundingBoxes) {
            g2d.drawRect(box.x, box.y, box.width, box.height);
        }

        g2d.dispose();
        return debugImage;
    }
}

