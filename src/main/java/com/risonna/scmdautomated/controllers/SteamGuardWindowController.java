package com.risonna.scmdautomated.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SteamGuardWindowController {
    @FXML
    private TextField codeField;
    @FXML
    private Label errorLabel;

    private String steamGuardCode;

    @FXML
    private void submitCode() {
        steamGuardCode = codeField.getText().trim();
        if (!steamGuardCode.isEmpty()) {
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.close();
        } else {
            errorLabel.setText("Please enter a valid code");
            errorLabel.setVisible(true);
        }
    }

    public String getSteamGuardCode() {
        return steamGuardCode;
    }
}