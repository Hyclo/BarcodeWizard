package ch.miguel.barcodewizard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        var extractor = new CustomDataMatrixExtractor();

        // Load the TIFF image as a byte array
        byte[] tiffByteArray = null;

        try {
            tiffByteArray = Files.readAllBytes(Paths.get("src\\main\\resources\\images\\image2.tif"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (tiffByteArray == null) {
            System.err.println("Error loading the TIFF image.");
            return;
        }

        String data = extractor.extractDataMatrix(tiffByteArray);
        System.out.println("Data Matrix content: " + data);
    }
}