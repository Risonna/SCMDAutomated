package com.risonna.scmdautomated.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML
    private TextField steamcmdPathTextField;

    @FXML
    private Button saveSettingsButton;

    public static final String STEAMCMD_PATH_FILE = "steamcmd_path.txt";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        File steamcmdPathFile = new File(STEAMCMD_PATH_FILE);
        if (steamcmdPathFile.exists()) {
            try {
                String steamcmdPath = new String(java.nio.file.Files.readAllBytes(steamcmdPathFile.toPath()));
                steamcmdPathTextField.setText(steamcmdPath);
            } catch (IOException e) {
                System.out.println("Error reading steamcmd path file: " + e.getMessage());
            }
        }
    }
    public static String getSteamLogin(){
        return "a";
    }

    @FXML
    private void saveSettingsButtonClicked() {
        String steamcmdPath = steamcmdPathTextField.getText();
        if (steamcmdPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Steamcmd path is required");
            alert.setContentText("Please enter a valid steamcmd path");
            alert.showAndWait();
            return;
        }

        try (FileWriter writer = new FileWriter(STEAMCMD_PATH_FILE)) {
            writer.write(steamcmdPath);
            System.out.println("Steamcmd path saved: " + steamcmdPath);
        } catch (IOException e) {
            System.out.println("Error saving steamcmd path file: " + e.getMessage());
        }

        steamcmdPathTextField.clear();
    }
    public static String getSteamcmdPath() {
        try {
            return new String(java.nio.file.Files.readAllBytes(new File(STEAMCMD_PATH_FILE).toPath()));
        } catch (IOException e) {
            System.out.println("Error reading steamcmd path file: " + e.getMessage());
            return null;
        }
    }
}