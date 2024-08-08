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
import javafx.collections.ListChangeListener;
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

public class CollectionItemsController implements DownloadStatusObserver{

    @FXML
    private VBox itemsContainer;
    private RecentDownloadsController recentDownloadsController;
    private Map<String, HBox> itemPanes = new HashMap<>();
    public void setRecentDownloadsController(RecentDownloadsController controller) {
        this.recentDownloadsController = controller;
        controller.addObserver(this);
    }
    @Override
    public void onDownloadStatusChanged(String publishedFileId, String status) {
        Platform.runLater(() -> updateItemStatus(publishedFileId, status));
    }
    @FXML
    private Parent root;
    public void setSlideInAnimation() {
        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.5), root);
        slideIn.setFromX(root.getLayoutX());
        slideIn.setToX(0);
        slideIn.play();
    }





    public void initialize() {
        // This method is called after the FXML file has been loaded
    }

    public void loadCollectionItems(List<Long> ids) {
        // Clear existing items
        Platform.runLater(() -> itemsContainer.getChildren().clear());
        itemPanes.clear();

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() {
                String response = APICaller.sendPostRequestCheckItemDetails(ids);
                if (response != null) {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject responseObj = jsonObject.getJSONObject("response");
                    JSONArray publishedFileDetails = responseObj.getJSONArray("publishedfiledetails");

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

    private HBox createPlaceholderItem() {
        HBox placeholderItem = new HBox(10);
        placeholderItem.getStyleClass().add("item-pane");
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(100, 100);
        placeholderItem.getChildren().add(loadingIndicator);
        return placeholderItem;
    }
    private void loadItem(JSONObject publishedFileDetail, HBox placeholderItem) {
        // Create the actual item in the background
        HBox itemPane = createItemPane(publishedFileDetail);

        // Replace the placeholder item with the actual item
        Platform.runLater(() -> {
            if(itemPane == null){
                itemsContainer.getChildren().remove(placeholderItem);
                return;
            }
            int index = itemsContainer.getChildren().indexOf(placeholderItem);
            itemsContainer.getChildren().set(index, itemPane);
        });
    }

    private HBox createItemPane(JSONObject publishedFileDetail) {
        HBox itemPane = new HBox(10);
        itemPane.getStyleClass().add("item-pane");
        String title;
        if(publishedFileDetail.has("title")) {
            title = publishedFileDetail.getString("title");
        }
        else {
            System.out.println("title null");
            return null;
        }
        long fileSize;
        if(publishedFileDetail.has("file_size")) {
            fileSize = publishedFileDetail.getLong("file_size");
        } else {
            System.out.println("filesize null");
            return null;
        }
        String imageUrl = null;
        if(publishedFileDetail.has("preview_url")) {
            imageUrl = publishedFileDetail.getString("preview_url");
        } else {
            System.out.println("image null");
            imageUrl = "https://albums193.zbporn.com/main/9998x9998/98000/98571/2335763.jpg";
        }
        long publishedFileId;
        if(publishedFileDetail.has("publishedfileid")) {
            publishedFileId = publishedFileDetail.getLong("publishedfileid");
        } else {
            System.out.println("publishedFieldId null");
            return null;
        }
        long appId;
        if(publishedFileDetail.has("consumer_app_id")) {
            appId = publishedFileDetail.getLong("consumer_app_id");
         } else {
            System.out.println("title null");
             return null;
         }
        String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;

        // Create UI elements
        ImageView imageView = new ImageView(ImageDownloader.downloadImage(imageUrl));
        imageView.setFitHeight(100);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);


        VBox detailsBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("item-title");
        Label sizeLabel = new Label("Size: " + humanReadableFileSize(fileSize));
        sizeLabel.getStyleClass().add("item-size");



        detailsBox.getChildren().addAll(titleLabel, sizeLabel);
        RecentDownload existingDownload = recentDownloadsController.getDownloadByPublishedFileId(String.valueOf(publishedFileId));

        Button actionButton = new Button();
        actionButton.getStyleClass().add("action-button");
        actionButton.setUserData(publishedFileId);

        if (SteamCMDInteractor.isFileDownloaded(filePath)) {
            actionButton.setText("Installed");
            actionButton.getStyleClass().add("installed-button");
            actionButton.setDisable(true);
        } else {
            existingDownload = recentDownloadsController.getDownloadByPublishedFileId(String.valueOf(publishedFileId));
            if (existingDownload != null) {
                updateButtonState(existingDownload.getDownloadStatus(), actionButton);
            } else {
                actionButton.setText("Download");
                actionButton.setOnAction(e -> downloadItem(String.valueOf(publishedFileId), title, imageView.getImage(), String.valueOf(fileSize), appId));
            }
        }


        detailsBox.getChildren().add(actionButton);
        itemPane.getChildren().addAll(imageView, detailsBox);

        itemPanes.put(String.valueOf(publishedFileId), itemPane);

        // Add some animations to make it look smoother
        itemPane.setOpacity(0);
        itemPane.setTranslateY(10);
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), new KeyValue(itemPane.opacityProperty(), 1)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), new KeyValue(itemPane.translateYProperty(), 0)));
        timeline.play();



        return itemPane;
    }

    private void updateButtonState(String status, Button actionButton) {
        Platform.runLater(() -> {
            actionButton.getStyleClass().remove("installed-button");
            if ("failed".equals(status)) {
                actionButton.setText("Retry");
                actionButton.setDisable(false);
                actionButton.setOnAction(e -> retryDownload(actionButton.getUserData().toString()));
            } else if ("success".equals(status)) {
                actionButton.setText("Installed");
                actionButton.getStyleClass().add("installed-button");
                actionButton.setDisable(true);
            } else {
                actionButton.setText(status);
                actionButton.setDisable(true);
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
            // Show a dialog to confirm the redownload
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Redownload");
            alert.setHeaderText("This item is already installed. Do you want to redownload it?");
            alert.setContentText("By proceeding, you will redownload the item, which may overwrite your existing files.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                RecentDownload newDownload = new RecentDownload(title, image, size, "downloading", null, publishedFileId, String.valueOf(appId));
                recentDownloadsController.addRecentDownload(newDownload);
                NotificationController.updateRecentDownloadsNotification(true, recentDownloadsController.getNotificationLabel());
                SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
                updateItemStatus(publishedFileId, "downloading");
            }
        } else {
            RecentDownload newDownload = new RecentDownload(title, image, size, "downloading", null, publishedFileId, String.valueOf(appId));
            recentDownloadsController.addRecentDownload(newDownload);
            NotificationController.updateRecentDownloadsNotification(true, recentDownloadsController.getNotificationLabel());
            SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
            updateItemStatus(publishedFileId, "downloading");
        }
    }

    public void updateItemStatus(String publishedFileId, String status) {
        HBox itemPane = itemPanes.get(publishedFileId);
        if (itemPane != null) {
            VBox detailsBox = (VBox) itemPane.getChildren().get(1);
            Button actionButton = (Button) detailsBox.getChildren().get(detailsBox.getChildren().size() - 1);
            updateButtonState(status, actionButton);
        }
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

}