package com.risonna.scmdautomated.misc;

public class DataUtils {
    public static String getHumanReadableFileSize(long fileSize) {
        if (fileSize < 1024) {
            return fileSize + " bytes";
        } else if (fileSize < 1024 * 1024) {
            return (fileSize / 1024) + " KB";
        } else if (fileSize < 1024 * 1024 * 1024) {
            return (fileSize / (1024 * 1024)) + " MB";
        } else {
            return (fileSize / (1024 * 1024 * 1024)) + " GB";
        }
    }
}
