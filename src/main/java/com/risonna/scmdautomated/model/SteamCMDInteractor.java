package com.risonna.scmdautomated.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.unix.PTYOutputStream;
import com.risonna.scmdautomated.controllers.RecentDownloadsController;
import com.risonna.scmdautomated.controllers.SettingsController;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import com.risonna.scmdautomated.model.entities.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.risonna.scmdautomated.misc.DataUtils.getHumanReadableFileSize;


public class SteamCMDInteractor {
    private static final int MAX_CONCURRENT_PROCESSES = 5;
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("Downloading item (\\d+) \\.\\.\\.");
    private static final Pattern SUCCESS_PATTERN = Pattern.compile("Success\\. Downloaded item (\\d+) to \"(.+)\" \\((\\d+) bytes\\)");
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\u001B\\[[;\\d]*[A-Za-z]");

    private static String stripAnsiEscapeCodes(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = ANSI_ESCAPE_PATTERN.matcher(input);
        return matcher.replaceAll("");
    }
    public static void downloadWorkshopItem(String publishedFileId, long appId, RecentDownloadsController recentDownloadsController) {
        String steamcmdPath = SettingsController.getSteamcmdPath();
        if (steamcmdPath == null || steamcmdPath.isEmpty()) {
            System.out.println("Steamcmd path is not set");
            return;
        }

        File steamcmdFile = new File(steamcmdPath + "\\steamcmd.exe");
        if (!steamcmdFile.exists()) {
            System.out.println("Steamcmd executable not found at: " + steamcmdPath + "\\steamcmd.exe");
            return;
        }
        String loginCredentials = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getUsername() + " " + UserSession.getInstance().decryptPassword() : "anonymous";

        String command = steamcmdPath  + "\\steamcmd.exe +login "  + loginCredentials + " +workshop_download_item " + appId + " " + publishedFileId + " validate" + " +quit";

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
                if(UserSession.getInstance().isLoggedIn()) {
                    downloadCollectionItemsLoggedIn(itemIds, recentDownloadsController);
                } else {
                    downloadCollectionItemsLoggedOut(itemIds, recentDownloadsController);
                }
                return null;
            }
        };
    }
    public static void downloadCollectionItemsLoggedIn(List<Long> itemIds, RecentDownloadsController recentDownloadsController) {
        String itemDetailsResponse = APICaller.sendPostRequestCheckItemDetails(itemIds);
        String appIdFurther = null;
        if (itemDetailsResponse == null) {
            return;
        }

        JSONObject itemDetailsJson = new JSONObject(itemDetailsResponse);
        JSONObject itemDetailsResponseObj = itemDetailsJson.getJSONObject("response");
        JSONArray publishedFileDetails = itemDetailsResponseObj.getJSONArray("publishedfiledetails");

        String steamcmdPath = SettingsController.getSteamcmdPath();
        String loginCredentials = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getUsername() + " " + UserSession.getInstance().decryptPassword() : "anonymous";

        StringBuilder commandBuilder = new StringBuilder(steamcmdPath + "\\steamcmd.exe +login " + loginCredentials);

        for (int i = 0; i < publishedFileDetails.length(); i++) {
            JSONObject itemDetail = publishedFileDetails.getJSONObject(i);
            String itemId = itemDetail.optString("publishedfileid");
            long appId = itemDetail.optLong("creator_app_id");
            String title = itemDetail.optString("title");
            String imageUrl = itemDetail.optString("preview_url");
            long fileSize = itemDetail.optLong("file_size");

            if (itemId.isEmpty() || appId == 0 || title.isEmpty()) {
                continue;
            }
            appIdFurther = String.valueOf(appId);

            Platform.runLater(() -> {
                RecentDownload download = new RecentDownload(title, ImageDownloader.downloadImage(imageUrl),
                        getHumanReadableFileSize(fileSize), "queued", null, itemId, String.valueOf(appId));
                recentDownloadsController.addRecentDownload(download);
            });

            commandBuilder.append(" +workshop_download_item ").append(appId).append(" ").append(itemId).append(" validate");
        }

        commandBuilder.append(" +quit");
        System.out.println(commandBuilder.toString());

        downloadAllItemsInSingleInstance(steamcmdPath, commandBuilder.toString(), recentDownloadsController, appIdFurther);

    }
    public static void downloadCollectionItemsLoggedOut(List<Long> itemIds, RecentDownloadsController recentDownloadsController) {
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
                        getHumanReadableFileSize(fileSize), "queued", null, itemId, String.valueOf(appId));
                recentDownloadsController.addRecentDownload(download);
            });

            downloadQueue.offer(() -> queuedDownloadWorkshopItem(itemId, appId, recentDownloadsController));
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
    private static void downloadAllItemsInSingleInstance(String steamcmdPath, String command, RecentDownloadsController recentDownloadsController, String appId) {
        List<String> commandList = new ArrayList<>();
        commandList.add(steamcmdPath + "\\steamcmd.exe");
        commandList.addAll(Arrays.asList(command.split(" ")));

        PtyProcessBuilder builder = new PtyProcessBuilder(commandList.toArray(new String[0]))
                .setDirectory(new File(steamcmdPath).getAbsolutePath());

        Thread thread = new Thread(() -> {
            try {
                PtyProcess process = builder.start();
                InputStream inputStream = process.getInputStream();

                StringBuilder lineBuffer = new StringBuilder();
                int c;
                while ((c = inputStream.read()) != -1) {
                    if (c == '\n' || c == '\r') {
                        String line = lineBuffer.toString().trim();
                        if (!line.isEmpty()) {
                            System.out.println("___________________________________________________________________________________________");
                            System.out.println(line);
                            System.out.println("____________________________________________________________________________________________");
                            processLine(line, recentDownloadsController, appId);
                        }
                        lineBuffer.setLength(0);
                    } else {
                        lineBuffer.append((char) c);
                    }
                }

                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.out.println("Error running steamcmd: " + e.getMessage());
            }
        });
        thread.start();
    }
    private static void processLine(String line, RecentDownloadsController recentDownloadsController, String appId) {
        Matcher downloadMatcher = DOWNLOAD_PATTERN.matcher(line);
        if (downloadMatcher.find()) {
            String itemId = downloadMatcher.group(1);
            System.out.println("Downloading : " + itemId);
            Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(itemId, "downloading", null));
            return;
        }

        if (line.contains("Success")) {
            String itemId = line.split(" ")[3];
            String filepath = line.split(" ")[5];
            String strippedFilePath = stripAnsiEscapeCodes(filepath);
            String replacedFilePath = strippedFilePath.replaceAll("\"", "") + "content\\" + appId + "\\" + itemId;
            System.out.println("Downloaded : " + itemId);
            System.out.println("Replaced file path: " + replacedFilePath);
            File file = new File(replacedFilePath);
            System.out.println("File absolute path: " + file.getAbsolutePath());
            System.out.println("File exists after download: " + file.exists());
            Platform.runLater(() -> recentDownloadsController.updateRecentDownloadStatusAndFilepath(itemId, "success", replacedFilePath));
            return;
        }

        if (line.contains("ERROR!")) {
            System.out.println("Error occurred: " + line);
        }
    }


    private static void queuedDownloadWorkshopItem(String publishedFileId, long appId, RecentDownloadsController recentDownloadsController) {
        String steamcmdPath = SettingsController.getSteamcmdPath();
        if (steamcmdPath == null || steamcmdPath.isEmpty()) {
            System.out.println("Steamcmd path is not set");
            return;
        }

        File steamcmdFile = new File(steamcmdPath + "\\steamcmd.exe");
        if (!steamcmdFile.exists()) {
            System.out.println("Steamcmd executable not found at: " + steamcmdPath + "\\steamcmd.exe");
            return;
        }
        String loginCredentials = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getUsername() + " " + UserSession.getInstance().decryptPassword() : "anonymous";
        String command = steamcmdPath + "\\steamcmd.exe +login " + loginCredentials + " +workshop_download_item " + appId + " " + publishedFileId + " validate +quit";

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

    public static String login() {
        String steamcmdPath =  SettingsController.getSteamcmdPath();
        String username = UserSession.getInstance().getUsername();
        String password = UserSession.getInstance().decryptPassword();
        String command = steamcmdPath + "\\steamcmd.exe +login " + username + " " + password + " +quit";

        List<String> commandList = new ArrayList<>();
        commandList.add("cmd.exe");
        commandList.add("/c");
        commandList.add(command);

        PtyProcessBuilder builder = new PtyProcessBuilder(commandList.toArray(new String[0]))
                .setDirectory(new File(steamcmdPath).getAbsolutePath());

        try {
            PtyProcess process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder output = new StringBuilder();
                while (reader.ready()) {
                    line = reader.readLine();
                    output.append(line).append("\n");
                    System.out.println(line);
                    if(output.toString().toLowerCase().contains("steam guard")){
                        break;
                    }
                }


                // Check output for login status
                String outputString = output.toString();
                if (output.toString().toLowerCase().contains("waiting for user info...ok")) {
                    return "OK";
                } else if (outputString.contains("FAILED")) {
                    return "FAILED";
                } else if (outputString.toLowerCase().contains("steam guard")) {
                    return "NEED_STEAM_GUARD";
                } else if (outputString.toLowerCase().contains("two-factor")) {
                    return "NEED_TWO_FACTOR";
                }

                return "UNKNOWN_ERROR";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "EXCEPTION: " + e.getMessage();
        }
    }
    public static String loginWithSteamGuard(String steamGuardCode) {
        String steamcmdPath = SettingsController.getSteamcmdPath();
        String username = UserSession.getInstance().getUsername();
        String password = UserSession.getInstance().decryptPassword();
        String command = steamcmdPath + "\\steamcmd.exe +login " + username + " " + password + " " + steamGuardCode +" +quit";

        List<String> commandList = new ArrayList<>();
        commandList.add("cmd.exe");
        commandList.add("/c");
        commandList.add(command);

        PtyProcessBuilder builder = new PtyProcessBuilder(commandList.toArray(new String[0]))
                .setDirectory(new File(steamcmdPath).getAbsolutePath());

        try {
            PtyProcess process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder output = new StringBuilder();
                while (reader.ready()) {
                    line = reader.readLine();
                    output.append(line).append("\n");
                    System.out.println(line);
                    if(output.toString().toLowerCase().contains("waiting for user info...ok")){
                        return "SUCCESS";
                    }
                }
                return "ERROR";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "ERROR";
    }
    public static boolean isFileDownloaded(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
}