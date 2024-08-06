package com.risonna.scmdautomated.model;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.risonna.scmdautomated.controllers.RecentDownloadsController;
import com.risonna.scmdautomated.controllers.SettingsController;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import com.risonna.scmdautomated.model.entities.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import org.json.JSONArray;
import org.json.JSONObject;

public class SteamCMDInteractor {
    private static final int MAX_CONCURRENT_PROCESSES = 5;
    public static void downloadWorkshopItem(String publishedFileId, long appId, boolean anonymous, RecentDownloadsController recentDownloadsController) {
        String steamcmdPath = SettingsController.getSteamcmdPath();
        if (steamcmdPath == null || steamcmdPath.isEmpty()) {
            System.out.println("Steamcmd path is not set");
            return;
        }

        File steamcmdFile = new File(steamcmdPath);
        if (!steamcmdFile.exists()) {
            System.out.println("Steamcmd executable not found at: " + steamcmdPath);
            return;
        }

        String command = steamcmdPath + " +login anonymous +workshop_download_item " + appId + " " + publishedFileId + " validate" + " +quit";

        Thread thread = new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int progress = 0;
                String filepath = null;
                while ((line = reader.readLine())!= null) {
                    System.out.println(line);
                    if (line.contains("Downloading...")) {
                        //...
                    } else if (line.startsWith("Success. Downloaded item")) {
                        // Extract the filepath from the output
                        String[] parts = line.split("\"");
                        if (parts.length > 1) {
                            filepath = parts[1];
                            // Update the status and filepath here
                            String finalFilepath = filepath;
                            Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, "success", finalFilepath));
                        }
                    } else if (line.contains("ERROR!")) {
                        // Update the status here
                        Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, "failed", null));
                    }
                }
                reader.close();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.out.println("Error running steamcmd: " + e.getMessage());
            }
        });
        thread.start();
    }

    public static Task<Void> createDownloadCollectionItemsTask(List<Long> itemIds, RecentDownloadsController recentDownloadsController) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                downloadCollectionItems(itemIds, recentDownloadsController);
                return null;
            }
        };
    }
    public static void downloadCollectionItems(List<Long> itemIds, RecentDownloadsController recentDownloadsController) {
        String itemDetailsResponse = APICaller.sendPostRequestCheckItemDetails(itemIds);
        if (itemDetailsResponse == null) {
            return;
        }

        JSONObject itemDetailsJson = new JSONObject(itemDetailsResponse);
        JSONObject itemDetailsResponseObj = itemDetailsJson.getJSONObject("response");
        JSONArray publishedFileDetails = itemDetailsResponseObj.getJSONArray("publishedfiledetails");

        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_PROCESSES);
        Queue<Runnable> downloadQueue = new LinkedList<>();

        for (int i = 0; i < publishedFileDetails.length(); i++) {
            JSONObject itemDetail = publishedFileDetails.getJSONObject(i);
            String itemId;
            long appId;
            String title;
            long fileSize;
            if(itemDetail.has("publishedfileid")) {
                itemId = itemDetail.getString("publishedfileid");
            }
            else {
                continue;
            }
            if(itemDetail.has("creator_app_id")) {
                appId = itemDetail.getLong("creator_app_id");
            }
            else {
                continue;
            }
            if(itemDetail.has("title")) {
                title = itemDetail.getString("title");
            }
            else {
                continue;
            }
            String imageUrl = itemDetail.getString("preview_url");
            if(itemDetail.has("file_size")) {
                fileSize = itemDetail.getLong("file_size");
            }
            else {
                continue;
            }

            Platform.runLater(() -> {
                RecentDownload download = new RecentDownload(title, ImageDownloader.downloadImage(imageUrl),
                        humanReadableFileSize(fileSize), "queued", null, itemId, String.valueOf(appId));
                recentDownloadsController.addRecentDownload(download);
            });

            downloadQueue.offer(() -> queuedDownloadWorkshopItem(itemId, appId, true, recentDownloadsController));
        }

        for (int i = 0; i < MAX_CONCURRENT_PROCESSES; i++) {
            executor.execute(() -> {
                while (!downloadQueue.isEmpty()) {
                    Runnable download = downloadQueue.poll();
                    if (download != null) {
                        download.run();
                    }
                }
            });
        }

        executor.shutdown();
    }

    private static void queuedDownloadWorkshopItem(String publishedFileId, long appId, boolean anonymous, RecentDownloadsController recentDownloadsController) {
        String steamcmdPath = SettingsController.getSteamcmdPath();
        if (steamcmdPath == null || steamcmdPath.isEmpty()) {
            System.out.println("Steamcmd path is not set");
            return;
        }

        File steamcmdFile = new File(steamcmdPath);
        if (!steamcmdFile.exists()) {
            System.out.println("Steamcmd executable not found at: " + steamcmdPath);
            return;
        }
        String loginMethod = anonymous? "anonymous" : SettingsController.getSteamLogin();
        String command = steamcmdPath + " +login " + loginMethod + " +workshop_download_item " + appId + " " + publishedFileId + " validate +quit";

        try {
            Process process = Runtime.getRuntime().exec(command);
            Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, "downloading", null));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String filepath = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.startsWith("Success. Downloaded item")) {
                    String[] parts = line.split("\"");
                    if (parts.length > 1) {
                        filepath = parts[1];
                        String finalFilepath = filepath;
                        Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, "success", finalFilepath));
                    }
                } else if (line.contains("ERROR!")) {
                    Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, "failed", null));
                }
            }
            reader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error running steamcmd: " + e.getMessage());
            Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, "failed", null));
        }
    }

    private static String humanReadableFileSize(long fileSize) {
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
    public static String login() {
        String steamcmdPath = SettingsController.getSteamcmdPath();
        if (steamcmdPath == null || steamcmdPath.isEmpty()) {
            System.out.println("Steamcmd path is not set");
            return "PATH ERROR";
        }
        File steamcmdFile = new File(steamcmdPath);
        if (!steamcmdFile.exists()) {
            System.out.println("Steamcmd executable not found at: " + steamcmdPath);
            return "STEAMCMD DOESN'T EXIST";
        }

        System.out.println("Starting SteamCMD login process...");

        try {
            List<String> command = new ArrayList<>();
            command.add(steamcmdPath);
            command.add("+login");
            command.add(UserSession.getInstance().getUsername());
            command.add(UserSession.getInstance().decryptPassword());
            command.add("+quit");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(steamcmdFile.getParentFile());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            System.out.println("SteamCMD process started. Command: " + String.join(" ", command));

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            StringBuilder output = new StringBuilder();
            long startTime = System.currentTimeMillis();
            long timeout = 60000; // 60 seconds timeout

            while ((line = reader.readLine()) != null) {
                System.out.println("SteamCMD: " + line);
                output.append(line).append("\n");

                if (System.currentTimeMillis() - startTime > timeout) {
                    System.out.println("Login process timed out");
                    process.destroy();
                    return "TIMEOUT";
                }

                if (line.contains("FAILED")) {
                    System.out.println("Login failed");
                    process.destroy();
                    return "FAILED";
                } else if (line.toLowerCase().contains("logging in user " + "'" + UserSession.getInstance().getUsername().toLowerCase() + "'" + "to steam public...ok" ) || line.contains("Waiting for user info...OK")) {
                    System.out.println("Login succeeded");
                    process.destroy();
                    return "OK";
                } else if (line.contains("Steam Guard code:")) {
                    System.out.println("Steam Guard code required");
                    process.destroy();
                    return "NEED_STEAM_GUARD";
                } else if (line.contains("Two-factor code:")) {
                    System.out.println("Two-factor authentication required");
                    process.destroy();
                    return "NEED_TWO_FACTOR";
                }
            }

            int exitCode = process.waitFor();
            System.out.println("SteamCMD process exited with code: " + exitCode);
            System.out.println("Full SteamCMD output:\n" + output.toString());

            if (output.toString().contains("Logged in OK")) {
                return "OK";
            } else if (output.toString().contains("FAILED")) {
                return "FAILED";
            }

            return "UNKNOWN_ERROR";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "EXCEPTION: " + e.getMessage();
        }
    }


}