package com.risonna.scmdautomated.model.entities;

import javafx.scene.image.Image;

public class RecentDownload {
    private String name;
    private Image image;
    private String fileSize;
    private String downloadStatus;
    private String filepath;
    private String publishedFieldId;
    private String appId;

    public RecentDownload(String name, Image image, String fileSize, String downloadStatus, String filepath, String publishedFieldId, String appId) {
        this.name = name;
        this.image = image;
        this.fileSize = fileSize;
        this.downloadStatus = downloadStatus;
        this.filepath = filepath;
        this.publishedFieldId = publishedFieldId;
        this.appId = appId;
    }


    public String getName() {
        return name;
    }


    public Image getImage() {
        return image;
    }


    public String getFileSize() {
        return fileSize;
    }

    public String getDownloadStatus() {
        return downloadStatus;
    }
    public void setDownloadStatus (String downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public String getFilepath() {
        return filepath;
    }
    public void setFilepath (String filepath) {
        this.filepath = filepath;
    }

    public String getPublishedFieldId() {
        return publishedFieldId;
    }

    public void setPublishedFieldId(String publishedFieldId) {
        this.publishedFieldId = publishedFieldId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String toString(){
        return "Item [id=" + publishedFieldId + ", name=" + name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((publishedFieldId == null) ? 0 : publishedFieldId.hashCode());
        result = prime * result + ((appId == null) ? 0 : appId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RecentDownload other))
            return false;
        if (publishedFieldId == null || publishedFieldId.isEmpty() || appId == null || appId.isEmpty())
            return false;
        return publishedFieldId.equals(other.publishedFieldId) && appId.equals(other.appId);
    }
}