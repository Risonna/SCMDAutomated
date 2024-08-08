package com.risonna.scmdautomated.controllers;

import com.risonna.scmdautomated.HelloApplication;
import com.risonna.scmdautomated.model.*;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import com.risonna.scmdautomated.model.entities.UserSession;
import com.risonna.scmdautomated.model.entities.WorkshopItem;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class MainController {
    @FXML
    private CheckBox loginCheckBox;

    @FXML
    private Button downloadButton;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField urlTextField;
    @FXML
    private Text titleLabel;

    @FXML
    private Text uploadDate;
    @FXML
    private Text updateDate;
    @FXML
    private Text size;

    @FXML
    private Text gameName;
    @FXML
    private Text gameReleaseDate;
    @FXML
    private ImageView gameImage;
    @FXML
    private Text errorMessageLabel;
    @FXML
    private Button collectionButton;
    @FXML
    private Text collectionItemsLabel;
    @FXML
    private Button settingsButton;
    @FXML
    private Button aboutButton;
    @FXML
    private ProgressBar downloadProgressBar;
    @FXML
    private StackPane recentDownloadsStackPane;

    @FXML
    private Label recentDownloadsNotification;

    private String publishedFileId;
    private long appId;
    private static RecentDownloadsController recentDownloadsController;
    @FXML
    private Label temporaryMessageLabel;
    private List<Long> collectionIds;
    @FXML
    private VBox centerContent;
    @FXML
    private Label installedItemLabel;
    @FXML
    private Button openFolderButton;
    private static CollectionItemsController collectionItemsController;
    public static void setCollectionItemsController(CollectionItemsController controller) {
        collectionItemsController = controller;
    }

    public static CollectionItemsController getCollectionItemsController() {
        return collectionItemsController;
    }

    public MainController() {
        recentDownloadsController = new RecentDownloadsController();
    }

    public static RecentDownloadsController getRecentDownloadsController() {
        return recentDownloadsController;
    }

    @FXML
    private void initialize() {
        Image dog = new Image("https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcSCki_UMys6lxgh_ubSzHzY34ajYywRpXt_mPFjxMFoOHERZb7c");
        imageView.setImage(dog);
        loginCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!UserSession.getInstance().isLoggedIn() && newValue) {
                showLoginWindow();
            } else {
                UserSession.getInstance().setLoggedIn(false);
            }
        });
        recentDownloadsController.setNotificationLabel(recentDownloadsNotification);

        //steamcmd path
        File steamcmdPathFile = new File(SettingsController.STEAMCMD_PATH_FILE);
        if (!steamcmdPathFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Steamcmd path is required");
            alert.setContentText("Please enter a valid steamcmd path in the settings");

            ButtonType settingsButtonType = new ButtonType("Settings");
            alert.getButtonTypes().setAll(settingsButtonType, ButtonType.CANCEL);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == settingsButtonType) {
                try {
                    FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("fxml/settings.fxml"));
                    Parent root = loader.load();
                    Stage settingsStage = new Stage();
                    settingsStage.initModality(Modality.APPLICATION_MODAL);
                    settingsStage.setTitle("Settings");
                    settingsStage.setScene(new Scene(root, 300, 150));
                    settingsStage.showAndWait();
                } catch (IOException e) {
                    System.out.println("Error loading settings window: " + e.getMessage());
                }
            }
        }
        // Add fade-in animation for the main content
        fadeInContent();

        // Add hover animations for buttons
        addButtonHoverEffects();

        // Initialize progress bar as invisible
        downloadProgressBar.setVisible(false);
    }

    private void showLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("fxml/loginWindow.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(loader.load()));
            LoginWindowController controller = loader.getController();
            controller.setLoginCheckBox(loginCheckBox);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showInstalledItemMessage(String title) {
        installedItemLabel.setText("This item is already installed: " + title);
        installedItemLabel.getStyleClass().add("installed-item-label");
        installedItemLabel.setVisible(true);
    }


    @FXML
    private void urlEntered() {
        String url = urlTextField.getText();
        if (isValidUrl(url)) {
            publishedFileId = getPublishedFileIdFromUrl(url);
            if (publishedFileId!= null) {
                Task<String> task = new Task<String>() {
                    @Override
                    protected String call() {
                        List<Long> arrayForOneItem = new ArrayList<Long>();
                        arrayForOneItem.add(Long.valueOf(publishedFileId));
                        return APICaller.sendPostRequestCheckItemDetails(arrayForOneItem);
                    }
                };

                task.setOnSucceeded(event -> {
                    String response = task.getValue();
                    if (response!= null) {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONObject responseObj = jsonObject.getJSONObject("response");
                        JSONArray publishedFileDetails = responseObj.getJSONArray("publishedfiledetails");
                        if (!publishedFileDetails.isEmpty()) {
                            JSONObject publishedFileDetail = publishedFileDetails.getJSONObject(0);
                            String title = publishedFileDetail.getString("title");
                            long fileSize = publishedFileDetail.getLong("file_size");
                            long timeUpdated = publishedFileDetail.getLong("time_updated");
                            long timeUploaded = publishedFileDetail.getLong("time_created");
                            String imageUrl = publishedFileDetail.getString("preview_url");
                            long creatorAppId = publishedFileDetail.getLong("creator_app_id");
                            appId = creatorAppId;
                            String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;
                            if (SteamCMDInteractor.isFileDownloaded(filePath)) {
                                showInstalledItemMessage("This item is already installed on your system.");
                                System.out.println("File exists :" + filePath);
                            } else {
                                installedItemLabel.setVisible(false);
                                System.out.println("File doesn't exist: " + filePath);
                            }

                            // Convert file size to human-readable format
                            String fileSizeHumanReadable = humanReadableFileSize(fileSize);

                            // Convert time updated and uploaded to human-readable format
                            String timeUpdatedHumanReadable = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeUpdated * 1000));
                            String timeUploadedHumanReadable = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeUploaded * 1000));

                            // Update the UI here
                            fadeTransition(imageView, 0.5, 1.0, 1000);
                            fadeTransition(titleLabel, 0.5, 1.0, 1000);
                            fadeTransition(gameName, 0.5, 1.0, 1000);
                            fadeTransition(gameReleaseDate, 0.5, 1.0, 1000);
                            fadeTransition(uploadDate, 0.5, 1.0, 1000);
                            fadeTransition(updateDate, 0.5, 1.0, 1000);
                            fadeTransition(size, 0.5, 1.0, 1000);
                            titleLabel.setText(title);
                            size.setText(fileSizeHumanReadable);
                            updateDate.setText(timeUpdatedHumanReadable);
                            uploadDate.setText(timeUploadedHumanReadable);
                            imageView.setImage(ImageDownloader.downloadImage(imageUrl));

                            // Check if the item is a collection
                            if (creatorAppId!= 766) {
                                collectionItemsLabel.setVisible(false);
                                collectionButton.setVisible(false);
                                Thread gameInfoThread = new Thread(() -> {
                                    try {
                                        getGameInfo(creatorAppId);
                                    } catch (ParseException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                gameInfoThread.start();
                            } else {
                                collectionItemsLabel.setVisible(true);
                                collectionButton.setVisible(true);
                                getCollectionDetails(publishedFileId);
                            }
                        }
                    }
                });

                task.setOnFailed(event -> {
                    // Handle failure here
                });

                Thread thread = new Thread(task);
                thread.start();
            }
        }
    }


    private boolean isValidUrl(String url) {
        String regex = "^https?:\\/\\/steamcommunity\\.com\\/sharedfiles\\/filedetails\\/\\?id=\\d+.*$";
        return url.matches(regex);
    }

    private String getPublishedFileIdFromUrl(String url) {
        URI uri = URI.create(url);
        String query = uri.getQuery();
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue[0].equals("id")) {
                return keyValue[1];
            }
        }
        return null; // or throw an exception, depending on your requirements
    }

    private String humanReadableFileSize(long fileSize) {
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

    private void getGameInfo(long creatorAppId) throws ParseException {
        String apiUrl = "https://store.steampowered.com/api/appdetails?appids=" + creatorAppId;
        String response = APICaller.sendGetRequest(apiUrl);
        if (response!= null) {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject gameInfo = jsonObject.getJSONObject(String.valueOf(creatorAppId));
            if (gameInfo.getBoolean("success")) {
                JSONObject data = gameInfo.getJSONObject("data");
                String gameNameLabel = data.getString("name");
                String gameReleaseDateLabel = data.getJSONObject("release_date").getString("date");
                String gamePreviewImage = data.getString("header_image");



                // Update the UI with the game information
                gameName.setText(gameNameLabel);
                gameReleaseDate.setText(gameReleaseDateLabel);
                gameImage.setImage(ImageDownloader.downloadImage(gamePreviewImage));
            } else {
                System.out.println("Failed to get game information");
            }
        } else {
            System.out.println("Failed to send GET request");
        }
    }

    private void getCollectionDetails(String collectionId) {
        String apiUrl = "https://api.steampowered.com/ISteamRemoteStorage/GetCollectionDetails/v1/";
        String postData = "collectioncount=1&publishedfileids[0]=" + collectionId;
        String response = APICaller.sendPostCollectionInfoRequest(apiUrl, postData);
        if (response!= null) {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject responseObj = jsonObject.getJSONObject("response");
            JSONArray collectionDetails = responseObj.getJSONArray("collectiondetails");
            if (!collectionDetails.isEmpty()) {
                JSONObject collectionDetail = collectionDetails.getJSONObject(0);
                if(collectionDetail.getJSONArray("children") == null){
                    System.out.println("jsonarray children is null");
                    return;
                }
                JSONArray children = collectionDetail.getJSONArray("children");
                collectionItemsLabel.setVisible(true);
                collectionItemsLabel.setText("Items in collection: " + children.length());
                if(collectionIds == null){
                    collectionIds = new ArrayList<>();
                } else {
                    collectionIds.clear();
                }
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    long publishedFileId = child.getLong("publishedfileid");


                    collectionIds.add(publishedFileId);
                    // Create a new Text node for each workshop item
                }
            }
        }
    }

    @FXML
    private void settingsButtonClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("fxml/settings.fxml"));
            Parent root = loader.load();
            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(root, 300, 150));
            settingsStage.showAndWait();
        } catch (IOException e) {
            System.out.println("Error loading settings window: " + e.getMessage());
        }
    }

    @FXML
    private void downloadButtonClicked() {
        try {
            if (publishedFileId != null && appId != 766) {
                if (SteamCMDInteractor.isFileDownloaded(SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId)) {
                    // Show a dialog to confirm the redownload
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Redownload");
                    alert.setHeaderText("This item is already installed. Do you want to redownload it?");
                    alert.setContentText("By proceeding, you will redownload the item, which may overwrite your existing files.");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        recentDownloadsController.addRecentDownload(new RecentDownload(titleLabel.getText(), imageView.getImage(), size.getText(),
                                "downloading", null, publishedFileId, String.valueOf(appId)));
                        NotificationController.updateRecentDownloadsNotification(true, recentDownloadsNotification);
                        SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
                        showTemporaryMessage("Download started");
                    }
                } else {
                    recentDownloadsController.addRecentDownload(new RecentDownload(titleLabel.getText(), imageView.getImage(), size.getText(),
                            "downloading", null, publishedFileId, String.valueOf(appId)));
                    NotificationController.updateRecentDownloadsNotification(true, recentDownloadsNotification);
                    SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
                    showTemporaryMessage("Download started");
                }
            } else if (publishedFileId != null && collectionIds != null && !collectionIds.isEmpty()) { //This is a collection (AppId - 766)
                Task<Void> downloadTask = SteamCMDInteractor.createDownloadCollectionItemsTask(collectionIds, recentDownloadsController);

                downloadTask.setOnSucceeded(event -> {
                    Platform.runLater(() -> showTemporaryMessage("Collection download completed"));
                });

                downloadTask.setOnFailed(event -> {
                    Platform.runLater(() -> showTemporaryMessage("Collection download failed"));
                });

                new Thread(downloadTask).start();
                showTemporaryMessage("Collection download started");
                // Show progress bar with animation
                downloadProgressBar.setVisible(true);
                downloadProgressBar.setProgress(0);
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(downloadProgressBar.progressProperty(), 0)),
                        new KeyFrame(Duration.seconds(2), new KeyValue(downloadProgressBar.progressProperty(), 1))

                );
                // Hide progress bar after download completes
                timeline.setOnFinished(event -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(500), downloadProgressBar);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.0);
                    ft.setOnFinished(e -> downloadProgressBar.setVisible(false));
                    ft.play();
                });
                timeline.play();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    private void recentDownloadsButtonClicked() {
        openRecentDownloadsWindow();
        NotificationController.updateRecentDownloadsNotification(false, recentDownloadsNotification);


    }
    @FXML
    private void openInstalledItemFolder() {
        if (publishedFileId != null && appId != 766) {
            String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;
            File file = new File(filePath);
            if (file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException e) {
                    showErrorMessage("Failed to open the installed item folder.");
                }
            } else {
                showErrorMessage("The installed item folder does not exist.");
            }
        } else {
            showErrorMessage("No installed item found.");
        }
    }

    public void openRecentDownloadsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("fxml/RecentDownloads.fxml"));
            loader.setController(recentDownloadsController);
            Parent root = loader.load();
//            RecentDownloadsController recentDownloadsController1 = loader.getController();
//            recentDownloadsController1.setRecentDownloads(recentDownloadsController.getRecentDownloads());
            if(recentDownloadsController.getNotificationLabel() == null){
                recentDownloadsController.setNotificationLabel(recentDownloadsNotification);
            }
            recentDownloadsController.updateRecentDownloadsList();
//            recentDownloadsController.updateRecentDownloadsList();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Recent Downloads");
            stage.setScene(new Scene(root, 600, 400));
            // Set the onCloseRequest handler for the stage
            stage.setOnCloseRequest(event -> recentDownloadsController.closeButtonClicked());


            stage.show();


        } catch (IOException e) {
            System.out.println("Error loading recent downloads window: " + e.getMessage());
        }
    }
    private void fadeInContent() {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), centerContent);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }
    private void fadeTransition(javafx.scene.Node node, double fromValue, double toValue, int durationMillis) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMillis), node);
        ft.setFromValue(fromValue);
        ft.setToValue(toValue);
        ft.play();
    }


    private void addButtonHoverEffects() {
        addHoverEffect(downloadButton);
        addHoverEffect(settingsButton);
        addHoverEffect(aboutButton);
        addHoverEffect(collectionButton);
    }
    private void addHoverEffect(Button button) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), button);
        scaleTransition.setToX(1.05);
        scaleTransition.setToY(1.05);

        button.setOnMouseEntered(e -> scaleTransition.playFromStart());
        button.setOnMouseExited(e -> {
            scaleTransition.setRate(-1);
            scaleTransition.play();
        });
    }
    private void showTemporaryMessage(String message) {
        temporaryMessageLabel.setText(message);
        temporaryMessageLabel.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), temporaryMessageLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), temporaryMessageLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> temporaryMessageLabel.setVisible(false));
            fadeOut.play();
        });

        pause.play();
    }
    @FXML
    private void collectionButtonClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("fxml/collection-items.fxml"));
            Parent root = loader.load();
            collectionItemsController = loader.getController();
            collectionItemsController.setRecentDownloadsController(recentDownloadsController);
            collectionItemsController.loadCollectionItems(collectionIds);
            collectionItemsController.setSlideInAnimation();

            Scene scene = new Scene(root);
            Stage newStage = new Stage();
            newStage.setTitle("Collection Items");
            newStage.setScene(scene);
            newStage.setMinHeight(1000);
            newStage.setMinWidth(600);
            newStage.show();

            newStage.setOnCloseRequest(event -> {
                recentDownloadsController.removeObserver(collectionItemsController);
                collectionItemsController = null;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showErrorMessage(String message) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), errorMessageLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), errorMessageLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> errorMessageLabel.setVisible(false));
            fadeOut.play();
        });

        pause.play();
    }
    // This method should be called when a download status changes from the main window
    public void updateDownloadStatus(String publishedFileId, String status) {
        recentDownloadsController.updateRecentDownloadStatusAndFilepath(publishedFileId, status, null);
        if (collectionItemsController != null) {
            collectionItemsController.updateItemStatus(publishedFileId, status);
        }
    }
}