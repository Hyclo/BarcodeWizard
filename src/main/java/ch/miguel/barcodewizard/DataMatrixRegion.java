package ch.miguel.barcodewizard;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class DataMatrixRegion {
    private Rectangle boundingBox;
    private BufferedImage image;

    public DataMatrixRegion(Rectangle boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
