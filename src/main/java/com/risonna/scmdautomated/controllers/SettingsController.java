package com.risonna.scmdautomated.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

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
    @FXML
    private Label messageLabel;


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
            showMessage("Error: SteamCMD path is required.", "error");
            return;
        }

        File steamcmdExe = new File(steamcmdPath, "steamcmd.exe");
        if (!steamcmdExe.exists()) {
            showMessage("Error: steamcmd.exe not found in the specified path.", "error");
            return;
        }

        try (FileWriter writer = new FileWriter(STEAMCMD_PATH_FILE)) {
            writer.write(steamcmdPath);
            showMessage("SteamCMD path saved successfully! You can close this window.", "success");
        } catch (IOException e) {
            showMessage("Error saving SteamCMD path: " + e.getMessage(), "error");
        }
    }
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        if (type.equals("error")) {
            messageLabel.getStyleClass().removeAll("success-message");
            messageLabel.getStyleClass().add("error-message");
        } else if (type.equals("success")) {
            messageLabel.getStyleClass().removeAll("error-message");
            messageLabel.getStyleClass().add("success-message");
        }
    }
    public static String getSteamcmdPath() {
        try {
            return new String(java.nio.file.Files.readAllBytes(new File(STEAMCMD_PATH_FILE).toPath()));
        } catch (IOException e) {
            System.out.println("Error reading steamcmd path file: " + e.getMessage());
            return null;
        }
    }
    @FXML
    private void browseButtonClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select SteamCMD Folder");

        File steamcmdPathFile = new File(STEAMCMD_PATH_FILE);
        if (steamcmdPathFile.exists()) {
            try {
                String steamcmdPath = new String(java.nio.file.Files.readAllBytes(steamcmdPathFile.toPath()));
                directoryChooser.setInitialDirectory(new File(steamcmdPath));
            } catch (IOException e) {
                System.out.println("Error reading steamcmd path file: " + e.getMessage());
            }
        } else{
            if(!steamcmdPathTextField.getText().isEmpty()){
                try {
                    directoryChooser.setInitialDirectory(new File(steamcmdPathTextField.getText()));
                } catch (Exception e) {
                    System.err.println();
                    showMessage("Invalid path specified", "error");
                }
            }
        }
        File selectedDirectory = directoryChooser.showDialog(steamcmdPathTextField.getScene().getWindow());

        if (selectedDirectory != null) {
            steamcmdPathTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }
}