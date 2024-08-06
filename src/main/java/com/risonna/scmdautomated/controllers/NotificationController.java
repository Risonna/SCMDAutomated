package com.risonna.scmdautomated.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class NotificationController {
    public static void updateRecentDownloadsNotification(boolean show, Label notification) {
        if (show) {
            notification.setVisible(true);

            Timeline timeline = new Timeline();
            timeline.setCycleCount(Timeline.INDEFINITE);

            KeyFrame kf1 = new KeyFrame(Duration.millis(200), e -> notification.setOpacity(1));
            KeyFrame kf2 = new KeyFrame(Duration.millis(400), e -> notification.setOpacity(0));
            KeyFrame kf3 = new KeyFrame(Duration.millis(600), e -> notification.setOpacity(1));
            KeyFrame kf4 = new KeyFrame(Duration.millis(800), e -> notification.setOpacity(0));

            timeline.getKeyFrames().addAll(kf1, kf2, kf3, kf4);

            timeline.play();
        } else {
            notification.setVisible(false);
        }
    }
}
