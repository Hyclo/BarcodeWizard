package ch.miguel.barcodewizard;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Contour {
    private final List<Point> points = new ArrayList<>();

    public void addPoint(int x, int y) {
        points.add(new Point(x, y));
    }

    public Rectangle getBoundingBox() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Point point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}

