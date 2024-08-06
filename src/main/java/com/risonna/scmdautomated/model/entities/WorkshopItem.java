package com.risonna.scmdautomated.model.entities;

import javafx.scene.image.Image;

public class WorkshopItem {
    public WorkshopItem(){

    }
    public WorkshopItem(String name, String description, Image image, String uploadDate, String updateDate, String fileSize){
        this.name = name;
        this.description = description;
        this.image = image;
        this.uploadDate = uploadDate;
        this.updateDate = updateDate;
        this.fileSize = fileSize;
    }
    private String name;
    private String description;
    private Image image;
    private String uploadDate;
    private String updateDate;
    private String fileSize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }
}
