package com.risonna.scmdautomated.controllers;

import com.risonna.scmdautomated.model.APICaller;
import com.risonna.scmdautomated.model.ImageDownloader;
import com.risonna.scmdautomated.model.SteamCMDInteractor;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class CollectionItemsController {

    @FXML
    private VBox itemsContainer;




    public void initialize() {
        // This method is called after the FXML file has been loaded
    }

    public void loadCollectionItems(List<Long> ids) {
        // Clear the existing items
        Platform.runLater(() -> itemsContainer.getChildren().clear());

        // Create a Task to load collection items in a separate thread
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() {
                String response = APICaller.sendPostRequestCheckItemDetails(ids);
                if (response != null) {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject responseObj = jsonObject.getJSONObject("response");
                    JSONArray publishedFileDetails = responseObj.getJSONArray("publishedfiledetails");

                    // Load collection items one by one
                    for (int i = 0; i < publishedFileDetails.length(); i++) {
                        JSONObject publishedFileDetail = publishedFileDetails.getJSONObject(i);

                        // Create a placeholder item with a loading indicator
                        HBox placeholderItem = createPlaceholderItem();

                        // Add the placeholder item to the items container
                        Platform.runLater(() -> itemsContainer.getChildren().add(placeholderItem));

                        // Load the actual item in the background
                        loadItem(publishedFileDetail, placeholderItem);
                    }
                }
                return null;
            }
        };

        // Start the load task
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

        Button downloadButton = new Button("Download");
        downloadButton.getStyleClass().add("download-button");
        downloadButton.setOnAction(e -> downloadItem(publishedFileId, title, imageView.getImage(), String.valueOf(fileSize), true, appId));

        detailsBox.getChildren().addAll(titleLabel, sizeLabel, downloadButton);

        itemPane.getChildren().addAll(imageView, detailsBox);

        // Add some animations to make it look smoother
        itemPane.setOpacity(0);
        itemPane.setTranslateY(10);
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), new KeyValue(itemPane.opacityProperty(), 1)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), new KeyValue(itemPane.translateYProperty(), 0)));
        timeline.play();

        return itemPane;
    }

    private void downloadItem(long publishedFileId, String title, Image image, String size, boolean anonymous, long appId) {
        try{
            if(publishedFileId != 0) {
                System.out.println("Downloading " + title + "..." +  size + "..." +  "anonymous: " +  anonymous +  "..." +  "id: " + publishedFileId);
                MainController.getRecentDownloadsController().addRecentDownload(new RecentDownload(title, image, size, "downloading", null, String.valueOf(publishedFileId), String.valueOf(appId)));
                NotificationController.updateRecentDownloadsNotification(true, MainController.getRecentDownloadsController().getNotificationLabel());
                SteamCMDInteractor.downloadWorkshopItem(String.valueOf(publishedFileId), appId, MainController.getRecentDownloadsController());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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