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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
        try {
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
            itemPane.setAlignment(Pos.CENTER_LEFT);

            String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;

            ImageView imageView = new ImageView(ImageDownloader.downloadImage(imageUrl));
            imageView.setFitHeight(100);
            imageView.setFitWidth(100);
            imageView.setPreserveRatio(true);

            HBox detailsBox = new HBox(5);
            detailsBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(detailsBox, Priority.ALWAYS);

            VBox labelsAndButton = new VBox(5);
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("item-title");
            Label sizeLabel = new Label("Size: " + getHumanReadableFileSize(fileSize));
            sizeLabel.getStyleClass().add("item-size");

            Button actionButton = new Button();
            actionButton.getStyleClass().add("action-button");
            actionButton.setUserData(publishedFileId);

            VBox buttonContainer = new VBox(5);
            buttonContainer.getChildren().add(actionButton);
            labelsAndButton.getChildren().addAll(titleLabel, sizeLabel, buttonContainer);

            detailsBox.getChildren().add(labelsAndButton);
            itemPane.getChildren().addAll(imageView, detailsBox);

            RecentDownload existingDownload = recentDownloadsController.getDownloadByPublishedFileId(String.valueOf(publishedFileId));
            if (SteamCMDInteractor.isFileDownloaded(filePath)) {
                // Item is already downloaded, show "Installed" button
                actionButton.setText("Installed");
                actionButton.getStyleClass().add("installed-button");
                actionButton.setDisable(false);
                actionButton.setOnAction( e -> retryDownload(String.valueOf(publishedFileId), title, imageView.getImage(), getHumanReadableFileSize(fileSize), appId));

                ImageView folderIcon = new ImageView(new Image(getClass().getResourceAsStream("/com/risonna/scmdautomated/images/folder.png")));
                folderIcon.setFitHeight(35);
                folderIcon.setFitWidth(35);
                folderIcon.setOnMouseClicked(event -> openFolder(filePath));

                // Now add folderIcon to itemPane
                itemPane.getChildren().add(folderIcon);
            } else if (existingDownload != null) {
                // Update button state based on existing download status
                updateButtonState(existingDownload.getDownloadStatus(), actionButton);
            } else {
                // Item is not downloaded, show "Download" button
                actionButton.setText("Download");
                actionButton.setOnAction(e -> downloadItem(String.valueOf(publishedFileId), title, imageView.getImage(), String.valueOf(fileSize), appId));
            }

            itemPanes.put(String.valueOf(publishedFileId), itemPane);

            animateItemPane(itemPane);
            return itemPane;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    private void openFolder(String filePath) {
        try {
            Desktop.getDesktop().open(new File(filePath));
        } catch (IOException e) {
            // Handle error if opening the folder fails
            System.err.println("Failed to open folder: " + e.getMessage());
        }
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
    private void retryDownload(String publishedFileId, String name, Image image, String size, long appId) {
        if (publishedFileId != null && name != null && image != null && size != null && appId > 0) {
            downloadItem(publishedFileId, name, image, size, appId);
        }
    }

    private void downloadItem(String publishedFileId, String title, Image image, String size, long appId) {
        String filePath = SettingsController.getSteamcmdPath() + "\\steamapps\\workshop\\content\\" + appId + "\\" + publishedFileId;

        if (SteamCMDInteractor.isFileDownloaded(filePath)) {
            // Item is already downloaded, show redownload confirmation
            Alert alert = createRedownloadConfirmationDialog();
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // User wants to redownload
                startDownload(publishedFileId, title, image, size, appId);
            }
        } else {
            // Item is not downloaded, start download
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
            HBox detailsBox = (HBox) itemPane.getChildren().get(1);
            VBox labelsAndButtons = (VBox) detailsBox.getChildren().get(0);
            VBox buttonContainer = (VBox) labelsAndButtons.getChildren().get(labelsAndButtons.getChildren().size() - 1);
            Button actionButton = (Button) buttonContainer.getChildren().get(buttonContainer.getChildren().size() - 1);
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
