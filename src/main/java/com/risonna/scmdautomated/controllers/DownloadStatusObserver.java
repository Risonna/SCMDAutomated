package com.risonna.scmdautomated.controllers;

public interface DownloadStatusObserver {
    void onDownloadStatusChanged(String publishedFileId, String status);
}