package com.risonna.scmdautomated.controllers;

import com.risonna.scmdautomated.model.SteamCMDInteractor;
import com.risonna.scmdautomated.model.entities.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class LoginWindowController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Label errorLabel;
    private String steamgGuardCode;

    private CheckBox loginCheckBox;

    public void setLoginCheckBox(CheckBox loginCheckBox) {
        this.loginCheckBox = loginCheckBox;
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setOnCloseRequest(event -> {
            loginCheckBox.setSelected(false);
            UserSession.getInstance().logout();
        });
    }

    @FXML
    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (!username.isEmpty() && !password.isEmpty()) {
            loginButton.setDisable(true);
            errorLabel.setVisible(false);

            Task<Boolean> loginTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return authenticate(username, password);
                }
            };

            loginTask.setOnSucceeded(event -> {
                boolean result = loginTask.getValue();
                if (result) {
                    Stage stage = (Stage) loginButton.getScene().getWindow();
                    stage.close();
                    UserSession.getInstance().setLoggedIn(true);
                } else {
                    UserSession.getInstance().setLoggedIn(false);
                }
                loginButton.setDisable(false);
            });

            loginTask.setOnFailed(event -> {
                errorLabel.setText("An unexpected error occurred");
                errorLabel.setVisible(true);
                loginButton.setDisable(false);
                UserSession.getInstance().setLoggedIn(false);
            });

            new Thread(loginTask).start();
        } else {
            UserSession.getInstance().setLoggedIn(false);
            errorLabel.setText("Username and password cannot be empty");
            errorLabel.setVisible(true);
        }
    }

    private boolean authenticate(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Platform.runLater(() -> {
                errorLabel.setText("Username and password cannot be empty");
                errorLabel.setVisible(true);
            });
            return false;
        }

        UserSession.getInstance().setCredentials(username, password);
        String loginResult = SteamCMDInteractor.login();
        System.out.println("Login result: " + loginResult);

        switch (loginResult) {
            case "OK":
                return true;
            case "FAILED":
                Platform.runLater(() -> errorLabel.setText("Invalid username or password"));
                break;
            case "NEED_STEAM_GUARD":
                this.steamgGuardCode = showSteamGuardWindow();
                System.out.println(this.steamgGuardCode);
                while (this.steamgGuardCode == null || this.steamgGuardCode.isEmpty()) {
                    Platform.runLater(() -> errorLabel.setText("Steam Guard code is required"));
                    this.steamgGuardCode = showSteamGuardWindow();
                }
                String steamguardResult = SteamCMDInteractor.loginWithSteamGuard(this.steamgGuardCode);
                if(steamguardResult.equals("SUCCESS")){
                    return true;

                } else if (steamguardResult.equals("ERROR")) {
                    Platform.runLater(() -> errorLabel.setText("Wrong Steam Guard code"));
                    return false;
                }
                break;
            case "NEED_TWO_FACTOR":
                Platform.runLater(() -> errorLabel.setText("Two-factor authentication is not supported"));
                break;
            case "PATH ERROR":
                Platform.runLater(() -> errorLabel.setText("SteamCMD path is not set"));
                break;
            case "STEAMCMD DOESN'T EXIST":
                Platform.runLater(() -> errorLabel.setText("SteamCMD executable not found"));
                break;
            case "TIMEOUT":
                Platform.runLater(() -> errorLabel.setText("Login process timed out. Please try again."));
                break;
            case "UNKNOWN_ERROR":
                Platform.runLater(() -> errorLabel.setText("An unknown error occurred. Please check the console for details."));
                break;
            default:
                if (loginResult.startsWith("EXCEPTION:")) {
                    Platform.runLater(() -> errorLabel.setText("An error occurred: " + loginResult.substring(10)));
                } else {
                    Platform.runLater(() -> errorLabel.setText("An unexpected error occurred. Please check the console for details."));
                }
                break;
        }

        Platform.runLater(() -> errorLabel.setVisible(true));
        return false;
    }

    private String showSteamGuardWindow() {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] steamGuardCodeHolder = new String[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/risonna/scmdautomated/fxml/SteamGuardWindow.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(loader.load()));
                SteamGuardWindowController controller = loader.getController();
                stage.showAndWait();
                steamGuardCodeHolder[0] = controller.getSteamGuardCode();
            } catch (IOException e) {
                e.printStackTrace();
                steamGuardCodeHolder[0] = null;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(); // Wait for the user input
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        return steamGuardCodeHolder[0];
    }

    public String getSteamgGuardCode() {
        return steamgGuardCode;
    }

    public void setSteamgGuardCode(String steamgGuardCode) {
        this.steamgGuardCode = steamgGuardCode;
    }
}