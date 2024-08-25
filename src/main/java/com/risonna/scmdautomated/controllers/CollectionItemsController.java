package com.risonna.scmdautomated.controllers;

import com.risonna.scmdautomated.model.APICaller;
import com.risonna.scmdautomated.model.ImageDownloader;
import com.risonna.scmdautomated.model.SteamCMDInteractor;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.risonna.scmdautomated.misc.DataUtils.getHumanReadableFileSize;

public class CollectionItemsController implements DownloadStatusObserver {

    @FXML
    private VBox itemsContainer;
    @FXML
    private Parent root;

    private RecentDownloadsController recentDownloadsController;
    private final Map<String, HBox> itemPanes = new HashMap<>();

    public void setRecentDownloadsController(RecentDownloadsController controller) {
        this.recentDownloadsController = controller;
        controller.addObserver(this);
    }

    @Override
    public void onDownloadStatusChanged(String publishedFileId, String status) {
        Platform.runLater(() -> updateItemStatus(publishedFileId, status));
    }

    public void setSlideInAnimation() {
        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.5), root);
        slideIn.setFromX(root.getLayoutX());
        slideIn.setToX(0);
        slideIn.play();
    }

    public void loadCollectionItems(List<Long> ids) {
        Platform.runLater(() -> itemsContainer.getChildren().clear());
        itemPanes.clear();

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() {
                String response = APICaller.sendPostRequestCheckItemDetails(ids);
                if (response != null) {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray publishedFileDetails = jsonObject.getJSONObject("response")
                            .getJSONArray("publishedfiledetails");

                    for (int i = 0; i < publishedFileDetails.length(); i++) {
                        JSONObject publishedFileDetail = publishedFileDetails.getJSONObject(i);
                        HBox itemPane = createItemPane(publishedFileDetail);
                        if (itemPane != null) {
                            Platform.runLater(() -> itemsContainer.getChildren().add(itemPane));
                        }
                    }
                }
                return null;
            }
        };

        Thread loadThread = new Thread(loadTask);
        loadThread.start();
    }

    private HBox createItemPane(JSONObject publishedFileDetail) {
        String title = publishedFileDetail.optString("title", null);
        long fileSize = publishedFileDetail.optLong("file_size", -1);
        String imageUrl = publishedFileDetail.optString("preview_url", "https://albums193.zbporn.com/main/9998x9998/98000/98571/2335763.jpg");
        long publishedFileId = publishedFileDetail.optLong("publishedfileid", -1);
        long appId = publishedFileDetail.optLong("consumer_app_id", -1);

        if (title == null || fileSize == -1 || publishedFileId == -1 || appId == -1) {
            System.out.println("Missing mandatory field(s) in JSON: " + publishedFileDetail.toString());
            return null;
        }

        HBox itemPane = new HBox(10);
        itemPane.getStyleClass().add("item-pane");

        String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;

        ImageView imageView = new ImageView(ImageDownloader.downloadImage(imageUrl));
        imageView.setFitHeight(100);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);

        VBox detailsBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("item-title");
        Label sizeLabel = new Label("Size: " + getHumanReadableFileSize(fileSize));
        sizeLabel.getStyleClass().add("item-size");

        detailsBox.getChildren().addAll(titleLabel, sizeLabel);

        Button actionButton = new Button();
        actionButton.getStyleClass().add("action-button");
        actionButton.setUserData(publishedFileId);

        RecentDownload existingDownload = recentDownloadsController.getDownloadByPublishedFileId(String.valueOf(publishedFileId));
        if (SteamCMDInteractor.isFileDownloaded(filePath)) {
            actionButton.setText("Installed");
            actionButton.getStyleClass().add("installed-button");
            actionButton.setDisable(true);
        } else if (existingDownload != null) {
            updateButtonState(existingDownload.getDownloadStatus(), actionButton);
        } else {
            actionButton.setText("Download");
            actionButton.setOnAction(e -> downloadItem(String.valueOf(publishedFileId), title, imageView.getImage(), String.valueOf(fileSize), appId));
        }

        detailsBox.getChildren().add(actionButton);
        itemPane.getChildren().addAll(imageView, detailsBox);
        itemPanes.put(String.valueOf(publishedFileId), itemPane);

        animateItemPane(itemPane);

        return itemPane;
    }

    private void updateButtonState(String status, Button actionButton) {
        Platform.runLater(() -> {
            actionButton.getStyleClass().remove("installed-button");
            switch (status) {
                case "failed":
                    actionButton.setText("Retry");
                    actionButton.setDisable(false);
                    actionButton.setOnAction(e -> retryDownload(actionButton.getUserData().toString()));
                    break;
                case "success":
                    actionButton.setText("Installed");
                    actionButton.getStyleClass().add("installed-button");
                    actionButton.setDisable(true);
                    break;
                default:
                    actionButton.setText(status);
                    actionButton.setDisable(true);
                    break;
            }
        });
    }

    private void retryDownload(String publishedFileId) {
        RecentDownload download = recentDownloadsController.getDownloadByPublishedFileId(publishedFileId);
        if (download != null) {
            downloadItem(publishedFileId, download.getName(), download.getImage(), download.getFileSize(), Long.parseLong(download.getAppId()));
        }
    }

    private void downloadItem(String publishedFileId, String title, Image image, String size, long appId) {
        String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;

        if (SteamCMDInteractor.isFileDownloaded(filePath)) {
            Alert alert = createRedownloadConfirmationDialog();
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                startDownload(publishedFileId, title, image, size, appId);
            }
        } else {
            startDownload(publishedFileId, title, image, size, appId);
        }
    }

    private void startDownload(String publishedFileId, String title, Image image, String size, long appId) {
        RecentDownload newDownload = new RecentDownload(title, image, size, "downloading", null, publishedFileId, String.valueOf(appId));
        recentDownloadsController.addRecentDownload(newDownload);
        NotificationController.updateRecentDownloadsNotification(true, recentDownloadsController.getNotificationLabel());
        SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
        updateItemStatus(publishedFileId, "downloading");
    }

    public void updateItemStatus(String publishedFileId, String status) {
        HBox itemPane = itemPanes.get(publishedFileId);
        if (itemPane != null) {
            VBox detailsBox = (VBox) itemPane.getChildren().get(1);
            Button actionButton = (Button) detailsBox.getChildren().get(detailsBox.getChildren().size() - 1);
            updateButtonState(status, actionButton);
        }
    }
    private Alert createRedownloadConfirmationDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Redownload");
        alert.setHeaderText("This item is already installed. Do you want to redownload it?");
        alert.setContentText("By proceeding, you will redownload the item, which may overwrite your existing files.");
        return alert;
    }

    private void animateItemPane(HBox itemPane) {
        itemPane.setOpacity(0);
        itemPane.setTranslateY(10);
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), new KeyValue(itemPane.opacityProperty(), 1)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), new KeyValue(itemPane.translateYProperty(), 0)));
        timeline.play();
    }
}
