package com.risonna.scmdautomated.controllers;

import com.risonna.scmdautomated.HelloApplication;
import com.risonna.scmdautomated.model.*;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import com.risonna.scmdautomated.model.entities.UserSession;
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

import static com.risonna.scmdautomated.misc.DataUtils.getHumanReadableFileSize;

public class MainController {
    @FXML private CheckBox loginCheckBox;
    @FXML private Button downloadButton;
    @FXML private ImageView imageView;
    @FXML private TextField urlTextField;
    @FXML private Text titleLabel;
    @FXML private Text uploadDate;
    @FXML private Text updateDate;
    @FXML private Text size;
    @FXML private Text gameName;
    @FXML private Text gameReleaseDate;
    @FXML private ImageView gameImage;
    @FXML private Text errorMessageLabel;
    @FXML private Button collectionButton;
    @FXML private Text collectionItemsLabel;
    @FXML private Button settingsButton;
    @FXML private Button aboutButton;
    @FXML private ProgressBar downloadProgressBar;
    @FXML private StackPane recentDownloadsStackPane;
    @FXML private Label recentDownloadsNotification;
    @FXML private Label temporaryMessageLabel;
    @FXML private VBox centerContent;
    @FXML private Label installedItemLabel;
    @FXML private Button openFolderButton;

    private String publishedFileId;
    private long appId;
    private static RecentDownloadsController recentDownloadsController;
    private List<Long> collectionIds;
    private static CollectionItemsController collectionItemsController;

    public MainController() {
        recentDownloadsController = new RecentDownloadsController();
    }

    @FXML
    private void initialize() {
        initializeUI();
        setupEventHandlers();
        checkSteamcmdPath();
        applyAnimations();
    }

    private void initializeUI() {
        NotificationController.updateRecentDownloadsNotification(false, recentDownloadsNotification);
        Image dog = new Image(getClass().getResourceAsStream("/com/risonna/scmdautomated/images/dog_copy.png"));
        imageView.setImage(dog);
        titleLabel.setText("No item selected");
        downloadProgressBar.setVisible(false);
        recentDownloadsController.setNotificationLabel(recentDownloadsNotification);
    }

    private void setupEventHandlers() {
        loginCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!UserSession.getInstance().isLoggedIn() && newValue) {
                showLoginWindow();
            } else {
                UserSession.getInstance().setLoggedIn(false);
            }
        });
    }

    private void checkSteamcmdPath() {
        File steamcmdPathFile = new File(SettingsController.STEAMCMD_PATH_FILE);
        if (!steamcmdPathFile.exists()) {
            showSteamcmdPathAlert();
        }
    }

    private void showSteamcmdPathAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Attention");
        alert.setHeaderText("SteamCMD path is required");
        alert.setContentText("Please enter a valid steamcmd path in the settings");

        alert.getButtonTypes().remove(ButtonType.CANCEL);
        ButtonType settingsButtonType = new ButtonType("Settings");
        alert.getButtonTypes().setAll(settingsButtonType);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();

        alertStage.setOnCloseRequest(event -> Platform.exit());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == settingsButtonType) {
            openSettingsWindow();
        }
    }

    private void openSettingsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("fxml/settings.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(root));

            // Disable 'X' button on settings window until a path is set
            settingsStage.setOnCloseRequest(event -> {
                if(!new File(SettingsController.STEAMCMD_PATH_FILE).exists()){
                    event.consume();
                }
            });

            settingsStage.showAndWait();
        } catch (IOException e) {
            System.out.println("Error loading settings window: " + e.getMessage());
        }
    }

    private void applyAnimations() {
        fadeInContent();
        addButtonHoverEffects();
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

    @FXML
    private void urlEntered() {
        String url = urlTextField.getText();
        if (isValidUrl(url)) {
            publishedFileId = getPublishedFileIdFromUrl(url);
            if (publishedFileId != null) {
                fetchItemDetails();
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
        return null;
    }

    private void fetchItemDetails() {
        Task<String> task = createFetchItemDetailsTask();
        task.setOnSucceeded(event -> handleFetchItemDetailsSuccess(task.getValue()));
        task.setOnFailed(event -> handleFetchItemDetailsFailure());
        new Thread(task).start();
    }

    private Task<String> createFetchItemDetailsTask() {
        return new Task<String>() {
            @Override
            protected String call() {
                List<Long> arrayForOneItem = new ArrayList<>();
                arrayForOneItem.add(Long.valueOf(publishedFileId));
                return APICaller.sendPostRequestCheckItemDetails(arrayForOneItem);
            }
        };
    }

    private void handleFetchItemDetailsSuccess(String response) {
        if (response != null) {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject responseObj = jsonObject.getJSONObject("response");
            JSONArray publishedFileDetails = responseObj.getJSONArray("publishedfiledetails");
            if (!publishedFileDetails.isEmpty()) {
                JSONObject publishedFileDetail = publishedFileDetails.getJSONObject(0);
                updateUIWithItemDetails(publishedFileDetail);
                checkIfItemIsInstalled(publishedFileDetail);
                handleCollectionOrSingleItemInfo(publishedFileDetail);
            }
        }
    }

    private void handleFetchItemDetailsFailure() {
        Platform.runLater(() -> showErrorMessage("Failed to fetch item details. Please try again."));
    }

    private void updateUIWithItemDetails(JSONObject publishedFileDetail) {
        String title = publishedFileDetail.getString("title");
        long fileSize = publishedFileDetail.getLong("file_size");
        long timeUpdated = publishedFileDetail.getLong("time_updated");
        long timeUploaded = publishedFileDetail.getLong("time_created");
        String imageUrl = publishedFileDetail.getString("preview_url");

        String fileSizeHumanReadable = getHumanReadableFileSize(fileSize);
        String timeUpdatedHumanReadable = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeUpdated * 1000));
        String timeUploadedHumanReadable = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeUploaded * 1000));

        Platform.runLater(() -> {
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
        });
    }

    private void checkIfItemIsInstalled(JSONObject publishedFileDetail) {
        long creatorAppId = publishedFileDetail.getLong("creator_app_id");
        appId = creatorAppId;
        String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;
        if (SteamCMDInteractor.isFileDownloaded(filePath)) {
            Platform.runLater(() -> showInstalledItemMessage("This item is already installed on your system."));
        } else {
            Platform.runLater(() -> {
                openFolderButton.setVisible(false);
                installedItemLabel.setVisible(false);});
        }
    }

    private void handleCollectionOrSingleItemInfo(JSONObject publishedFileDetail) {
        long creatorAppId = publishedFileDetail.getLong("creator_app_id");
        if (creatorAppId != 766) {
            handleSingleItemInfo(creatorAppId);
        } else {
            long consumerAppId = publishedFileDetail.getLong("consumer_app_id");
            handleCollectionInfo(publishedFileId, consumerAppId);
        }
    }

    private void handleSingleItemInfo(long creatorAppId) {
        Platform.runLater(() -> {
            collectionItemsLabel.setVisible(false);
            collectionButton.setVisible(false);
        });
        Thread gameInfoThread = new Thread(() -> {
            try {
                getGameInfo(creatorAppId);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
        gameInfoThread.start();
    }

    private void handleCollectionInfo(String collectionId, long consumerAppId) {
        Platform.runLater(() -> {
            collectionItemsLabel.setVisible(true);
            collectionButton.setVisible(true);
        });
        getCollectionDetails(collectionId);
        Thread gameInfoThread = new Thread(() -> {
            try {
                getGameInfo(consumerAppId);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
        gameInfoThread.start();
    }

    private void getGameInfo(long creatorAppId) throws ParseException {
        String apiUrl = "https://store.steampowered.com/api/appdetails?appids=" + creatorAppId;
        String response = APICaller.sendGetRequest(apiUrl);
        if (response != null) {
            JSONObject jsonObject = new JSONObject(response);
            System.out.println("creator app id is " + creatorAppId);
            JSONObject gameInfo = jsonObject.getJSONObject(String.valueOf(creatorAppId));
            if (gameInfo.getBoolean("success")) {
                JSONObject data = gameInfo.getJSONObject("data");
                String gameNameLabel = data.getString("name");
                String gameReleaseDateLabel = data.getJSONObject("release_date").getString("date");
                String gamePreviewImage = data.getString("header_image");

                Platform.runLater(() -> {
                    gameName.setText(gameNameLabel);
                    gameReleaseDate.setText(gameReleaseDateLabel);
                    gameImage.setImage(ImageDownloader.downloadImage(gamePreviewImage));
                });
            } else {
                Platform.runLater(() -> showErrorMessage("Failed to get game information"));
            }
        } else {
            Platform.runLater(() -> showErrorMessage("Failed to send GET request"));
        }
    }

    private void getCollectionDetails(String collectionId) {
        String apiUrl = "https://api.steampowered.com/ISteamRemoteStorage/GetCollectionDetails/v1/";
        String postData = "collectioncount=1&publishedfileids[0]=" + collectionId;
        String response = APICaller.sendPostCollectionInfoRequest(apiUrl, postData);
        if (response != null) {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject responseObj = jsonObject.getJSONObject("response");
            JSONArray collectionDetails = responseObj.getJSONArray("collectiondetails");
            if (!collectionDetails.isEmpty()) {
                JSONObject collectionDetail = collectionDetails.getJSONObject(0);
                if (collectionDetail.getJSONArray("children") == null) {
                    Platform.runLater(() -> showErrorMessage("Collection is empty"));
                    return;
                }
                JSONArray children = collectionDetail.getJSONArray("children");
                Platform.runLater(() -> {
                    collectionItemsLabel.setVisible(true);
                    collectionItemsLabel.setText("Items in collection: " + children.length());
                });
                if (collectionIds == null) {
                    collectionIds = new ArrayList<>();
                } else {
                    collectionIds.clear();
                }
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    long publishedFileId = child.getLong("publishedfileid");
                    collectionIds.add(publishedFileId);
                }
            }
        }
    }

    @FXML
    private void settingsButtonClicked() {
        openSettingsWindow();
    }

    @FXML
    private void downloadButtonClicked() {
        if (publishedFileId == null) {
            showErrorMessage("No item selected for download");
            return;
        }

        if (appId != 766) {
            handleSingleItemDownload();
        } else if (collectionIds != null && !collectionIds.isEmpty()) {
            handleCollectionDownload();
        } else {
            showErrorMessage("No valid item or collection to download");
        }
    }

    private void handleSingleItemDownload() {
        String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;
        if (SteamCMDInteractor.isFileDownloaded(filePath)) {
            confirmAndRedownload();
        } else {
            startDownload();
        }
    }

    private void confirmAndRedownload() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Redownload");
        alert.setHeaderText("This item is already installed. Do you want to redownload it?");
        alert.setContentText("By proceeding, you will redownload the item, which may overwrite your existing files.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            startDownload();
        }
    }

    private void startDownload() {
        recentDownloadsController.addRecentDownload(new RecentDownload(titleLabel.getText(), imageView.getImage(), size.getText(),
                "downloading", null, publishedFileId, String.valueOf(appId)));
        NotificationController.updateRecentDownloadsNotification(true, recentDownloadsNotification);
        SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
        showTemporaryMessage("Download started");
    }

    private void handleCollectionDownload() {
        Task<Void> downloadTask = SteamCMDInteractor.createDownloadCollectionItemsTask(collectionIds, recentDownloadsController);
        configureCollectionDownloadTask(downloadTask);
        new Thread(downloadTask).start();
        showTemporaryMessage("Collection download attempt");
        showDownloadProgress();
    }

    private void configureCollectionDownloadTask(Task<Void> downloadTask) {
        downloadTask.setOnSucceeded(event -> Platform.runLater(() -> showTemporaryMessage("Collection download initiated")));
        downloadTask.setOnFailed(event -> Platform.runLater(() -> showTemporaryMessage("Collection download failed")));
    }

    private void showDownloadProgress() {
        downloadProgressBar.setVisible(true);
        downloadProgressBar.setProgress(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(downloadProgressBar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(2), new KeyValue(downloadProgressBar.progressProperty(), 1))
        );
        configureProgressBarFadeOut(timeline);
        timeline.play();
    }

    private void configureProgressBarFadeOut(Timeline timeline) {
        timeline.setOnFinished(event -> {
            FadeTransition ft = new FadeTransition(Duration.millis(500), downloadProgressBar);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> downloadProgressBar.setVisible(false));
            ft.play();
        });
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
            if (recentDownloadsController.getNotificationLabel() == null) {
                recentDownloadsController.setNotificationLabel(recentDownloadsNotification);
            }
            recentDownloadsController.setupRecentDownloadsList();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Recent Downloads");
            stage.setScene(new Scene(root, 600, 400));
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

    private void showInstalledItemMessage(String message) {
        installedItemLabel.setText(message);
        installedItemLabel.getStyleClass().add("installed-item-label");
        installedItemLabel.setVisible(true);
        openFolderButton.setVisible(true);
    }
}