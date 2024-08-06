package com.risonna.scmdautomated.model;

import javafx.scene.image.Image;

public class ImageDownloader {
    public static Image downloadImage(String imageUrl) {
        try {
            return new Image(imageUrl);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }
}