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
import java.util.*;
import java.util.List;

public class RecentDownloadsController implements Initializable {
    @FXML
    private Button closeButton;
    @FXML
    private Button clearAllButton;
    @FXML
    private ListView<RecentDownload> recentDownloadsList;
    private ObservableList<RecentDownload> recentDownloads = FXCollections.observableArrayList();
    private List<DownloadStatusObserver> observers = new ArrayList<>();
    private Label notificationLabel;
    private Timeline updateTimeline;
    public void addObserver(DownloadStatusObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(DownloadStatusObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String publishedFileId, String status) {
        for (DownloadStatusObserver observer : observers) {
            observer.onDownloadStatusChanged(publishedFileId, status);
        }
    }


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
                            File file = new File(item.getFilepath());
                            System.out.println("FilePath in controller: " + item.getFilepath());
                            System.out.println("File absolute path: " + file.getAbsolutePath());
                            System.out.println("File exists: " + file.exists());
                            if (file.exists()) {
                                Desktop.getDesktop().open(file);
                            } else {
                                System.out.println("File does not exist at the time of opening, retrying after delay.");
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        if (file.exists()) {
                                            try {
                                                Desktop.getDesktop().open(file);
                                            } catch (IOException e) {
                                                System.out.println("Error opening file after delay: " + e.getMessage());
                                            }
                                        } else {
                                            System.out.println("File still does not exist after delay.");
                                        }
                                    }
                                }, 2000); // Delay in milliseconds
                            }
                        } catch (IOException e) {
                            System.out.println("Error opening file: " + e.getMessage());
                        }
                    }
                });



                reloadButton.setOnAction(event -> downloadWorkshopItem(item.getPublishedFieldId(), Long.parseLong(item.getAppId()), RecentDownloadsController.this));

                reloadButton.setVisible(item.getDownloadStatus().equals("failed"));
            }
        });

        recentDownloadsList.setItems(recentDownloads);
        // Set onAction handlers for buttons
        closeButton.setOnAction(event -> closeButtonClicked());
        clearAllButton.setOnAction(event -> clearAllButtonClicked());
    }

    public void addRecentDownload(RecentDownload recentDownload) {
        if(!recentDownloads.contains(recentDownload)) {
            recentDownloads.add(recentDownload);
        }
    }

    @FXML
    void closeButtonClicked() {
        NotificationController.updateRecentDownloadsNotification(false, notificationLabel);
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        stage.close();
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
                notifyObservers(publishedFileId, status);
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
    public void downloadWorkshopItem(String publishedFileId, long appId, RecentDownloadsController recentDownloadsController) {
        // Logic for downloading the workshop item goes here
        updateRecentDownloadStatusAndFilepath(publishedFileId, "downloading", null);
        SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
        System.out.println("Downloading workshop item: " + publishedFileId);
    }

    public void setRecentDownloadsList(ListView<RecentDownload> recentDownloadsList) {
        this.recentDownloadsList = recentDownloadsList;
    }
    public ObservableList<RecentDownload> getRecentDownloads() {
        return recentDownloads;
    }
    public RecentDownload getDownloadByPublishedFileId(String publishedFileId) {
        for (RecentDownload download : recentDownloads) {
            if (download.getPublishedFieldId().equals(publishedFileId)) {
                return download;
            }
        }
        return null;
    }
}