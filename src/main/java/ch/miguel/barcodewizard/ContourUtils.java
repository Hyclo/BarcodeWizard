package ch.miguel.barcodewizard;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class ContourUtils {

        /**
     * Filters contours by removing those that are oversized.
     *
     * @param contours List of contours to filter.
     * @param maxWidth Maximum allowed width for a contour.
     * @param maxHeight Maximum allowed height for a contour.
     * @return List of contours that fit within the size constraints.
     */
    public static List<Contour> filterWrongSizedContours(List<Contour> contours) {
        int maxWidth = 150;
        int maxHeight = 150;
        int minWidth = 50;
        int minHeight = 50;

        List<Contour> filteredContours = new ArrayList<>();

        for (Contour contour : contours) {
            Rectangle boundingBox = contour.getBoundingBox();

            if (boundingBox.width <= maxWidth 
                && boundingBox.height <= maxHeight 
                && boundingBox.width >= minWidth 
                && boundingBox.height >= minHeight) {
                    
                filteredContours.add(contour);
            }
        }

        return filteredContours;
    }

    /**
     * Filters contours by removing those contained within larger contours.
     *
     * @param contours List of contours to filter.
     * @return List of filtered contours with no containment.
     */
    public static List<Contour> filterContainedContours(List<Contour> contours) {
        List<Contour> filteredContours = new ArrayList<>();

        for (Contour outer : contours) {
            boolean isContained = false;

            for (Contour inner : contours) {
                if (outer != inner && isContainedWithin(outer.getBoundingBox(), inner.getBoundingBox())) {
                    // Discard this contour if it is contained within a larger one
                    isContained = true;
                    break;
                }
            }

            if (!isContained) {
                filteredContours.add(outer);
            }
        }

        return filteredContours;
    }

    /**
     * Checks if one bounding box is contained within another.
     *
     * @param inner The smaller bounding box.
     * @param outer The larger bounding box.
     * @return True if inner is completely within outer, false otherwise.
     */
    private static boolean isContainedWithin(Rectangle inner, Rectangle outer) {
        return outer.contains(inner);
    }
}

