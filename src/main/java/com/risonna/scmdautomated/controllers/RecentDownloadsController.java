package com.risonna.scmdautomated.controllers;


import com.risonna.scmdautomated.model.SteamCMDInteractor;
import com.risonna.scmdautomated.model.entities.RecentDownload;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RecentDownloadsController implements Initializable {
    @FXML
    private Button closeButton;
    @FXML
    private ListView<RecentDownload> recentDownloadsList;
    private ObservableList<RecentDownload> recentDownloads = FXCollections.observableArrayList();
    private Label notificationLabel;
    private Timeline updateTimeline;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        recentDownloadsList.setCellFactory(param -> new ListCell<>() {
            private HBox hbox;
            private ImageView imageView;
            private VBox infoBox;
            private Label nameLabel;
            private Label sizeLabel;
            private Label statusLabel;
            private Button openButton;
            private Button reloadButton;
            private Region spacer;

            {
                // Initialize the cell components only once
                hbox = new HBox(10);
                hbox.getStyleClass().add("download-item");

                imageView = new ImageView();
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);

                infoBox = new VBox(5);
                nameLabel = new Label();
                nameLabel.getStyleClass().add("name");
                sizeLabel = new Label();
                sizeLabel.getStyleClass().add("size");
                infoBox.getChildren().addAll(nameLabel, sizeLabel);

                statusLabel = new Label();
                statusLabel.getStyleClass().add("status");

                openButton = new Button();
                ImageView folderIcon = new ImageView(new Image("https://static-00.iconduck.com/assets.00/folder-icon-512x410-jvths5l6.png"));
                folderIcon.setFitWidth(20);
                folderIcon.setFitHeight(20);
                openButton.setGraphic(folderIcon);
                openButton.getStyleClass().add("open-button");

                reloadButton = new Button();
                ImageView reloadIcon = new ImageView(new Image(getClass().getResourceAsStream("/com/risonna/scmdautomated/images/loading.png")));
                reloadIcon.setFitWidth(20);
                reloadIcon.setFitHeight(20);
                reloadButton.setGraphic(reloadIcon);
                reloadButton.getStyleClass().add("reload-button");

                spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                hbox.getChildren().addAll(imageView, infoBox, spacer, statusLabel, openButton, reloadButton);
            }

            @Override
            protected void updateItem(RecentDownload item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setGraphic(hbox);

                imageView.setImage(item.getImage());
                nameLabel.setText(item.getName());
                sizeLabel.setText(item.getFileSize());
                statusLabel.setText(item.getDownloadStatus());

                statusLabel.getStyleClass().removeAll("status-success", "status-error");
                statusLabel.getStyleClass().add(item.getDownloadStatus().equals("success") ? "status-success" : "status-error");

                openButton.setOnAction(event -> {
                    if (item.getDownloadStatus().equals("success")) {
                        try {
                            Desktop.getDesktop().open(new File(item.getFilepath()));
                        } catch (IOException e) {
                            System.out.println("Error opening file: " + e.getMessage());
                        }
                    }
                });

                reloadButton.setOnAction(event -> downloadWorkshopItem(item.getPublishedFieldId(), Long.parseLong(item.getAppId()), true, RecentDownloadsController.this));

                reloadButton.setVisible(item.getDownloadStatus().equals("failed"));
            }
        });

        recentDownloadsList.setItems(recentDownloads);
    }

    public void addRecentDownload(RecentDownload recentDownload) {
        if(!recentDownloads.contains(recentDownload)) {
            recentDownloads.add(recentDownload);
        }
    }

    @FXML
    private void closeButtonClicked() {
        NotificationController.updateRecentDownloadsNotification(false, notificationLabel);
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        stage.close();
    }

    public ObservableList<RecentDownload> getRecentDownloads() {
        return recentDownloads;
    }

    public void setRecentDownloads(ObservableList<RecentDownload> recentDownloads) {
        this.recentDownloads = recentDownloads;
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        updateTimeline = new Timeline(new KeyFrame(Duration.millis(15000), e -> {
            Platform.runLater(() -> {
                if (recentDownloadsList != null) {
                    recentDownloadsList.refresh();
                }
            });
        }));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    public void updateRecentDownloadsList() {
        recentDownloadsList.setItems(recentDownloads);
    }

    public void updateRecentDownloadStatusAndFilepath(String publishedFileId, String status, String filepath) {
        for (RecentDownload download : recentDownloads) {
            if (download.getPublishedFieldId().equals(publishedFileId)) {
                download.setDownloadStatus(status);
                download.setFilepath(filepath);
                NotificationController.updateRecentDownloadsNotification(true, notificationLabel);
                break;
            }
        }
        Platform.runLater(() -> {
            if (recentDownloadsList != null) {
                recentDownloadsList.refresh();
            }
        });
    }

    public Label getNotificationLabel() {
        return notificationLabel;
    }

    public void setNotificationLabel(Label notificationLabel) {
        this.notificationLabel = notificationLabel;
    }

    @FXML
    private void clearAllButtonClicked() {
        recentDownloads.clear();
        NotificationController.updateRecentDownloadsNotification(false, notificationLabel);
    }

    // Mocking the downloadWorkshopItem method as per the instruction
    public void downloadWorkshopItem(String publishedFileId, long appId, boolean anonymous, RecentDownloadsController recentDownloadsController) {
        // Logic for downloading the workshop item goes here
        updateRecentDownloadStatusAndFilepath(publishedFileId, "downloading", null);
        SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, anonymous, recentDownloadsController);
        System.out.println("Downloading workshop item: " + publishedFileId);
    }

    public void setRecentDownloadsList(ListView<RecentDownload> recentDownloadsList) {
        this.recentDownloadsList = recentDownloadsList;
    }
    public ListView<RecentDownload> getRecentDownloadsList() {
        return recentDownloadsList;
    }
}