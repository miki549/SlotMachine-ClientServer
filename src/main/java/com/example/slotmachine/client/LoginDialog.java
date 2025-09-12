package com.example.slotmachine.client;

import com.example.slotmachine.server.dto.LoginResponse;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoginDialog {
    private final ApiClient apiClient;
    private LoginResponse loginResponse;
    private boolean cancelled = false;

    public LoginDialog(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public LoginResponse showAndWait(Stage parentStage) {
        Stage loginStage = new Stage();
        loginStage.initStyle(StageStyle.TRANSPARENT);
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.initOwner(parentStage);

        // UI elemek
        Label titleLabel = new Label("Bejelentkezés");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Felhasználónév");
        usernameField.setPrefWidth(250);
        usernameField.setStyle("-fx-font-size: 14px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Jelszó");
        passwordField.setPrefWidth(250);
        passwordField.setStyle("-fx-font-size: 14px;");

        Button loginButton = new Button("Bejelentkezés");
        loginButton.setPrefWidth(120);
        loginButton.getStyleClass().add("login-button");

        Button registerButton = new Button("Regisztráció");
        registerButton.setPrefWidth(120);
        registerButton.getStyleClass().add("register-button");

        Button cancelButton = new Button("Mégse");
        cancelButton.setPrefWidth(120);
        cancelButton.getStyleClass().add("cancel-button");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        // Event handlers
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Kérjük, töltse ki az összes mezőt!");
                return;
            }

            loginButton.setDisable(true);
            statusLabel.setText("Bejelentkezés...");

            // Háttérben futtatjuk a hálózati hívást
            new Thread(() -> {
                try {
                    LoginResponse response = apiClient.login(username, password);
                    
                    Platform.runLater(() -> {
                        loginResponse = response;
                        loginStage.close();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Bejelentkezés sikertelen: " + ex.getMessage());
                        loginButton.setDisable(false);
                    });
                }
            }).start();
        });

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Kérjük, töltse ki az összes mezőt!");
                return;
            }

            registerButton.setDisable(true);
            statusLabel.setText("Regisztráció...");

            new Thread(() -> {
                try {
                    apiClient.register(username, password);
                    
                    Platform.runLater(() -> {
                        statusLabel.setText("Regisztráció sikeres! Most jelentkezzen be.");
                        statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
                        registerButton.setDisable(false);
                        usernameField.clear();
                        passwordField.clear();
                        usernameField.requestFocus();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Regisztráció sikertelen: " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                        registerButton.setDisable(false);
                    });
                }
            }).start();
        });

        cancelButton.setOnAction(e -> {
            cancelled = true;
            loginStage.close();
        });

        // Enter lenyomásra bejelentkezés
        passwordField.setOnAction(e -> loginButton.fire());

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.getChildren().addAll(
            titleLabel, usernameField, passwordField, 
            loginButton, registerButton, cancelButton, statusLabel
        );

        layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10;");
        layout.setEffect(new DropShadow(20, Color.BLACK));

        // Lekerekített sarkok
        Rectangle clip = new Rectangle(350, 400);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        layout.setClip(clip);

        Scene scene = new Scene(layout, 350, 400);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");

        loginStage.setScene(scene);
        loginStage.setResizable(false);

        // Középre igazítás
        loginStage.setOnShown(e -> {
            loginStage.setX(parentStage.getX() + (parentStage.getWidth() - loginStage.getWidth()) / 2);
            loginStage.setY(parentStage.getY() + (parentStage.getHeight() - loginStage.getHeight()) / 2);
        });

        // Fókusz beállítása
        Platform.runLater(usernameField::requestFocus);

        loginStage.showAndWait();

        return cancelled ? null : loginResponse;
    }
}
