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



import java.io.File;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;



import java.awt.Desktop;

public class RecentDownloadsController implements Initializable {

    @FXML
    private Button closeButton;
    @FXML
    private Button clearAllButton;
    @FXML
    private ListView<RecentDownload> recentDownloadsList;

    private ObservableList<RecentDownload> recentDownloads = FXCollections.observableArrayList();
    private final List<DownloadStatusObserver> observers = new ArrayList<>();
    private Label notificationLabel;
    private Timeline updateTimeline;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupRecentDownloadsList();
        configureButtons();
    }

    public void addObserver(DownloadStatusObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(DownloadStatusObserver observer) {
        observers.remove(observer);
    }

    public void setRecentDownloads(ObservableList<RecentDownload> recentDownloads) {
        this.recentDownloads = recentDownloads;
        startUpdateTimeline();
    }

    public void setNotificationLabel(Label notificationLabel) {
        this.notificationLabel = notificationLabel;
    }

    public Label getNotificationLabel() {
        return notificationLabel;
    }

    public ObservableList<RecentDownload> getRecentDownloads() {
        return recentDownloads;
    }

    public void addRecentDownload(RecentDownload recentDownload) {
        if (!recentDownloads.contains(recentDownload)) {
            recentDownloads.add(recentDownload);
        }
    }

    public RecentDownload getDownloadByPublishedFileId(String publishedFileId) {
        return recentDownloads.stream()
                .filter(download -> download.getPublishedFieldId().equals(publishedFileId))
                .findFirst()
                .orElse(null);
    }

    public void updateRecentDownloadStatusAndFilepath(String publishedFileId, String status, String filepath) {
        RecentDownload download = getDownloadByPublishedFileId(publishedFileId);
        if (download != null) {
            download.setDownloadStatus(status);
            download.setFilepath(filepath);
            notifyObservers(publishedFileId, status);
            NotificationController.updateRecentDownloadsNotification(true, notificationLabel);
        }
        refreshDownloadsList();
    }

    @FXML
    private void clearAllButtonClicked() {
        recentDownloads.clear();
        NotificationController.updateRecentDownloadsNotification(false, notificationLabel);
    }

    @FXML
    public void closeButtonClicked() {
        NotificationController.updateRecentDownloadsNotification(false, notificationLabel);
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        stage.close();
    }

    public void downloadWorkshopItem(String publishedFileId, long appId, RecentDownloadsController recentDownloadsController) {
        updateRecentDownloadStatusAndFilepath(publishedFileId, "downloading", null);
        SteamCMDInteractor.downloadWorkshopItem(publishedFileId, appId, recentDownloadsController);
        System.out.println("Downloading workshop item: " + publishedFileId);
    }

    private void notifyObservers(String publishedFileId, String status) {
        observers.forEach(observer -> observer.onDownloadStatusChanged(publishedFileId, status));
    }

    private void refreshDownloadsList() {
        Platform.runLater(() -> {
            if (recentDownloadsList != null) {
                recentDownloadsList.refresh();
            }
        });
    }

    public void setupRecentDownloadsList() {
        recentDownloadsList.setCellFactory(param -> new RecentDownloadListCell(this));
        recentDownloadsList.setItems(recentDownloads);
    }

    private void configureButtons() {
        closeButton.setOnAction(event -> closeButtonClicked());
        clearAllButton.setOnAction(event -> clearAllButtonClicked());
    }

    private void startUpdateTimeline() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        updateTimeline = new Timeline(new KeyFrame(Duration.millis(15000), e -> refreshDownloadsList()));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    private static class RecentDownloadListCell extends ListCell<RecentDownload> {

        private final HBox hbox = new HBox(10);
        private final ImageView imageView = new ImageView();
        private final VBox infoBox = new VBox(5);
        private final Label nameLabel = new Label();
        private final Label sizeLabel = new Label();
        private final Label statusLabel = new Label();
        private final Button openButton = new Button();
        private final Button reloadButton = new Button();
        private final Region spacer = new Region();

        private final RecentDownloadsController controller;

        public RecentDownloadListCell(RecentDownloadsController controller) {
            this.controller = controller;
            initializeCellComponents();
        }

        @Override
        protected void updateItem(RecentDownload item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setGraphic(hbox);
                updateCellContent(item);
            }
        }

        private void initializeCellComponents() {
            hbox.getStyleClass().add("download-item");

            imageView.setFitWidth(40);
            imageView.setFitHeight(40);

            nameLabel.getStyleClass().add("name");
            sizeLabel.getStyleClass().add("size");
            infoBox.getChildren().addAll(nameLabel, sizeLabel);

            statusLabel.getStyleClass().add("status");

            configureButton(openButton, "/com/risonna/scmdautomated/images/folder.png", 20, 20, "open-button");
            configureButton(reloadButton, "/com/risonna/scmdautomated/images/loading.png", 20, 20, "reload-button");

            HBox.setHgrow(spacer, Priority.ALWAYS);

            hbox.getChildren().addAll(imageView, infoBox, spacer, statusLabel, openButton, reloadButton);
        }

        private void configureButton(Button button, String iconPath, int iconWidth, int iconHeight, String styleClass) {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitWidth(iconWidth);
            icon.setFitHeight(iconHeight);
            button.setGraphic(icon);
            button.getStyleClass().add(styleClass);
        }

        private void updateCellContent(RecentDownload item) {
            imageView.setImage(item.getImage());
            nameLabel.setText(item.getName().length() < 25 ? item.getName() : item.getName().substring(0, 25) + "...");
            sizeLabel.setText(item.getFileSize());
            statusLabel.setText(item.getDownloadStatus());

            updateStatusLabelStyle(item.getDownloadStatus());
            configureOpenButton(item);
            configureReloadButton(item);
        }

        private void updateStatusLabelStyle(String status) {
            statusLabel.getStyleClass().removeAll("status-success", "status-error");
            statusLabel.getStyleClass().add(status.equals("success") ? "status-success" : "status-error");
        }

        private void configureOpenButton(RecentDownload item) {
            openButton.setOnAction(event -> {
                if ("success".equals(item.getDownloadStatus())) {
                    openDownloadedFile(item.getFilepath());
                }
            });
        }

        private void configureReloadButton(RecentDownload item) {
            reloadButton.setOnAction(event -> controller.downloadWorkshopItem(item.getPublishedFieldId(), Long.parseLong(item.getAppId()), controller));
            reloadButton.setVisible("failed".equals(item.getDownloadStatus()));
        }

        private void openDownloadedFile(String filepath) {
            File file = new File(filepath);
            try {
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    retryOpenFileAfterDelay(file, 2000);
                }
            } catch (IOException e) {
                System.err.println("Error opening file: " + e.getMessage());
            }
        }

        private void retryOpenFileAfterDelay(File file, int delay) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (file.exists()) {
                        try {
                            Desktop.getDesktop().open(file);
                        } catch (IOException e) {
                            System.err.println("Error opening file after delay: " + e.getMessage());
                        }
                    } else {
                        System.err.println("File still does not exist after delay.");
                    }
                }
            }, delay);
        }
    }
}
